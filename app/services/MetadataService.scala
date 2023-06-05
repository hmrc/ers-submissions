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

import models.ErsSummary
import play.api.Logging
import play.api.libs.json.{JsError, JsObject, JsSuccess}
import repositories.MetadataMongoRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MetadataService @Inject()(metadataMongoRepository: MetadataMongoRepository,
                                ersLoggingAndAuditing: ErsLoggingAndAuditing)(implicit ec: ExecutionContext)
  extends Logging {

  lazy val metadataRepository: MetadataMongoRepository = metadataMongoRepository

  def storeErsSummary(ersSummary: ErsSummary)(implicit hc: HeaderCarrier): Future[Boolean] = {
    metadataRepository.storeErsSummary(ersSummary).recover {
      case ex: Exception =>
        ersLoggingAndAuditing.handleException(ersSummary, ex, "Exception during storing ersSummary")
        false
    }
  }

  def validateErsSummaryFromJson(json: JsObject): Option[ErsSummary] = {
    json.validate[ErsSummary] match {
      case ersSummary: JsSuccess[ErsSummary] =>
        val isMetadataValid: (Boolean, Option[String]) = validateErsSummary(ersSummary.value)
        if(isMetadataValid._1) {
          Some(ersSummary.value)
        }
        else {
          logger.info("Invalid metadata. Json: " + json.toString() + ", errors: " + isMetadataValid._2.getOrElse(""))
          None
        }
      case e: JsError =>
        logger.info("Invalid request. Json: " + json.toString() + ", errors: " + JsError.toJson(e).toString())
        None
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
