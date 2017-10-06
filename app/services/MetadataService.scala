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

package services

import models.ErsSummary
import play.api.Logger
import play.api.mvc.Request
import repositories.{Repositories, MetadataMongoRepository, MetadataRepository}
import play.api.libs.json.{JsError, JsSuccess, JsObject}
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier

object MetadataService extends MetadataService {

  override lazy val metadataRepository: MetadataMongoRepository = Repositories.metadataRepository
  override val ersLoggingAndAuditing: ErsLoggingAndAuditing = ErsLoggingAndAuditing

}

trait MetadataService {

  lazy val metadataRepository: MetadataRepository = ???
  val ersLoggingAndAuditing: ErsLoggingAndAuditing

  def storeErsSummary(ersSummary: ErsSummary)(implicit request: Request[_], hc: HeaderCarrier): Future[Boolean] = {
    metadataRepository.storeErsSummary(ersSummary).recover {
      case ex: Exception => {
        ersLoggingAndAuditing.handleException(ersSummary, ex, "Exception during storing ersSummary")
        false
      }
    }
  }

  def validateErsSummaryFromJson(json: JsObject): Option[ErsSummary] = {
    json.validate[ErsSummary] match {
      case ersSummary: JsSuccess[ErsSummary] => {
        val isMetadataValid: (Boolean, Option[String]) = validateErsSummary(ersSummary.value)
        if(isMetadataValid._1) {
          Some(ersSummary.value)
        }
        else {
          Logger.info("Invalid metadata. Json: " + json.toString() + ", errors: " + isMetadataValid._2.getOrElse(""))
          None
        }
      }
      case e: JsError => {
        Logger.info("Invalid request. Json: " + json.toString() + ", errors: " + JsError.toFlatJson(e).toString())
        None
      }
    }
  }

  def validateErsSummary(ersSummary: ErsSummary): (Boolean, Option[String])  = {

    val metaDataMap = Map(
      "isNilReturn" -> ersSummary.isNilReturn,
      "schemeRef" -> ersSummary.metaData.schemeInfo.schemeRef,
      "schemeType" -> ersSummary.metaData.schemeInfo.schemeType
    )

    for((metaDataKey, metaDataValue) <- metaDataMap) {
      metaDataKey match {
        case "isNilReturn"  =>  {
          if(metaDataValue != "1" && metaDataValue != "2") {
            return (false, Some(metaDataKey))
          }
        }
        case _ => {
          if (metaDataValue.isEmpty) {
            return (false, Some(metaDataKey))
          }
        }
      }
    }
    (true, None)
  }

}
