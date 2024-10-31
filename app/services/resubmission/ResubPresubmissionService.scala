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

import cats.implicits._
import common.ERSEnvelope
import common.ERSEnvelope.ERSEnvelope
import messages._
import models._
import org.mongodb.scala.bson.BsonDocument
import play.api.Logging
import play.api.libs.json.{JsError, JsObject, JsPath, JsSuccess, JsonValidationError, Reads}
import play.api.mvc.Request
import repositories.{MetadataMongoRepository, PresubmissionMongoRepository, Selectors}
import services.SubmissionService
import services.audit.AuditEvents
import uk.gov.hmrc.http.HeaderCarrier
import utils.Session

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ResubPresubmissionService @Inject()(metadataRepository: MetadataMongoRepository,
                                          presubmissionRepository: PresubmissionMongoRepository,
                                          submissionCommonService: SubmissionService,
                                          auditEvents: AuditEvents)
                                         (implicit ec: ExecutionContext) extends Logging {

  def validateJson[T](record: JsObject)(implicit reads: Reads[T]): Option[T] =
    record.validate[T] match {
      case JsSuccess(obj, _) =>
        Some(obj)
      case JsError(errors: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]) =>
        val jsErrors = errors
          .map((e: (JsPath, collection.Seq[JsonValidationError])) => s"${e._1}: ${e._2.mkString(", ")}")
          .mkString(", ")
        logger.warn(s"Failed to validate JsObject error: ${jsErrors}")
        None
    }

  def logAggregateMetadataMetrics()(implicit hc: HeaderCarrier): ERSEnvelope[String] = {
    for {
      aggregatedRecords <- metadataRepository
        .getAggregateCountOfSubmissions(Session.id(hc))
      aggregatedLogs = AggregatedLogs(
        aggregatedRecords.flatMap(validateJson[AggregatedLog])
      ).message
    } yield aggregatedLogs
  }

  def logFailedSubmissionCount(processFailedSubmissionsConfig: ProcessFailedSubmissionsConfig)
                              (implicit hc: HeaderCarrier): ERSEnvelope[String] = {
    val failedJobSelector: BsonDocument = metadataRepository.createFailedJobSelector(processFailedSubmissionsConfig)
    for {
      numberOfRecords <- metadataRepository
        .getNumberOfFailedJobs(failedJobSelector, Session.id(hc))
      countToLog = TotalNumberSubmissionsToProcessMessage(
        numberOfFailedJobs = numberOfRecords
      ).message
    } yield countToLog
  }

  def getPreSubSelectedSchemeRefDetailsMessage(processFailedSubmissionsConfig: ProcessFailedSubmissionsConfig)
                                              (implicit hc: HeaderCarrier): ERSEnvelope[String] =
    for {
      preSubmissionStatuses <- presubmissionRepository
        .getStatusForSelectedSchemes(Session.id(hc), Selectors(processFailedSubmissionsConfig))
      preSubmissionSchemeData: Seq[SchemeData] = preSubmissionStatuses
        .flatMap(validateJson[SchemeData])
      selectedSchemeRefLogs = PreSubSelectedSchemeRefLogs(preSubmissionSchemeData)
    } yield selectedSchemeRefLogs.message

  def getMetadataSelectedSchemeRefDetailsMessage(processFailedSubmissionsConfig: ProcessFailedSubmissionsConfig)
                                                (implicit hc: HeaderCarrier): ERSEnvelope[String] =
    for {
      metadataStatuses <- metadataRepository
        .getStatusForSelectedSchemes(Session.id(hc), Selectors(processFailedSubmissionsConfig))
      metadataErsSummaries: Seq[ErsSummary] = metadataStatuses
        .flatMap(validateJson[ErsSummary])
      selectedSchemeRefLogs = MetaDataSelectedSchemeRefLogs(metadataErsSummaries)
    } yield selectedSchemeRefLogs.message

  def processFailedSubmissions(processFailedSubmissionsConfig: ProcessFailedSubmissionsConfig)
                              (implicit request: Request[_], hc: HeaderCarrier): ERSEnvelope[Boolean] = {

    val failedJobSelector: BsonDocument = metadataRepository.createFailedJobSelector(processFailedSubmissionsConfig)
    for {
      failedJobIds <- metadataRepository.getFailedJobs(failedJobSelector, processFailedSubmissionsConfig)
      _ = logger.info(NumberOfFailedToBeProcessedMessage(failedJobIds.length).message)
      updateResult <- metadataRepository.findAndUpdateByStatus(failedJobIds, Session.id(hc))
      ersSummaries <- metadataRepository.findErsSummaries(failedJobIds, Session.id(hc))
      resubmissionResults <- if (updateResult.wasAcknowledged()) {
        ersSummaries.map { ersSummary =>
          startResubmission(ersSummary, processFailedSubmissionsConfig).map { result =>
            auditEvents.resubmissionResult(ersSummary.metaData.schemeInfo, result)
            result
          }
        }.sequence
      } else {
        logger.warn(NoDataToResubmitMessage.message)
        ERSEnvelope(scala.Seq[Boolean]())
      }
    } yield resubmissionResults.forall(identity)
  }

  def startResubmission(ersSummary: ErsSummary, processFailedSubmissionsConfig: ProcessFailedSubmissionsConfig)
                       (implicit request: Request[_], hc: HeaderCarrier): ERSEnvelope[Boolean] = {
    logger.info(ProcessingResubmitMessage.message + ersSummary.metaData.schemeInfo.basicLogMessage)
    submissionCommonService.callProcessData(ersSummary,
      processFailedSubmissionsConfig.failedStatus,
      processFailedSubmissionsConfig.resubmitSuccessStatus).map { result =>
      if(result) {
        logger.info(s"Resubmission completed successfully for schemeRef: ${ersSummary.metaData.schemeInfo.schemeRef}")
      } else {
        logger.error(s"RESUBMISSION_FAILED [startResubmission] Resubmission failed for: ${ersSummary.metaData.schemeInfo.basicLogMessage}")
        auditEvents.sendToAdrEvent("ErsTransferToAdrFailed", ersSummary, source = Some("scheduler"))
      }
      result
    }
  }
}
