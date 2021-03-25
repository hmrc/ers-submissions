/*
 * Copyright 2021 HM Revenue & Customs
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

import javax.inject.Inject
import models.{SchemeData, SchemeInfo, SubmissionsSchemeData}
import play.api.Logger
import play.api.libs.json.JsObject
import play.api.mvc.Request
import repositories.{PresubmissionMongoRepository, Repositories}
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier

class PresubmissionService @Inject()(repositories: Repositories, ersLoggingAndAuditing: ErsLoggingAndAuditing) {

  lazy val presubmissionRepository: PresubmissionMongoRepository = repositories.presubmissionRepository

  def storeJson(presubmissionData: SchemeData)(implicit request: Request[_], hc: HeaderCarrier): Future[Boolean] = {

    presubmissionRepository.storeJson(presubmissionData).recover {
      case ex: Exception => {
        ersLoggingAndAuditing.handleException(presubmissionData.schemeInfo, ex, "Exception during storing presubmission data")
        false
      }
    }

  }

  def storeJson(presubmissionData: SubmissionsSchemeData, jsObject: JsObject)(implicit request: Request[_], hc: HeaderCarrier): Future[Boolean] = {

    Logger.error("jsObject we're storing has size " + jsObject.toString.length)
//    Logger.error("jsObject is " + jsObject.toString())
    presubmissionRepository.storeJson(jsObject, presubmissionData.schemeInfo.toString).recover {
      case ex: Exception => {
        Logger.error("our issue is " + ex)
        ersLoggingAndAuditing.handleException(presubmissionData.schemeInfo, ex, "Exception during storing presubmission data")
        false
      }
    }

  }

  def getJson(schemeInfo: SchemeInfo): Future[List[SchemeData]] = {
    Logger.debug("LFP -> 3. PresubmissionService.getJson () ")
    presubmissionRepository.getJson(schemeInfo)
  }

  def removeJson(schemeInfo: SchemeInfo)(implicit request: Request[_], hc: HeaderCarrier): Future[Boolean] = {
    presubmissionRepository.removeJson(schemeInfo).recover {
      case ex: Exception => {
        ersLoggingAndAuditing.handleException(schemeInfo, ex, "Exception during deleting presubmission data")
        false
      }
    }

  }

  def compareSheetsNumber(expectedSheets: Int, schemeInfo: SchemeInfo)(implicit request: Request[_], hc: HeaderCarrier): Future[Boolean] = {
    presubmissionRepository.count(schemeInfo).map { existingSheets =>
      existingSheets == expectedSheets
    }.recover {
      case ex: Exception => {
        ersLoggingAndAuditing.handleException(schemeInfo, ex, "Exception during checking for presubmission data")
        false
      }
    }

  }

  def findAndUpdate(schemeInfo: SchemeInfo)(implicit request: Request[_], hc: HeaderCarrier): Future[Option[SchemeData]] = {
    presubmissionRepository.findAndUpdate(schemeInfo)
  }

}
