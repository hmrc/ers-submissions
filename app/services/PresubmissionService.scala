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

package services

import models.{SchemeData, SchemeInfo, SubmissionsSchemeData}
import play.api.Logging
import repositories.{PresubmissionMongoRepository, Repositories}
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PresubmissionService @Inject()(repositories: Repositories, ersLoggingAndAuditing: ErsLoggingAndAuditing)
                                    (implicit ec: ExecutionContext) extends Logging {

  lazy val presubmissionRepository: PresubmissionMongoRepository = repositories.presubmissionRepository

  def storeJson(presubmissionData: SchemeData)(implicit hc: HeaderCarrier): Future[Boolean] = {

    presubmissionRepository.storeJson(presubmissionData).recover {
      case ex: Exception =>
        ersLoggingAndAuditing.handleException(presubmissionData.schemeInfo, ex, "Exception during storing presubmission data")
        false
    }

  }

  def storeJsonV2(presubmissionData: SubmissionsSchemeData, schemeData: SchemeData)(implicit hc: HeaderCarrier): Future[Boolean] = {

    presubmissionRepository.storeJsonV2(presubmissionData.schemeInfo.toString, schemeData).recover {
      case ex: Exception =>
        ersLoggingAndAuditing.handleException(presubmissionData.schemeInfo, ex, "Exception during storing presubmission data in submission v2")
        false
    }

  }

  def getJson(schemeInfo: SchemeInfo): Future[Seq[SchemeData]] = {
    logger.debug("LFP -> 3. PresubmissionService.getJson () ")
    presubmissionRepository.getJson(schemeInfo).map(_.map(_.as[SchemeData]))
  }

  def removeJson(schemeInfo: SchemeInfo)(implicit hc: HeaderCarrier): Future[Boolean] = {
    presubmissionRepository.removeJson(schemeInfo).recover {
      case ex: Exception =>
        ersLoggingAndAuditing.handleException(schemeInfo, ex, "Exception during deleting presubmission data")
        false
    }

  }

  def compareSheetsNumber(expectedSheets: Int, schemeInfo: SchemeInfo)(implicit hc: HeaderCarrier): Future[Boolean] = {
    presubmissionRepository.count(schemeInfo).map { existingSheets =>
      existingSheets == expectedSheets
    }.recover {
      case ex: Exception =>
        ersLoggingAndAuditing.handleException(schemeInfo, ex, "Exception during checking for presubmission data")
        false
    }

  }

}
