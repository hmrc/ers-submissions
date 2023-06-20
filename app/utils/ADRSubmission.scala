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

package utils

import com.typesafe.config.Config
import common.ERSEnvelope
import common.ERSEnvelope.ERSEnvelope
import models._
import play.api.Logging
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Request
import services.PresubmissionService
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggingAndRexceptions.ADRExceptionEmitter

import javax.inject.Inject
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.concurrent.ExecutionContext
import scala.util.Try

class ADRSubmission @Inject()(submissionCommon: SubmissionCommon,
                              presubmissionService: PresubmissionService,
                              adrExceptionEmitter: ADRExceptionEmitter,
                              configUtils: ConfigUtils)
                             (implicit ec: ExecutionContext) extends Logging {

  def generateSubmission(ersSummary: ErsSummary)(implicit request: Request[_], hc: HeaderCarrier): ERSEnvelope[JsObject] = {
    val schemeType: String = ersSummary.metaData.schemeInfo.schemeType.toUpperCase()

    if (ersSummary.isNilReturn == IsNilReturn.False.toString) {
      createSubmissionJson(ersSummary, schemeType)
    }
    else {
      ERSEnvelope(createRootJson(Json.obj(), ersSummary, schemeType))
    }
  }

  def createSubmissionJson(ersSummary: ErsSummary, schemeType: String)(implicit request: Request[_], hc: HeaderCarrier): ERSEnvelope[JsObject] =
    for {
      sheetsDataJson <- createSheetsJson(Json.obj(), ersSummary, schemeType)
    } yield createRootJson(sheetsDataJson, ersSummary, schemeType)

  def createSheetsJson(sheetsJson: JsObject, ersSummary: ErsSummary, schemeType: String)(implicit request: Request[_], hc: HeaderCarrier): ERSEnvelope[JsObject] = {
    var result = sheetsJson

    for {
      schemeDataSeq <- presubmissionService.getJson(ersSummary.metaData.schemeInfo)
    } yield {
      val sheetNamesAndDataPresent = schemeDataSeq.forall(fd => fd.sheetName.nonEmpty && fd.data.nonEmpty)
      if (schemeDataSeq.nonEmpty && sheetNamesAndDataPresent) {
        logger.info(s"Found data in pre-submission repository, mapped successfully. File data list size: ${schemeDataSeq.size}, ${ersSummary.metaData.schemeInfo.basicLogMessage}")
      } else {
        logger.warn(s"No data returned from pre-submission repository or data is incomplete: ${ersSummary.metaData.schemeInfo.basicLogMessage}")
      }
      for (fileData <- schemeDataSeq) {
        Try {
          val sheetName: String = fileData.sheetName
          val configData: Config = configUtils.getConfigData(s"$schemeType/$sheetName", sheetName, ersSummary)
          val data: JsObject = buildJson(configData, fileData.data.get)
          submissionCommon.mergeSheetData(configData.getConfig("data_location"), result, data)
        }.toEither match {
          case Right(jsObject) => {
            result = result ++ jsObject
          }
          case Left(error) =>
            logger.warn(s"Failed to create Json from sheets data for: ${ersSummary.metaData.schemeInfo.basicLogMessage}. Exception: ${error.getMessage}")
            JsonFromSheetsCreationError(error.getMessage)
        }
      }
      result
    }
  }

  def createRootJson(sheetsJson: JsObject, ersSummary: ErsSummary, schemeType: String)(implicit request: Request[_], hc: HeaderCarrier): JsObject = {
    val rootConfigData: Config = configUtils.getConfigData(s"$schemeType/$schemeType", schemeType, ersSummary)
    buildRoot(rootConfigData, ersSummary, sheetsJson, ersSummary, schemeType)
  }

  def buildRoot(configData: Config, metadata: Object, sheetsJson: JsObject, ersSummary: ErsSummary, schemeType: String)
               (implicit request: Request[_], hc: HeaderCarrier): JsObject = {
    import scala.jdk.CollectionConverters._

    var json: JsObject = Json.obj()

    val fieldsConfigList = configData.getConfigList("fields").asScala

    for (elem <- fieldsConfigList) {
      elem.getString("type") match {
        case "object" =>
          val elemVal = buildRoot(elem, metadata, sheetsJson, ersSummary, schemeType)
          json ++= submissionCommon.addObjectValue(elem, elemVal)
        case "array" =>
          if (elem.hasPath("values")) {
            val elVal = for (el <- elem.getConfigList("values").asScala) yield {
              val ev = configUtils.extractField(el, metadata)
              val valid_value = el.getString("valid_value")
              if (ev.toString == valid_value) {
                val elName = el.getString("name")
                Json.obj(elName -> el.getString("value"))
              }
              else {
                Json.obj()
              }
            }
            if (elVal.isInstanceOf[ArrayBuffer[JsObject]]) {
              val filtered = elVal.filterNot(c => c.equals(Json.obj()))
              if (filtered.count(_ != Json.obj()) > 0) {
                val elemName = elem.getString("name")
                json ++= Json.obj(
                  elemName -> filtered
                )
              }
            }
          }
          else {
            val arrayData = configUtils.extractField(elem, metadata)
            arrayData match {
              case value: List[Object@unchecked] =>
                val elemVal = for (el <- value) yield buildRoot(elem, el, sheetsJson, ersSummary, schemeType)
                json ++= submissionCommon.addArrayValue(elem, elemVal)
              case _ =>
            }
          }
        case "common" =>
          val loadConfig: String = elem.getString("load")
          val configData: Config = configUtils.getConfigData(s"common/$loadConfig", loadConfig, ersSummary)
          json ++= buildRoot(configData, metadata, sheetsJson, ersSummary, schemeType)
        case "sheetData" =>
          json ++= sheetsJson
        case _ => json ++= submissionCommon.getMetadataValue(elem, metadata)
      }
    }
    json
  }

  def buildJson(configData: Config, fileData: ListBuffer[scala.Seq[String]], row: Option[Int] = None)
               (implicit request: Request[_], hc: HeaderCarrier): JsObject = {
    import scala.jdk.CollectionConverters._

    var json: JsObject = Json.obj()

    val fieldsConfigList = configData.getConfigList("fields").asScala
    for (elem <- fieldsConfigList) {
      elem.getString("type") match {
        case "object" =>
          val elemVal = buildJson(elem, fileData, row)
          json ++= submissionCommon.addObjectValue(elem, elemVal)
        case "array" =>
          if (fileData.nonEmpty) {
            if(row.isEmpty) {
              val elemVal: List[JsObject] = for (row <- fileData.indices.toList) yield buildJson(elem, fileData, Some(row))
              json ++= submissionCommon.addArrayValue(elem, elemVal)
            }
            else {
              val elemVal = buildJson(elem, fileData, row)
              json ++= submissionCommon.addArrayValue(elem, List(elemVal))
            }
          }
        case _ => json ++= submissionCommon.getFileDataValue(elem, fileData, row)
      }
    }
    json
  }
}
