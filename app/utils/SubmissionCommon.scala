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
import org.joda.time.DateTime
import org.mongodb.scala.bson.BsonObjectId
import play.api.Logging
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._
import uk.gov.hmrc.http.HttpResponse

import java.text.SimpleDateFormat
import javax.inject.Inject
import scala.collection.mutable.ListBuffer

class SubmissionCommon @Inject()(configUtils: ConfigUtils) extends Logging{

  def getCorrelationID(response: HttpResponse): String = {
    val correlationIdRegEx = "CorrelationId -> Buffer\\((\\w+-\\w+-\\w+-\\w+-\\w+)".r
    correlationIdRegEx.findFirstMatchIn(
      response.headers.toString()
    ).map(
      _ group 1
    ).getOrElse("")
  }

  def getBSONObjectID(objectID: String): BsonObjectId = {
    try {
      BsonObjectId.apply(
        objectID.replace("BSONObjectID(\"", "").replace("\")", "")
      )
    }
    catch {
      case ex: Exception =>
        logger.error(s"Error creating ObjectID from $objectID, exception: ${ex.getMessage}")
        throw ex
    }
  }

  def castToDouble(value: String): Option[Double] = {
    try {
      Some(value.toDouble)
    }
    catch {
      case ex: Exception => None
    }
  }

  def castToInt(value: String): Option[Int] = {
    try {
      Some(value.toDouble.toInt)
    }
    catch {
      case ex: Exception => None
    }
  }

  def customFormat(value: DateTime, formatInfo: Config): String = {
    formatInfo.getString("type") match {
      case "datetime" =>
        val jsonFormat = formatInfo.getString("json_format")
        val jsonDateTimeFormat = new SimpleDateFormat(jsonFormat)

        jsonDateTimeFormat.format(
          value.toDate
        )
      case _ => value.toString()
    }
  }

  def getNewField(configElem: Config, elemVal: JsValueWrapper): JsObject = {
    val elemName: String = configElem.getString("name")
    Json.obj(
      elemName -> elemVal
    )
  }

  def getConfigElemFieldValueByType(configElem: Config, fieldName: String): JsValueWrapper = {
    val elemType: String = configElem.getString("type")
    elemType match {
      case "boolean" => configElem.getBoolean(fieldName)
      case "int" => configElem.getInt(fieldName)
      case "string" => configElem.getString(fieldName)
      case _ => throw new Exception("Undefined type")
    }
  }

  def getConfigElemValue(configElem: Config): JsObject = {
    val elemVal: JsValueWrapper = getConfigElemFieldValueByType(configElem, "value")
    getNewField(configElem, elemVal)
  }

  def getFileDataValue(configElem: Config, fileData: ListBuffer[Seq[String]], row: Option[Int]): JsObject = {

    if(configElem.hasPath("value")) {
      getConfigElemValue(configElem)
    }
    else {
      val elemColumn = configElem.getInt("column")
      val elemRow = row.getOrElse(configElem.getInt("row"))
      val value = fileData(elemRow)(elemColumn)

      if (value.nonEmpty) {
        val elemType: String = configElem.getString("type")
        val elemVal: JsValueWrapper = elemType match {
          case "string" => value
          case "int" => castToInt(value)
          case "double" => castToDouble(value)
          case "boolean" =>
            val valid_value = configElem.getString("valid_value")
            value.toUpperCase == valid_value.toUpperCase
        }
        getNewField(configElem, elemVal)
      }
      else {
        Json.obj()
      }
    }
  }

  def getMetadataValue(configElem: Config, metadata: Object): JsObject = {

    if(configElem.hasPath("value")) {
      getConfigElemValue(configElem)
    }
    else if(configElem.hasPath("datetime_value") && configElem.getString("datetime_value") == "now") {
      val elemVal = System.currentTimeMillis().toString
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
            Json.obj()
          }
        case value =>
          val elemType: String = configElem.getString("type")
          val elemVal: JsValueWrapper = elemType match {
            case "boolean" =>
              val valid_value = configElem.getString("valid_value")
              value.toString == valid_value
            case "string" =>
              value match {
                case time: DateTime =>
                  customFormat(time, configElem.getConfig("format"))
                case _: String =>
                  value.toString
                case _ =>
                  JsNull
              }
          }
          getNewField(configElem, elemVal)
      }
    }
  }

  def addObjectValue(elem: Config, elemVal: JsObject): JsObject = {
    if(elemVal == Json.obj()) {
      return Json.obj()
    }
    getNewField(elem, elemVal)
  }

  def addArrayValue(elem: Config, elemVal:  List[JsObject]): JsObject = {
    if (elemVal.count(_ != Json.obj()) == 0) {
      return Json.obj()
    }
    getNewField(elem, elemVal)
  }

  def getObjectFromJson(fieldName: String, json: JsObject): JsObject = {
    (json \ fieldName).asOpt[JsObject].getOrElse(Json.obj())
  }

  def getArrayFromJson(fieldName: String, json: JsObject): JsArray = {
    (json \ fieldName).asOpt[JsArray].getOrElse(Json.arr())
  }

  def mergeSheetData(configData: Config, oldJson: JsObject, newJson: JsObject): JsObject = {
    if(oldJson == Json.obj()) {
      return newJson
    }
    if(newJson == Json.obj()) {
      return newJson
    }
    val jsonField = configData.getString("name")
    if(configData.hasPath("data_location")) {
      return newJson ++ Json.obj(
        jsonField -> mergeSheetData(
          configData.getConfig("data_location"),
          getObjectFromJson(jsonField, oldJson),
          getObjectFromJson(jsonField, newJson)
        )
      )
    }
    Json.obj(
      jsonField -> (getArrayFromJson(jsonField, oldJson) ++ getArrayFromJson(jsonField, newJson))
    )
  }
}
