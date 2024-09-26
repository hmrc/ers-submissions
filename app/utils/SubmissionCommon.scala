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
import models.SchemeInfo
import play.api.Logging
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._
import uk.gov.hmrc.http.HttpResponse
import java.time._
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

class SubmissionCommon @Inject()(configUtils: ConfigUtils) extends Logging {

  private val EmptyJson: JsObject = Json.obj()
  private val EmptyJsonArray: JsArray = Json.arr()

  def getCorrelationID(response: HttpResponse): String = {
    response.header("CorrelationId") match {
      case Some(correlationId) => correlationId
      case None =>
        logger.warn(s"[SubmissionCommon][getCorrelationID] Response headers: ${response.headers.toString()}")
        "missingCorrelationId"
    }
  }

  def customFormat(value: LocalDateTime, formatInfo: Config): String = {
    formatInfo.getString("type") match {
      case "datetime" =>
        val jsonFormat = formatInfo.getString("json_format")
        val formatter = DateTimeFormatter.ofPattern(jsonFormat)

        value.format(formatter)
      case _ => value.toString
    }
  }

  def getNewField(configElem: Config, elemVal: JsValueWrapper): JsObject =
    Json.obj(configElem.getString("name") -> elemVal)

  def getConfigElemFieldValueByType(configElem: Config, fieldName: String): JsValueWrapper =
    configElem.getString("type") match {
      case "boolean" => configElem.getBoolean(fieldName)
      case "int"     => configElem.getInt(fieldName)
      case "string"  => configElem.getString(fieldName)
      case _         => throw new IllegalArgumentException("Undefined type")
    }

  def getConfigElemValue(configElem: Config): JsObject =
    getNewField(configElem, getConfigElemFieldValueByType(configElem, "value"))

  def getFileDataValue(configElem: Config, fileData: ListBuffer[Seq[String]], row: Option[Int], sheetName: Option[String], schemeInfo: Option[SchemeInfo]): JsObject = {
    if(configElem.hasPath("value")) getConfigElemValue(configElem)
    else {
      val elemColumn = configElem.getInt("column")
      val elemRow = row.getOrElse(configElem.getInt("row"))
      handleValueRetrieval(configElem, fileData, elemRow, elemColumn, sheetName, schemeInfo)
    }
  }

  def handleValueRetrieval(configElem: Config,
                            fileData: ListBuffer[Seq[String]],
                            elemRow: Int,
                            elemColumn: Int,
                            sheetName: Option[String],
                            schemeInfo: Option[SchemeInfo]): JsObject = {

    def getNewFieldWrapper(configElem: Config, value: String): Option[JsObject] = {
      val allowedTypes = List("string", "int", "double", "boolean")

      if (value.nonEmpty && configElem.hasPath("type") && allowedTypes.contains(configElem.getString("type"))) {
        val parsedConfigValue: JsValueWrapper = configElem.getString("type") match {
          case "string" => value
          case "int" => value.toIntOption
          case "double" => value.toDoubleOption
          case "boolean" => value.toUpperCase == configElem.getString("valid_value").toUpperCase
        }

        getNewFieldSafe(configElem, parsedConfigValue)
      } else {
        None
      }
    }

    val valueFromConfig: Option[JsObject] = for {
      row <- fileData.lift(elemRow)
      valueFromColumn <- row.lift(elemColumn)
      finalValue <- getNewFieldWrapper(configElem, valueFromColumn)
    } yield finalValue

    valueFromConfig.getOrElse(EmptyJson)
  }

  private def getNewFieldSafe(configElem: Config, elemVal: JsValueWrapper): Option[JsObject] =
    for {
      name <- if (configElem.hasPath("name")) Some(configElem.getString("name")) else None
      if name.nonEmpty
    } yield Json.obj(name -> elemVal)


  def getMetadataValue(configElem: Config, metadata: Object): JsObject = {

    if(configElem.hasPath("value")) {
      getConfigElemValue(configElem)
    }
    else if(configElem.hasPath("datetime_value") && configElem.getString("datetime_value") == "now") {
      val elemVal = Instant.now().toEpochMilli.toString
      getNewField(configElem, elemVal)
    }
    else {
      configUtils.extractField(configElem, metadata) match {
        case None =>
          if (configElem.hasPath("default_value")) {
            val elemType: String = configElem.getString("type")
            val elemVal: JsValueWrapper = if(elemType == "boolean") {
              configElem.getBoolean("default_value")
            }
            else {
              configElem.getString("default_value")
            }
            getNewField(configElem, elemVal)
          }
          else {
            EmptyJson
          }
        case value =>
          val elemType: String = configElem.getString("type")
          val elemVal: JsValueWrapper = elemType match {
            case "boolean" =>
              val valid_value = configElem.getString("valid_value")
              value.toString == valid_value
            case "string" =>
              Try(LocalDateTime.parse(value.toString, DateTimeFormatter.ISO_DATE_TIME)) match {
                case Success(time) => customFormat(time, configElem.getConfig("format"))
                case Failure(_) => value.toString
              }
            case _ =>
              JsNull
          }
          getNewField(configElem, elemVal)
      }
    }
  }

  def addObjectValue(elem: Config, elemVal: JsObject): JsObject = elemVal match {
    case EmptyJson => EmptyJson
    case _ => getNewField(elem, elemVal)
  }

  def addArrayValue(elem: Config, elemVal: List[JsObject]): JsObject = {
    if (elemVal.exists(_ != EmptyJson)) {
      getNewField(elem, elemVal)
    } else {
      EmptyJson
    }
  }

  def getObjectFromJson(fieldName: String, json: JsObject): JsObject = {
    (json \ fieldName).asOpt[JsObject].getOrElse(EmptyJson)
  }

  def getArrayFromJson(fieldName: String, json: JsObject): JsArray = {
    (json \ fieldName).asOpt[JsArray].getOrElse(EmptyJsonArray)
  }

  def mergeSheetData(configData: Config, oldJson: JsObject, newJson: JsObject): JsObject = (oldJson, newJson) match {
    case (EmptyJson, _) | (_, EmptyJson)=> newJson
    case _ =>
      val jsonField = configData.getString("name")
      getOptionalDataLocation(configData) match {
        case Some(dataLocationConfig) =>
          newJson ++ Json.obj(
            jsonField -> mergeSheetData(
              dataLocationConfig,
              getObjectFromJson(jsonField, oldJson),
              getObjectFromJson(jsonField, newJson)
            )
          )
        case None =>
          Json.obj(
            jsonField -> (getArrayFromJson(jsonField, oldJson) ++ getArrayFromJson(jsonField, newJson))
          )
      }
  }

  private def getOptionalDataLocation(config: Config): Option[Config] = {
    if (config.hasPath("data_location")) {
      Some(config.getConfig("data_location"))
    } else {
      None
    }
  }
  
}
