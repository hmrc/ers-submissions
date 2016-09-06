/*
 * Copyright 2016 HM Revenue & Customs
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

package models

import org.joda.time.DateTime
import play.api.libs.json.{JsObject, Json}
import scala.collection.mutable.ListBuffer

case class SchemeInfo (
                        schemeRef: String,
                        timestamp: DateTime = DateTime.now,
                        schemeId: String,
                        taxYear: String,
                        schemeName: String,
                        schemeType: String
                        )

object SchemeInfo {
  implicit val format = Json.format[SchemeInfo]
}

case class SchemeData(
                       schemeInfo: SchemeInfo,
                       sheetName: String,
                       numberOfParts: Option[Int],
                       data: Option[ListBuffer[Seq[String]]]
                       )
object SchemeData {
  implicit val format = Json.format[SchemeData]
}

case class SchemeRefContainer(schemeRef: String)
object SchemeRefContainer {
  implicit val format = Json.format[SchemeRefContainer]
}
case class SchemeInfoContainer(schemeInfo: SchemeRefContainer)
object SchemeInfoContainer {
  implicit val format = Json.format[SchemeInfoContainer]
}

case class FullSchemeInfoContainer(schemeInfo: SchemeInfo)
object FullSchemeInfoContainer {
  implicit val format = Json.format[FullSchemeInfoContainer]
}

case class MetaDataContainer(metaData: SchemeInfoContainer)
object MetaDataContainer {
  implicit val format = Json.format[MetaDataContainer]
}

case class FullMetaDataContainer(metaData: FullSchemeInfoContainer)
object FullMetaDataContainer {
  implicit val format = Json.format[FullMetaDataContainer]
}

case class PostSubmissionData(schemeInfo: SchemeInfo, status: String, data: JsObject)
object PostSubmissionData {
  implicit val format = Json.format[PostSubmissionData]
}

case class ErsJsonStoreInfo(
                             schemeInfo: SchemeInfo,
                             fileId: Option[String],
                             fileName: Option[String],
                             fileLength: Option[Long],
                             uploadDate: Option[Long],
                             status: String
                             )
object ErsJsonStoreInfo {
  implicit val format = Json.format[ErsJsonStoreInfo]
}
