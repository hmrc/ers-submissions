/*
 * Copyright 2017 HM Revenue & Customs
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
import play.api.mvc.Request
import repositories.{MetadataMongoRepository, Repositories}
import services.SubmissionCommonService
import services.audit.AuditEvents
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.LoggingAndRexceptions.{ErsLoggingAndAuditing, ResubmissionExceptionEmiter}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ResubPresubmissionService extends ResubPresubmissionService {
  override lazy val metadataRepository: MetadataMongoRepository = Repositories.metadataRepository
  override val schedulerLoggingAndAuditing: ErsLoggingAndAuditing = ErsLoggingAndAuditing
  override val submissionCommonService: SubmissionCommonService = SubmissionCommonService
}

trait ResubPresubmissionService extends SchedulerConfig {
  lazy val metadataRepository: MetadataMongoRepository = ???
  val schedulerLoggingAndAuditing: ErsLoggingAndAuditing
  val submissionCommonService: SubmissionCommonService

  def processFailedSubmissions()(implicit request: Request[_], hc: HeaderCarrier): Future[Boolean] = {
    metadataRepository.findAndUpdateByStatus(searchStatusList, resubmitWithNilReturn, resubmitAfterDate, schemeRefList, resubmitScheme).flatMap { ersSummary =>
      if(ersSummary.isDefined) {
        startResubmission(ersSummary.get).map(res => res)
      }
      else {
        schedulerLoggingAndAuditing.logWarn("No data found for resubmission")
        Future(true)
      }
    }.recover {
      case rex: ResubmissionException => throw rex
      case ex: Exception => ResubmissionExceptionEmiter.emitFrom(
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
    submissionCommonService.callProcessData(ersSummary, failedStatus, resubmitSuccessStatus).map(res => res).recover {
      case aex: ADRTransferException => {
        AuditEvents.sendToAdrEvent("ErsTransferToAdrFailed", ersSummary, source = Some("scheduler"))
        ResubmissionExceptionEmiter.emitFrom(
          Map(
            "message" -> s"Resubmitting data to ADR - ADRTransferException: ${aex.message}",
            "context" -> s"${aex.context}"
          ),
          Some(aex),
          Some(ersSummary.metaData.schemeInfo)
        )
      }
      case ex: Exception => {
        AuditEvents.sendToAdrEvent("ErsTransferToAdrFailed", ersSummary, source = Some("scheduler"))
        ResubmissionExceptionEmiter.emitFrom(
          Map(
            "message" -> s"Resubmitting data to ADR - Exception: ${ex.getMessage}",
            "context" -> "ResubPresubmissionService.startResubmission.callProcessData"
          ),
          Some(ex),
          Some(ersSummary.metaData.schemeInfo)
        )
      }
      case ex: Throwable => {
        AuditEvents.sendToAdrEvent("ErsTransferToAdrFailed", ersSummary, source = Some("scheduler"))
        throw ex
      }
    }
  }
}
