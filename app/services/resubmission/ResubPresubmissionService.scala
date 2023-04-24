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

import config.ApplicationConfig
import models._
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
                                          val applicationConfig: ApplicationConfig,
                                          auditEvents: AuditEvents,
                                          resubmissionExceptionEmiter: ResubmissionExceptionEmitter)
                                         (implicit ec: ExecutionContext) extends SchedulerConfig {

  def logFailedSubmissionCount(): Future[Unit] =
    for {
      numberOfRecords: Long <- metadataRepository
        .getNumberOfFailedJobs(searchStatusList)
      countToLog = NumberOfFailedJobsMessage(
        numberOfFailedJobs = numberOfRecords,
        failedStatuses = searchStatusList
      ).message
    } yield schedulerLoggingAndAuditing.logInfo(countToLog)

  def processFailedSubmissions()(implicit request: Request[_], hc: HeaderCarrier): Future[Option[Boolean]] = {
    metadataRepository.findAndUpdateByStatus(
      searchStatusList,
      isResubmitBeforeDate,
      schemeRefList,
      resubmitScheme
    ).flatMap {
      case Some(ersSummary) => startResubmission(ersSummary).map(res => {
        auditEvents.resubmissionResult(ersSummary.metaData.schemeInfo, res)
        Some(res)
      })
      case None =>
        schedulerLoggingAndAuditing.logWarn(NoDataToResubmitMessage.message)
        Future(None)
    }.recover {
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
  }

  def startResubmission(ersSummary: ErsSummary)(implicit request: Request[_], hc: HeaderCarrier): Future[Boolean] = {
    schedulerLoggingAndAuditing.logInfo(ProcessingResubmitMessage.message + Some(ersSummary))
    submissionCommonService.callProcessData(ersSummary, failedStatus, resubmitSuccessStatus).map(res => res).recover {
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
