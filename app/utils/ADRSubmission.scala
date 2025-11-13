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
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Request
import services.PresubmissionService
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggingAndExceptions.ErsLogger

import javax.inject.Inject
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.concurrent.ExecutionContext

class ADRSubmission @Inject()(submissionCommon: SubmissionCommon,
                              presubmissionService: PresubmissionService,
                              configUtils: ConfigUtils)
                             (implicit ec: ExecutionContext) extends ErsLogger {

  private val EmptyJson: JsObject = Json.obj()

  def generateSubmission(ersSummary: ErsSummary)(implicit request: Request[_], hc: HeaderCarrier): ERSEnvelope[JsObject] = {
    val schemeType: String = ersSummary.metaData.schemeInfo.schemeType.toUpperCase()
    logInfo(s"[ADRSubmission][generateSubmission] ${ersSummary.basicLogMessage} ${ersSummary.metaData.schemeInfo.basicLogMessage}")

    if (ersSummary.isNilReturn == IsNilReturn.False.toString) {
      createSubmissionJson(ersSummary, schemeType)
    }
    else {
      createRootJson(EmptyJson, ersSummary, schemeType)
    }
  }

  def createSubmissionJson(ersSummary: ErsSummary, schemeType: String)(implicit request: Request[_], hc: HeaderCarrier): ERSEnvelope[JsObject] =
    for {
      sheetsDataJson <- createSheetsJson(EmptyJson, ersSummary, schemeType)
      rootJson <- createRootJson(sheetsDataJson, ersSummary, schemeType)
    } yield rootJson

  def createSheetsJson(sheetsJson: JsObject, ersSummary: ErsSummary, schemeType: String)(implicit request: Request[_], hc: HeaderCarrier): ERSEnvelope[JsObject] = {
    presubmissionService.getJson(ersSummary.metaData.schemeInfo).map { schemeDataSeq =>
      val sheetNamesAndDataPresent = schemeDataSeq.forall(fd => fd.sheetName.nonEmpty && fd.data.nonEmpty)

      if (schemeDataSeq.nonEmpty && sheetNamesAndDataPresent) {
        logInfo(s"[ADRSubmission][createSheetsJson] Found data in pre-submission repository, mapped successfully. File data list size: ${schemeDataSeq.size}, ${ersSummary.metaData.schemeInfo.basicLogMessage}")
      } else {
        logWarn(s"[ADRSubmission][createSheetsJson] No data returned from pre-submission repository or data is incomplete: ${ersSummary.metaData.schemeInfo.basicLogMessage}")
      }

      schemeDataSeq.foldLeft(sheetsJson) { (result, fileData) =>
        val sheetName: String = fileData.sheetName
        val configData: Config = configUtils.getConfigData(s"$schemeType/$sheetName", sheetName, ersSummary)
        val data: JsObject = buildJson(configData, fileData.data.get, None, Some(sheetName), Some(fileData.schemeInfo))

        result ++ submissionCommon.mergeSheetData(configData.getConfig("data_location"), result, data)
      }
    }
  }

  def createRootJson(sheetsJson: JsObject, ersSummary: ErsSummary, schemeType: String)(implicit request: Request[_], hc: HeaderCarrier): ERSEnvelope[JsObject] = {
    val rootConfigData: Config = configUtils.getConfigData(s"$schemeType/$schemeType", schemeType, ersSummary)
    ERSEnvelope(buildRoot(rootConfigData, ersSummary, sheetsJson, ersSummary, schemeType))
  }

  import scala.jdk.CollectionConverters._

  def buildRoot(configData: Config, metadata: Object, sheetsJson: JsObject, ersSummary: ErsSummary, schemeType: String)
               (implicit request: Request[_], hc: HeaderCarrier): JsObject = {
    @annotation.tailrec
    def buildRootTail(fieldsConfigList: List[Config], json: JsObject): JsObject = {
      fieldsConfigList match {
        case Nil => json
        case elem :: tail =>
          elem.getString("type") match {
            case "object" =>
              val elemVal = buildRoot(elem, metadata, sheetsJson, ersSummary, schemeType)
              val updatedJson = json ++ submissionCommon.addObjectValue(elem, elemVal)
              buildRootTail(tail, updatedJson)
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
                    EmptyJson
                  }
                }
                if (elVal.isInstanceOf[ArrayBuffer[JsObject]]) {
                  val filtered = elVal.filterNot(c => c.equals(EmptyJson))
                  if (filtered.count(_ != EmptyJson) > 0) {
                    val elemName = elem.getString("name")
                    val updatedJson = json ++ Json.obj(elemName -> filtered)
                    buildRootTail(tail, updatedJson)
                  } else {
                    buildRootTail(tail, json)
                  }
                } else {
                  buildRootTail(tail, json)
                }
              } else {
                val arrayData = configUtils.extractField(elem, metadata)
                arrayData match {
                  case value: List[Object@unchecked] =>
                    val elemVal = for (el <- value) yield buildRoot(elem, el, sheetsJson, ersSummary, schemeType)
                    val updatedJson = json ++ submissionCommon.addArrayValue(elem, elemVal)
                    buildRootTail(tail, updatedJson)
                  case _ =>
                    buildRootTail(tail, json)
                }
              }
            case "common" =>
              val loadConfig: String = elem.getString("load")
              val configData: Config = configUtils.getConfigData(s"common/$loadConfig", loadConfig, ersSummary)
              val updatedJson = json ++ buildRoot(configData, metadata, sheetsJson, ersSummary, schemeType)
              buildRootTail(tail, updatedJson)
            case "sheetData" =>
              val updatedJson = json ++ sheetsJson
              buildRootTail(tail, updatedJson)
            case _ =>
              val updatedJson = json ++ submissionCommon.getMetadataValue(elem, metadata)
              buildRootTail(tail, updatedJson)
          }
      }
    }

    val fieldsConfigList = configData.getConfigList("fields").asScala.toList
    buildRootTail(fieldsConfigList, EmptyJson)
  }

  def buildJson(configData: Config, fileData: ListBuffer[scala.Seq[String]], row: Option[Int] = None, sheetName: Option[String] = None, schemeInfo: Option[SchemeInfo] = None)
               (implicit request: Request[_], hc: HeaderCarrier): JsObject = {
    import scala.jdk.CollectionConverters._

    val fieldsConfigList = configData.getConfigList("fields").asScala
    fieldsConfigList.foldLeft(EmptyJson) { (json, elem) =>
      elem.getString("type") match {
        case "object" =>
          val elemVal = buildJson(elem, fileData, row, sheetName, schemeInfo)
          json ++ submissionCommon.addObjectValue(elem, elemVal)
        case "array" =>
          if (fileData.nonEmpty) {
            if (row.isEmpty) {
              val elemVal: List[JsObject] = fileData.indices.toList.map(row => buildJson(elem, fileData, Some(row), sheetName, schemeInfo))
              json ++ submissionCommon.addArrayValue(elem, elemVal)
            } else {
              val elemVal = buildJson(elem, fileData, row, sheetName, schemeInfo)
              json ++ submissionCommon.addArrayValue(elem, List(elemVal))
            }
          }
          else json
        case _ => json ++ submissionCommon.getFileDataValue(elem, fileData, row)
      }
    }
  }
}
