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
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Source, Sink}
import org.apache.pekko.util.ByteString
import play.api.Logging
import play.api.libs.json._
import play.api.mvc.Request
import services.PresubmissionService
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.concurrent.{ExecutionContext, Future}
import scala.collection.concurrent.TrieMap
import scala.util.{Success, Try, Failure}

class ADRSubmission @Inject()(submissionCommon: SubmissionCommon,
                              presubmissionService: PresubmissionService,
                              configUtils: ConfigUtils)
                             (implicit ec: ExecutionContext, mat: Materializer) extends Logging {

  private val EmptyJson: JsObject = Json.obj()

  def generateSubmission(ersSummary: ErsSummary)(implicit request: Request[_], hc: HeaderCarrier): ERSEnvelope[JsObject] = {
    val schemeType: String = ersSummary.metaData.schemeInfo.schemeType.toUpperCase()
    logger.info(s"[ADRSubmission][generateSubmission] ${ersSummary.basicLogMessage} ${ersSummary.metaData.schemeInfo.basicLogMessage}")

    if (ersSummary.isNilReturn == IsNilReturn.False.toString) {
      createSubmissionJson(ersSummary, schemeType)
    }
    else {
      createRootJson(EmptyJson, ersSummary, schemeType)
    }
  }

  def generateStreamSubmission(ersSummary: ErsSummary)
                              (implicit request: Request[_], hc: HeaderCarrier): ERSEnvelope[Source[ByteString, NotUsed]] = {
    val schemeType: String = ersSummary.metaData.schemeInfo.schemeType.toUpperCase()
    val schemeInfo = ersSummary.metaData.schemeInfo
    for {
      configInfo <- fetchConfigInfo(schemeInfo, schemeType, ersSummary)
      baseRootJson <- createRootJson(EmptyJson, ersSummary, schemeType)
      dataStream <- createRowDataStream(ersSummary, schemeType)
    } yield {
      val (dataPath, booleanFlags, firstRowMetadata) = configInfo

      val completeRootJson = processRootWithMetadata(baseRootJson, booleanFlags, firstRowMetadata)

      val rootJsonString = Json.stringify(completeRootJson)
      val dataStreaminsertPoint = rootJsonString.lastIndexOf("}}")
      val jsonBeforeDataStream = rootJsonString.substring(0, dataStreaminsertPoint)
      val jsonAfterDataStream = rootJsonString.substring(dataStreaminsertPoint)
      val arrayWrapper = dataPath.headOption.getOrElse("grant")
      val dataArray = dataPath.drop(1).headOption.getOrElse("grants")
      val openingByteString = ByteString(jsonBeforeDataStream + s",\"$arrayWrapper\":{\"$dataArray\":[")
      val closingByteString = ByteString("]}" + jsonAfterDataStream)

      Source
        .single(openingByteString)
        .concat(dataStream)
        .concat(Source.single(closingByteString))
    }
  }

  private def createRowDataStream(ersSummary: ErsSummary, schemeType: String)
                                 (implicit request: Request[_], hc: HeaderCarrier): ERSEnvelope[Source[ByteString, NotUsed]] = {

    presubmissionService.getJsonStreaming(ersSummary.metaData.schemeInfo).map { source =>

      val configCache = TrieMap.empty[String, Config]

      source
        .map(_.as[SchemeData])
        .mapConcat { schemeData =>
          val rows = schemeData.data.getOrElse(ListBuffer.empty).toList
          rows.map(row => (row, schemeData.schemeInfo, schemeData.sheetName))
        }
        .mapAsync(parallelism = 2) { case (row, schemeInfo, sheetName) =>
          Future {
            processRowToByteString(row, schemeInfo, sheetName, configCache, schemeType, ersSummary)
          }
        }
        .intersperse(ByteString(","))
    }
  }

  private def processRowToByteString(row: Seq[String], schemeInfo: SchemeInfo, sheetName: String, configCache: TrieMap[String, Config], schemeType: String, ersSummary: ErsSummary
                                    )(implicit request: Request[_], hc: HeaderCarrier): ByteString = {

    val config = configCache.getOrElseUpdate(
      sheetName,
      configUtils.getConfigData(s"$schemeType/$sheetName", sheetName, ersSummary)
    )

    val fullJson = buildJson(config, ListBuffer(row), Some(0), Some(sheetName), Some(schemeInfo))
    val path = getDataLocationNames(config)

    val rowObject: JsObject = if (path.nonEmpty) {
      val eventArray = path.foldLeft(fullJson: JsValue) { (json, key) =>
        (json \ key).getOrElse(Json.obj())
      }
      eventArray.asOpt[Seq[JsObject]].flatMap(_.headOption).getOrElse(fullJson)
    } else {
      fullJson
    }

    ByteString(Json.stringify(rowObject))
  }

  private def processRootWithMetadata(rootJson: JsObject, booleanFlags: List[String], firstRowMetadata: Map[String, JsValue]): JsObject = {

    val booleanUpdates: Map[String, JsBoolean] = booleanFlags.map(_ -> JsBoolean(true)).toMap

    val currentSubmissionReturn: JsObject = (rootJson \ "submissionReturn")
      .asOpt[JsObject]
      .getOrElse(Json.obj())

    val processedSubmissionReturn = currentSubmissionReturn ++
      JsObject(booleanUpdates) ++
      JsObject(firstRowMetadata)

    rootJson + ("submissionReturn" -> processedSubmissionReturn)
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
        logger.info(s"Found data in pre-submission repository, mapped successfully. File data list size: ${schemeDataSeq.size}, ${ersSummary.metaData.schemeInfo.basicLogMessage}")
      } else {
        logger.warn(s"No data returned from pre-submission repository or data is incomplete: ${ersSummary.metaData.schemeInfo.basicLogMessage}")
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

  private def getDataLocationNames(config: Config): List[String] = {
    @annotation.tailrec
    def loop(conf: Config, acc: List[String]): List[String] = {
      val currentName = conf.getString("name")
      if (conf.hasPath("data_location")) {
        val nextConf = conf.getConfig("data_location")
        loop(nextConf, acc :+ currentName)
      } else {
        acc :+ currentName
      }
    }
    if (config.hasPath("data_location")) {
      val dataLoc = config.getConfig("data_location")
      loop(dataLoc, List.empty)
    } else {
      List.empty
    }
  }

  private def getConfigTrueBooleans(config: Config): List[String] = {
    import scala.jdk.CollectionConverters._
    if (config.hasPath("fields")) {
      val fields = config.getConfigList("fields").asScala.toList
      fields.collect {
        case elem if elem.hasPath("type") && elem.getString("type") == "boolean" &&
          elem.hasPath("value") && elem.getBoolean("value") =>
          elem.getString("name")
      }
    } else {
      List.empty
    }
  }

  private def extractEMIAdjustmentMetadata(config: Config, firstRow: Seq[String]): Map[String, JsValue] = {
    import scala.jdk.CollectionConverters._
    config.getConfigList("fields").asScala.flatMap { elem =>
      if (elem.hasPath("row") && elem.getInt("row") == 0 && elem.hasPath("column")) {
        val colIndex = elem.getInt("column")
        if (colIndex >= 0 && colIndex < firstRow.length && firstRow(colIndex).nonEmpty) {
          val cell = firstRow(colIndex)
          val fieldType = elem.getString("type")

          fieldType match {
            case "boolean" if elem.hasPath("valid_value") =>
              val valid = elem.getString("valid_value")
              val booleanVal = cell.trim.toUpperCase == valid.trim.toUpperCase
              Some(elem.getString("name") -> JsBoolean(booleanVal))
            case "string" =>
              Some(elem.getString("name") -> JsString(cell))
            case _ => None
          }
        } else None
      } else None
    }.toMap
  }

  private def fetchConfigInfo(schemeInfo: SchemeInfo, schemeType: String, ersSummary: ErsSummary)
                             (implicit hc: HeaderCarrier): ERSEnvelope[(List[String], List[String], Map[String, JsValue])] = {
    presubmissionService.getJsonStreaming(schemeInfo).flatMap { source =>
      val firstSchemeElement = source.map(_.as[SchemeData]).take(1).runWith(Sink.headOption)
      val configFuture = firstSchemeElement.map {
        case Some(schemeData) if schemeData.sheetName.nonEmpty =>
          Try(configUtils.getConfigData(s"$schemeType/${schemeData.sheetName}", schemeData.sheetName, ersSummary)) match {
            case Success(config) =>
              val dataPath = getDataLocationNames(config)
              val booleanFlags = getConfigTrueBooleans(config)

              val firstRowMetadata = if (schemeData.sheetName == "EMI40_Adjustments_V4") {
                schemeData.data.flatMap(_.headOption).map { firstRow =>
                  extractEMIAdjustmentMetadata(config, firstRow)
                }.getOrElse(Map.empty[String, JsValue])
              } else {
                Map.empty[String, JsValue]
              }

              (dataPath, booleanFlags, firstRowMetadata)

            case Failure(ex) =>
              logger.warn(s"Failed to load config for ${schemeData.sheetName}: ${ex.getMessage}")
              (List.empty[String], List.empty[String], Map.empty[String, JsValue])
          }
        case _ =>
          logger.warn(s"No scheme data found or empty sheet name for ${schemeInfo.schemeRef}")
          (List.empty[String], List.empty[String], Map.empty[String, JsValue])
      }
      ERSEnvelope(configFuture)
    }
  }
}