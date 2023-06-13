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

import cats.implicits.catsStdInstancesForFuture
import common.ERSEnvelope
import models.SchemeDataMappingError
import common.ERSEnvelope.ERSEnvelope
import models.{SchemeData, SchemeInfo}
import play.api.Logging
import repositories.{PresubmissionMongoRepository, Repositories}
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing
import utils.Session

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.Try

class PresubmissionService @Inject()(repositories: Repositories, ersLoggingAndAuditing: ErsLoggingAndAuditing)
                                    (implicit ec: ExecutionContext) extends Logging {

  lazy val presubmissionRepository: PresubmissionMongoRepository = repositories.presubmissionRepository

  def storeJson(schemeData: SchemeData)(implicit hc: HeaderCarrier): ERSEnvelope[Boolean] =
    presubmissionRepository.storeJson(schemeData, Session.id(hc)).recover {
      case error =>
        logger.info(s"Storing data in pre-submission repository failed with error: [$error] for: ${schemeData.schemeInfo.basicLogMessage}")
        false
    }

  def getJson(schemeInfo: SchemeInfo)(implicit hc: HeaderCarrier): ERSEnvelope[scala.Seq[SchemeData]] = {
    presubmissionRepository.getJson(schemeInfo, Session.id(hc)).flatMap { result =>
      if (result.nonEmpty) {
        logger.info(s"Found data in pre-submission repository for: ${schemeInfo.basicLogMessage}, mapping to scheme data.")
       Try {
          result.map(_.as[SchemeData])
        }.toEither match {
          case Left(value) =>
            logger.info(s"Mapping data to SchemeData failed with error: [${value.getMessage}] for ${schemeInfo.basicLogMessage}")
            ERSEnvelope(SchemeDataMappingError(value.getMessage))
          case Right(value) => ERSEnvelope(value)
        }
      } else {
        logger.info(s"No data found in pre-submission repository for: ${schemeInfo.basicLogMessage}")
        ERSEnvelope(scala.Seq())
      }
    }
  }

  def removeJson(schemeInfo: SchemeInfo)(implicit hc: HeaderCarrier): ERSEnvelope[Boolean] =
    presubmissionRepository.removeJson(schemeInfo, Session.id(hc)).recover {
      case error =>
        logger.info(s"Removing data from pre-submission repository failed with error: [$error] for: ${schemeInfo.basicLogMessage}")
        false
    }

  def compareSheetsNumber(expectedSheets: Int, schemeInfo: SchemeInfo)(implicit hc: HeaderCarrier): ERSEnvelope[Boolean] = {
    presubmissionRepository.count(schemeInfo, Session.id(hc)).map { existingSheets =>
      existingSheets.toInt == expectedSheets
    }.recover {
      case error =>
        logger.info(s"Count data in pre-submission repository failed with error: [$error] for: ${schemeInfo.basicLogMessage}")
        false
    }
  }
}
