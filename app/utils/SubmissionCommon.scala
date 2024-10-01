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

  def getFileDataValue(configElem: Config, fileData: ListBuffer[Seq[String]], row: Option[Int]): JsObject = {
    if (configElem.hasPath("value")) getConfigElemValue(configElem)
    else {
      val elemColumn = configElem.getInt("column")
      val elemRow = row.getOrElse(configElem.getInt("row"))
      handleValueRetrieval(configElem, fileData, elemRow, elemColumn)
    }
  }


  /** Attempt to retrieve a formatted value from the file data, using 'type', 'valid_value' and 'name' info from config.
   *
   * @param configElem   config used to check for 'type', 'valid_value', and 'name' keys
   * @param fileData     data to access values from
   * @param elemRow     row to access within the data
   * @param elemColumn  column to access within the row
   * @return            the parsed value from the row & column, as a JSON object e.g. {"dateOfGrant":"2015-12-09"},
   *                    or an empty json object
   */
  def handleValueRetrieval(configElem: Config,
                           fileData: ListBuffer[Seq[String]],
                           elemRow: Int,
                           elemColumn: Int): JsObject = {
    val valueFromConfig: Option[JsObject] = for {
      row: Seq[String] <- fileData.lift(elemRow)
      valueFromColumn: String <- row.lift(elemColumn)
      finalValue: JsObject <- getNewFieldOpt(configElem, valueFromColumn)
    } yield finalValue

    valueFromConfig.getOrElse(EmptyJson)
  }


  /**
   *
   * @param configRow          the row from which to access the 'name' and 'valid_value' fields
   * @param valueFromColumn   value from fileData at the previously specified row & column
   * @return                  a parsed value with the the correct name and value, or an empty JSON object
   */
  private def getNewFieldOpt(configRow: Config, valueFromColumn: String): Option[JsObject] = {
    for {
      typeFromConfig <- configUtils.getConfigStringOpt(configRow, "type")
      result <- {
        val allowedTypes = List("string", "int", "double", "boolean")

        if (valueFromColumn.nonEmpty && allowedTypes.contains(typeFromConfig)) {
          if (typeFromConfig == "boolean") {
            // For the boolean case, we do another check against the 'valid_value' config element.
            // This case is handled separately to create the expected empty JSON object,
            // and not an object like { "name" : null } as JsValueWrapper converts empty options to null
            val parsedBooleanConfigValueOpt: Option[JsValueWrapper] = getNewFieldOptBooleanCase(configRow, valueFromColumn)

            parsedBooleanConfigValueOpt
              .flatMap(parsedBooleanConfigValue =>
                configUtils
                  .getConfigStringOpt(configRow, "name")
                  .map(nameFromConfig => Json.obj(nameFromConfig -> parsedBooleanConfigValue))
              )
          } else {

            val parsedConfigValue: JsValueWrapper = typeFromConfig match {
              case "string" => valueFromColumn
              case "int" => valueFromColumn.toIntOption
              case "double" => valueFromColumn.toDoubleOption
            }

            configUtils
              .getConfigStringOpt(configRow, "name")
              .map(nameFromConfig => Json.obj(nameFromConfig -> parsedConfigValue))
          }
        } else {
          None
        }
      }
    } yield result
  }

  private def getNewFieldOptBooleanCase(configElem: Config, valueFromColumn: String): Option[JsValueWrapper] = {
    configUtils
      .getConfigStringOpt(configElem, "valid_value")
      .map(valueFromConfig => valueFromColumn.toUpperCase == valueFromConfig.toUpperCase)
  }

  def getMetadataValue(configElem: Config, metadata: Object): JsObject = {

    if (configElem.hasPath("value")) {
      getConfigElemValue(configElem)
    }
    else if (configElem.hasPath("datetime_value") && configElem.getString("datetime_value") == "now") {
      val elemVal = Instant.now().toEpochMilli.toString
      getNewField(configElem, elemVal)
    }
    else {
      configUtils.extractField(configElem, metadata) match {
        case None =>
          if (configElem.hasPath("default_value")) {
            val elemType: String = configElem.getString("type")
            val elemVal: JsValueWrapper = if (elemType == "boolean") {
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
    case (EmptyJson, _) | (_, EmptyJson) => newJson
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
