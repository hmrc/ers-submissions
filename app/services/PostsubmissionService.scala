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

package services

import models._
import org.joda.time.DateTime
import play.api.Logger
import play.api.mvc.Request
import repositories._
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.LoggingAndRexceptions.ADRExceptionEmitter
import utils._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object PostsubmissionService extends PostsubmissionService {
  override lazy val jsonStoreInfoRepository: JsonStoreInfoMongoRepository = Repositories.postsubmissionRepository
  override val submissionCommon: SubmissionCommon = SubmissionCommon
  override val submissionCommonService: SubmissionCommonService = SubmissionCommonService
}

trait PostsubmissionService {
  lazy val jsonStoreInfoRepository: JsonStoreInfoRepository = ???
  val submissionCommon: SubmissionCommon
  val submissionCommonService: SubmissionCommonService

  def processDataForADR(ersSummary: ErsSummary)(implicit request: Request[_], hc: HeaderCarrier): Future[Boolean] = {
    Logger.warn(s"Submission journey 2.1. start creating storeInfo: ${DateTime.now}")
    jsonStoreInfoRepository.createErsJsonStoreInfo(
      ErsJsonStoreInfo(ersSummary.metaData.schemeInfo, None, None, None, None, Statuses.Saved.toString)
    ).flatMap { res =>
      if(res) {
        Logger.warn(s"Submission journey 2.2. creating storeInfo was successful: ${DateTime.now}")
        submissionCommonService.callProcessData(ersSummary, Statuses.Failed.toString).map(r => r)
      }
      else {
        Logger.warn(s"Submission journey 2.2. creating storeInfo failed: ${DateTime.now}")
        ADRExceptionEmitter.emitFrom(
          ersSummary.metaData,
          Map(
            "message" -> "Creating ErsJsonStoreInfo failed",
            "context" -> "PostsubmissionService.processDataForADR"
          )
        )
      }
    }.recover {
      case aex: ADRTransferException => {
        Logger.warn(s"Submission journey 2.2. creating storeInfo failed exception ${aex.message}: ${DateTime.now}")
        throw aex
      }
      case ex: Exception => {
        Logger.warn(s"Submission journey 2.2. creating storeInfo failed exception ${ex.getMessage}: ${DateTime.now}")
        ADRExceptionEmitter.emitFrom(
          ersSummary.metaData,
          Map(
            "message" -> "Exception during creating ErsJsonStoreInfo",
            "context" -> "PostsubmissionService.processDataForADR"
          ),
          Some(ex)
        )
      }
    }
  }
}
