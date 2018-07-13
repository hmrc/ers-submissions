/*
 * Copyright 2018 HM Revenue & Customs
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

import java.text.SimpleDateFormat
import com.typesafe.config.Config
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID
import scala.collection.mutable.ListBuffer
import uk.gov.hmrc.http.HttpResponse

object SubmissionCommon extends SubmissionCommon

trait SubmissionCommon extends ConfigUtils {

  def getCorrelationID(response: HttpResponse): String = {
    val correlationIdRegEx = "CorrelationId -> Buffer\\((\\w+-\\w+-\\w+-\\w+-\\w+)".r
    correlationIdRegEx.findFirstMatchIn(
      response.allHeaders.toString()
    ).map(
      _ group 1
    ).getOrElse("")
  }

  def getBSONObjectID(objectID: String): BSONObjectID = {
    try {
      BSONObjectID.parse(
        objectID.replace("BSONObjectID(\"", "").replace("\")", "")
      ).get
    }
    catch {
      case ex: Exception => {
        Logger.error(s"Error creating ObjectID from ${objectID}, exception: ${ex.getMessage}")
        throw ex
      }
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
      case "datetime" => {
        val jsonFormat = formatInfo.getString("json_format")
        val jsonDateTimeFormat = new SimpleDateFormat(jsonFormat)

        jsonDateTimeFormat.format(
          value.toDate
        )
      }
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

      if (!value.isEmpty) {
        val elemType: String = configElem.getString("type")
        val elemVal: JsValueWrapper = elemType match {
          case "string" => value
          case "int" => castToInt(value)
          case "double" => castToDouble(value)
          case "boolean" => {
            val valid_value = configElem.getString("valid_value")
            (value.toUpperCase == valid_value.toUpperCase)
          }
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
      val value = extractField(configElem, metadata)
      if (value.isInstanceOf[Option[_]] && !value.asInstanceOf[Option[_]].isDefined) {
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
      }
      else {
        val elemType: String = configElem.getString("type")
        val elemVal: JsValueWrapper = elemType match {
          case "boolean" => {
            val valid_value = configElem.getString("valid_value")
            (value.toString == valid_value)
          }
          case "string" => {
            if (value.isInstanceOf[DateTime]) {
              customFormat(value.asInstanceOf[DateTime], configElem.getConfig("format"))
            }
            else if (value.isInstanceOf[String]) {
              value.toString
            }
            else {
              JsNull
            }
          }
        }
        getNewField(configElem, elemVal)
      }
    }
  }

  def updateSkipNext(oldSkipNext: Int, configData: Config, fileData: ListBuffer[Seq[String]], row: Option[Int] = None): Int = {
    if (!configData.hasPath("skip_next")) {
      return oldSkipNext
    }

    if(!configData.hasPath("skip_condition")) {
      return configData.getInt("skip_next")
    }

    val column = configData.getInt("column")
    val value = fileData(row.getOrElse(configData.getInt("row")))(column)
    if (value.toUpperCase == configData.getString("skip_condition")) {
      configData.getInt("skip_next")
    }
    else {
      oldSkipNext
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
