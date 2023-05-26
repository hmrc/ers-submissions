/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services.resubmission

import models._
import org.mongodb.scala.bson.{BsonDocument, ObjectId}
import org.mongodb.scala.result.UpdateResult
import play.api.libs.json.{JsError, JsObject, JsSuccess}
import play.api.mvc.Request
import repositories.MetadataMongoRepository
import services.SubmissionService
import services.audit.AuditEvents
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggingAndRexceptions.{ErsLoggingAndAuditing, ResubmissionExceptionEmitter}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ResubPresubmissionService @Inject()(metadataRepository: MetadataMongoRepository,
                                          val schedulerLoggingAndAuditing: ErsLoggingAndAuditing,
                                          submissionCommonService: SubmissionService,
                                          auditEvents: AuditEvents,
                                          resubmissionExceptionEmiter: ResubmissionExceptionEmitter)
                                         (implicit ec: ExecutionContext) {

  def mapJsonToAggregatedLog(record: JsObject): Option[AggregatedLog] =
    record.validate[AggregatedLog] match {
      case JsSuccess(obj, _) =>
        Some(obj)
      case JsError(_) =>
        None
    }

  def logAggregateMetadataMetrics(): Future[Unit] = {
    for {
      aggregatedRecords: Seq[JsObject] <- metadataRepository
        .getAggregateCountOfSubmissions()
      aggregatedLogs = AggregatedLogs(
        aggregatedRecords.flatMap(mapJsonToAggregatedLog)
      ).message
    } yield schedulerLoggingAndAuditing.logInfo(aggregatedLogs)
  }

  def logFailedSubmissionCount()(implicit processFailedSubmissionsConfig: ProcessFailedSubmissionsConfig): Future[Unit] = {
    val failedJobSelector: BsonDocument = metadataRepository.createFailedJobSelector()
    for {
      numberOfRecords: Long <- metadataRepository
        .getNumberOfFailedJobs(failedJobSelector)
      countToLog = TotalNumberSubmissionsToProcessMessage(
        numberOfFailedJobs = numberOfRecords
      ).message
    } yield schedulerLoggingAndAuditing.logInfo(countToLog)
  }

  def processFailedSubmissions()(implicit request: Request[_],
                                 hc: HeaderCarrier,
                                 processFailedSubmissionsConfig: ProcessFailedSubmissionsConfig): Future[Boolean] = {

    val failedJobSelector: BsonDocument = metadataRepository.createFailedJobSelector()
    for {
      failedJobIds: Seq[ObjectId] <- metadataRepository.getFailedJobs(failedJobSelector)
      _ = resubmissionExceptionEmiter.logInfo(NumberOfFailedToBeProcessedMessage(failedJobIds.length).message)
      updateResult: UpdateResult <- metadataRepository
        .findAndUpdateByStatus(failedJobIds)
        .recover {
          case rex: ResubmissionException => throw rex
          case ex: Exception => resubmissionExceptionEmiter.emitFrom(
            Map(
              "message" -> "Searching for data to be resubmitted",
              "context" -> "ResubPresubmissionService.processFailedSubmissions.findAndUpdateByStatus"
            ),
            Some(ex),
            None
          )
        }
      ersSummaries: Seq[ErsSummary] <- metadataRepository.findErsSummaries(failedJobIds)
      resubmissionResults: Seq[Boolean] <- if (updateResult.wasAcknowledged()) {
          Future.sequence(
            ersSummaries.map {
              ersSummary =>
                startResubmission(ersSummary).map(res => {
                  auditEvents.resubmissionResult(ersSummary.metaData.schemeInfo, res)
                  res
                })
            }
          )
        }
      else {
        schedulerLoggingAndAuditing.logWarn(NoDataToResubmitMessage.message)
        Future(Seq.empty[Boolean])
      }
    } yield resubmissionResults.forall(identity)
  }

  def startResubmission(ersSummary: ErsSummary)(implicit request: Request[_],
                                                hc: HeaderCarrier,
                                                processFailedSubmissionsConfig: ProcessFailedSubmissionsConfig): Future[Boolean] = {
    schedulerLoggingAndAuditing.logInfo(ProcessingResubmitMessage.message + Some(ersSummary.confirmationDateTime))
    submissionCommonService.callProcessData(ersSummary,
      processFailedSubmissionsConfig.failedStatus,
      processFailedSubmissionsConfig.resubmitSuccessStatus).map(res => res).recover {
      case aex: ADRTransferException =>
        auditEvents.sendToAdrEvent("ErsTransferToAdrFailed", ersSummary, source = Some("scheduler"))
        resubmissionExceptionEmiter.emitFrom(
          Map(
            "message" -> s"Resubmitting data to ADR - ADRTransferException: ${aex.message}",
            "context" -> s"${aex.context}"
          ),
          Some(aex),
          Some(ersSummary.metaData.schemeInfo)
        )
      case ex: Exception =>
        auditEvents.sendToAdrEvent("ErsTransferToAdrFailed", ersSummary, source = Some("scheduler"))
        resubmissionExceptionEmiter.emitFrom(
          Map(
            "message" -> s"Resubmitting data to ADR - Exception: ${ex.getMessage}",
            "context" -> "ResubPresubmissionService.startResubmission.callProcessData"
          ),
          Some(ex),
          Some(ersSummary.metaData.schemeInfo)
        )
      case ex: Throwable =>
        auditEvents.sendToAdrEvent("ErsTransferToAdrFailed", ersSummary, source = Some("scheduler"))
        throw ex
    }
  }
}
