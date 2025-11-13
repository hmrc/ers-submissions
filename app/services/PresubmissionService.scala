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
import common.ERSEnvelope.ERSEnvelope
import models.{NoData, SchemeData, SchemeDataMappingError, SchemeInfo}
import repositories.{PresubmissionMongoRepository, Repositories}
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggingAndExceptions.ErsLogger
import utils.Session

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.Try

class PresubmissionService @Inject()(repositories: Repositories)(implicit ec: ExecutionContext) extends ErsLogger {

  lazy val presubmissionRepository: PresubmissionMongoRepository = repositories.presubmissionRepository

  def storeJson(schemeData: SchemeData)(implicit hc: HeaderCarrier): ERSEnvelope[Boolean] =
    presubmissionRepository.storeJson(schemeData, Session.id(hc))

  def getJson(schemeInfo: SchemeInfo)(implicit hc: HeaderCarrier): ERSEnvelope[scala.Seq[SchemeData]] = {
    presubmissionRepository.getJson(schemeInfo, Session.id(hc)).flatMap { result =>
      if (result.nonEmpty) {
        logInfo(s"Found data in pre-submission repository for: ${schemeInfo.basicLogMessage}, mapping to scheme data.")
        Try {
          result.map(_.as[SchemeData])
        }.toEither match {
          case Left(value) =>
            logError(s"Mapping data to SchemeData failed with error: [${value.getMessage}] for ${schemeInfo.basicLogMessage}")
            ERSEnvelope(SchemeDataMappingError(value.getMessage))
          case Right(value) => ERSEnvelope(value)
        }
      } else {
        logError(s"No data found in pre-submission repository for: ${schemeInfo.basicLogMessage}")
        ERSEnvelope(NoData())
      }
    }
  }

  def removeJson(schemeInfo: SchemeInfo)(implicit hc: HeaderCarrier): ERSEnvelope[Boolean] =
    presubmissionRepository.removeJson(schemeInfo, Session.id(hc)).flatMap {
      case result if result.wasAcknowledged() && result.getDeletedCount > 0 =>
        logInfo(s"Deleted ${result.getDeletedCount} documents from presubmission repository for: ${schemeInfo.basicLogMessage}")
        ERSEnvelope(true)
      case result if result.wasAcknowledged() =>
        logInfo(s"No data to delete from presubmission repository for: ${schemeInfo.basicLogMessage}")
        ERSEnvelope(NoData())
      case _ =>
        logger.warn(s"Deleting old presubmission data failed for: ${schemeInfo.basicLogMessage}")
        ERSEnvelope(false)
    }

  def compareSheetsNumber(expectedSheets: Int, schemeInfo: SchemeInfo)(implicit hc: HeaderCarrier): ERSEnvelope[(Boolean, Long)] =
    presubmissionRepository.count(schemeInfo, Session.id(hc)).map { existingSheets =>
      (existingSheets.toInt == expectedSheets, existingSheets)
    }
}
