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

import common.ERSEnvelope.ERSEnvelope
import models.ErsSummary
import play.api.Logging
import play.api.libs.json.{JsError, JsObject, JsResult, JsSuccess}
import repositories.MetadataMongoRepository
import services.audit.AuditEvents
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggingAndExceptions.ErsLogger
import utils.Session

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class MetadataService @Inject()(metadataMongoRepository: MetadataMongoRepository, auditEvents: AuditEvents)
                               (implicit ec: ExecutionContext) extends ErsLogger {

  lazy val metadataRepository: MetadataMongoRepository = metadataMongoRepository

  def storeErsSummary(ersSummary: ErsSummary)(implicit hc: HeaderCarrier): ERSEnvelope[Boolean] =
    metadataRepository.storeErsSummary(ersSummary, Session.id(hc)).recover {
      case error =>
        logError(s"[MetadataService][storeErsSummary] Storing data in metadata repository failed with error: [$error] for: ${ersSummary.metaData.schemeInfo}")
        auditEvents.auditError("storeErsSummary", s"Storing data in metadata repository failed with error: [$error]")
        false
    }

  def validateErsSummaryFromJson(json: JsObject): JsResult[ErsSummary] = {
    json.validate[ErsSummary] match {
      case ersSummary: JsSuccess[ErsSummary] =>
        val isMetadataValid: (Boolean, Option[String]) = validateErsSummary(ersSummary.value)
        if (isMetadataValid._1) {
          ersSummary
        }
        else {
          logWarn("[MetadataService][validateErsSummaryFromJson] Invalid metadata. Errors: " + isMetadataValid._2.getOrElse(""))
          JsError(s"Metadata invalid: ${isMetadataValid._2.getOrElse("")}")
        }
      case error: JsError =>
        logWarn("[MetadataService][validateErsSummaryFromJson] Invalid request. Errors: " + JsError.toJson(error).toString())
        error
    }
  }

  def validateErsSummary(ersSummary: ErsSummary): (Boolean, Option[String]) = {
    val nilReturnInvalid = ersSummary.isNilReturn != "1" && ersSummary.isNilReturn != "2"
    val schemeRefEmpty = ersSummary.metaData.schemeInfo.schemeRef.isEmpty
    val schemeTypeEmpty = ersSummary.metaData.schemeInfo.schemeType.isEmpty

    (nilReturnInvalid, schemeRefEmpty,schemeTypeEmpty) match {
      case (true, _, _) => (false, Some("isNilReturn"))
      case (_, true, _) => (false, Some("schemeRef"))
      case (_, _, true) => (false, Some("schemeType"))
      case _ => (true, None)
    }
  }
}
