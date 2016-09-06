/*
 * Copyright 2016 HM Revenue & Customs
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
import repositories.{Repositories, JsonStoreInfoMongoRepository, JsonStoreInfoRepository}
import services.SubmissionCommonService
import services.audit.AuditEvents
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.LoggingAndRexceptions.{ErsLoggingAndAuditing, ResubmissionExceptionEmiter}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ResubPresubmissionService extends ResubPresubmissionService {
  override lazy val jsonStoreInfoRepository: JsonStoreInfoMongoRepository = Repositories.postsubmissionRepository
  override val schedulerLoggingAndAuditing: ErsLoggingAndAuditing = ErsLoggingAndAuditing
  override val submissionCommonService: SubmissionCommonService = SubmissionCommonService
}

trait ResubPresubmissionService extends SchedulerConfig {
  lazy val jsonStoreInfoRepository: JsonStoreInfoRepository = ???
  val schedulerLoggingAndAuditing: ErsLoggingAndAuditing
  val submissionCommonService: SubmissionCommonService

  def processFailedSubmissions()(implicit request: Request[_], hc: HeaderCarrier): Future[Boolean] = {
    jsonStoreInfoRepository.findAndUpdateByStatus(searchStatusList, schemeRefList).flatMap { jsonStoreInfo =>
      if(jsonStoreInfo.isDefined) {
        val ersJsonStoreInfo: ErsJsonStoreInfo = jsonStoreInfo.get
        startResubmission(ersJsonStoreInfo.schemeInfo).map(res => res)
      }
      else {
        schedulerLoggingAndAuditing.logWarn("No data found for resubmission")
        Future(true)
      }
    }.recover {
      case rex: ResubmissionException => {
        if(rex.schemeInfo.isDefined) {
          updateFailedSubmission(rex.schemeInfo.get)
        }
        throw rex
      }
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

  def updateFailedSubmission(schemeInfo: SchemeInfo)(implicit request: Request[_], hc: HeaderCarrier): Unit = {
    jsonStoreInfoRepository.updateStatus(failedStatus, schemeInfo).map { res =>
      schedulerLoggingAndAuditing.handleResult(res, Some("Update status was successful"), Some("Update status failed"), Some(schemeInfo))
    }.recover {
      case ex: Exception => schedulerLoggingAndAuditing.handleException(schemeInfo, ex, "Update status in ResubPresubmissionService.updateFailedSubmission")
    }
  }

  def startResubmission(schemeInfo: SchemeInfo)(implicit request: Request[_], hc: HeaderCarrier): Future[Boolean] = {
    submissionCommonService.getErsSummaryBySchemeInfo(schemeInfo).flatMap { ersSummary =>
      submissionCommonService.callProcessData(ersSummary, failedStatus).map(res => res).recover {
        case ex: Throwable => {
          AuditEvents.sendToAdrEvent("ErsTransferToAdrFailed", ersSummary, source = Some("scheduler"))
          throw ex
        }
      }
    }.recover {
      case aex: ADRTransferException => ResubmissionExceptionEmiter.emitFrom(
        Map(
          "message" -> s"Resubmitting data to ADR - ADRTransferException: ${aex.message}",
          "context" -> s"${aex.context}"
        ),
        Some(aex),
        Some(schemeInfo)
      )
      case ex: Exception => ResubmissionExceptionEmiter.emitFrom(
        Map(
          "message" -> s"Resubmitting data to ADR - Exception: ${ex.getMessage}",
          "context" -> "ResubPresubmissionService.startResubmission.getErsSummaryBySchemeInfo"
        ),
        Some(ex),
        Some(schemeInfo)
      )
    }
  }
}
