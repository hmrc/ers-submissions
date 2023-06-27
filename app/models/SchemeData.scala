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

package models

import org.joda.time.DateTime
import play.api.libs.json._

import scala.collection.mutable.ListBuffer

case class SchemeInfo (schemeRef: String,
                       timestamp: DateTime = DateTime.now,
                       schemeId: String,
                       taxYear: String,
                       schemeName: String,
                       schemeType: String) {

  val basicLogMessage: String = List(schemeRef, schemeType, taxYear, timestamp.getMillis.toString).mkString("[",",","]")
}

object SchemeInfo {
  import models.DateTime._

  implicit val format: OFormat[SchemeInfo] = Json.format[SchemeInfo]
}

case class SchemeData(schemeInfo: SchemeInfo,
                      sheetName: String,
                      numberOfParts: Option[Int],
                      data: Option[ListBuffer[scala.Seq[String]]])
object SchemeData {
  implicit val format: OFormat[SchemeData] = Json.format[SchemeData]
}

case class SubmissionsSchemeData(schemeInfo: SchemeInfo,
                                 sheetName: String,
                                 data: UpscanCallback,
                                 numberOfRows: Int)

object SubmissionsSchemeData {
  implicit val format: OFormat[SubmissionsSchemeData] = Json.format[SubmissionsSchemeData]
}
