/*
 * Copyright 2020 HM Revenue & Customs
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
import javax.inject.Inject
import models._
import play.api.Logger
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Request
import services.PresubmissionService
import utils.LoggingAndRexceptions.ADRExceptionEmitter

import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.concurrent.Future
import util.control.Breaks._
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier

class ADRSubmission @Inject()(submissionCommon: SubmissionCommon,
                              presubmissionService: PresubmissionService,
                              adrExceptionEmitter: ADRExceptionEmitter,
                              configUtils: ConfigUtils) {

  def generateSubmission()(implicit request: Request[_], hc: HeaderCarrier, ersSummary: ErsSummary): Future[JsObject] = {
    Logger.debug("LFP -> 1. generateSubmission :START ")
    implicit val schemeType: String = ersSummary.metaData.schemeInfo.schemeType.toUpperCase()

    if (ersSummary.isNilReturn == IsNilReturn.False.toString) {
      createSubmissionJson() map {res => res}
    }
    else {
      Future(createRootJson(Json.obj())).map(res => res)
    }
  }

  def createSubmissionJson()(implicit request: Request[_], hc: HeaderCarrier, ersSummary: ErsSummary, schemeType: String): Future[JsObject] = {
    createSheetsJson(Json.obj()) flatMap { sheetsDataJson =>
      Logger.debug("LFP -> 7. Json Creation complete --> ")
      Future(createRootJson(sheetsDataJson)) map { res => res}
    }
  }

  def createSheetsJson(sheetsJson: JsObject)(implicit request: Request[_], hc: HeaderCarrier, ersSummary: ErsSummary, schemeType: String): Future[JsObject] = {
    var result = sheetsJson
    Logger.debug("LFP -> 2. createSheetsJson () ")
    presubmissionService.getJson(ersSummary.metaData.schemeInfo).map { fileDataList =>
      Logger.debug("LFP -> 5. Json retireved + list = " + fileDataList.size)
      for(fileData <- fileDataList) {
        Logger.debug(s" LFP -> 6. data record  is --> ${fileData.sheetName}" )
        val sheetName: String = fileData.sheetName
        val configData: Config = configUtils.getConfigData(s"${schemeType}/${sheetName}", sheetName)
        val data: JsObject = buildJson(configData, fileData.data.get)
        result = result ++ submissionCommon.mergeSheetData(configData.getConfig("data_location"), result, data)
      }
      result
    }.recover {
      case ex: Exception => adrExceptionEmitter.emitFrom(
        ersSummary.metaData,
        Map(
          "message" -> "Exception during findAndUpdate presubmission data",
          "context" -> "ADRSubmission.createSheetsJson"
        ),
        Some(ex)
      )
    }
  }

  def createRootJson(sheetsJson: JsObject)(implicit request: Request[_], hc: HeaderCarrier, ersSummary: ErsSummary, schemeType: String): JsObject = {
    val rootConfigData: Config = configUtils.getConfigData(s"${schemeType}/${schemeType}", schemeType)
    buildRoot(rootConfigData, ersSummary, sheetsJson)
  }

  def buildRoot(configData: Config, metadata: Object, sheetsJson: JsObject)(implicit request: Request[_], hc: HeaderCarrier, ersSummary: ErsSummary, schemeType: String): JsObject = {
    import scala.collection.JavaConversions._

    var json: JsObject = Json.obj()

    val fieldsConfigList = configData.getConfigList("fields")

    for (elem <- fieldsConfigList) {
      elem.getString("type") match {
        case "object" => {
          val elemVal = buildRoot(elem, metadata, sheetsJson)
          json ++= submissionCommon.addObjectValue(elem, elemVal)
        }
        case "array" => {
          if (elem.hasPath("values")) {
            val elVal = for (el <- elem.getConfigList("values")) yield {
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
            if (arrayData.isInstanceOf[List[Object @unchecked]]) {
              val elemVal = for (el <- arrayData.asInstanceOf[List[Object]]) yield buildRoot(elem, el, sheetsJson)
              json ++= submissionCommon.addArrayValue(elem, elemVal)
            }
          }
        }
        case "common" => {
          val loadConfig: String = elem.getString("load")
          val configData: Config = configUtils.getConfigData(s"common/${loadConfig}", loadConfig)
          json ++= buildRoot(configData, metadata, sheetsJson)
        }
        case "sheetData" => {
          json ++= sheetsJson
        }
        case _ => json ++= submissionCommon.getMetadataValue(elem, metadata)
      }
    }

    json
  }

  def buildJson(configData: Config, fileData: ListBuffer[Seq[String]], row: Option[Int] = None)(implicit request: Request[_], hc: HeaderCarrier): JsObject = {
    import scala.collection.JavaConversions._

    var json: JsObject = Json.obj()

    val fieldsConfigList = configData.getConfigList("fields")
    for (elem <- fieldsConfigList) {
      elem.getString("type") match {
        case "object" => {
          val elemVal = buildJson(elem, fileData, row)
          json ++= submissionCommon.addObjectValue(elem, elemVal)
        }
        case "array" => {
          if (fileData.length > 0) {
            if(row.isEmpty) {
              val elemVal: List[JsObject] = for (row <- (0 to (fileData.length - 1)).toList) yield buildJson(elem, fileData, Some(row))
              json ++= submissionCommon.addArrayValue(elem, elemVal)
            }
            else {
              val elemVal = buildJson(elem, fileData, row)
              json ++= submissionCommon.addArrayValue(elem, List(elemVal))
            }
          }
        }
        case _ => json ++= submissionCommon.getFileDataValue(elem, fileData, row)
      }
    }
    json
  }

}
