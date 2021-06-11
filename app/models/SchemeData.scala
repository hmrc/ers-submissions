/*
 * Copyright 2021 HM Revenue & Customs
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

import com.github.nscala_time.time.Imports.DateTimeZone
import org.joda.time.DateTime
import play.api.libs.json.{Format, Json, OFormat, Reads, Writes, __}


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

  private val dateTimeRead: Reads[DateTime] =
    (__).read[Long].map { dateTime =>
      new DateTime(dateTime, DateTimeZone.UTC)
    }

  private val dateTimeWrite: Writes[DateTime] = (dateTime: DateTime) => Json.toJson(dateTime.getMillis)

  implicit val dateTimeFormats: Format[DateTime] = Format(dateTimeRead, dateTimeWrite)
  implicit val format: OFormat[SchemeInfo] = Json.format[SchemeInfo]

}

case class SchemeData(
                       schemeInfo: SchemeInfo,
                       sheetName: String,
                       numberOfParts: Option[Int],
                       data: Option[ListBuffer[Seq[String]]]
                       )
object SchemeData {
  implicit val format: OFormat[SchemeData] = Json.format[SchemeData]
}

case class SubmissionsSchemeData(
                       schemeInfo: SchemeInfo,
                       sheetName: String,
                       data: UpscanCallback,
                       numberOfRows: Int
                       )

object SubmissionsSchemeData {
  implicit val format: OFormat[SubmissionsSchemeData] = Json.format[SubmissionsSchemeData]
}

case class SchemeRefContainer(schemeRef: String)
object SchemeRefContainer {
  implicit val format: OFormat[SchemeRefContainer] = Json.format[SchemeRefContainer]
}
case class SchemeInfoContainer(schemeInfo: SchemeRefContainer)
object SchemeInfoContainer {
  implicit val format: OFormat[SchemeInfoContainer] = Json.format[SchemeInfoContainer]
}

case class FullSchemeInfoContainer(schemeInfo: SchemeInfo)
object FullSchemeInfoContainer {
  implicit val format: OFormat[FullSchemeInfoContainer] = Json.format[FullSchemeInfoContainer]
}

case class MetaDataContainer(metaData: SchemeInfoContainer)
object MetaDataContainer {
  implicit val format: OFormat[MetaDataContainer] = Json.format[MetaDataContainer]
}

case class FullMetaDataContainer(metaData: FullSchemeInfoContainer)
object FullMetaDataContainer {
  implicit val format: OFormat[FullMetaDataContainer] = Json.format[FullMetaDataContainer]
}
