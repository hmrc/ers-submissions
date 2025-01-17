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

package messages

import play.api.libs.json.{Format, Json}

case class Id(schemeType: String, transferStatus: Option[String])
case class AggregatedLog(_id: Id, count: Int){
  val logLine: String = s"schemaType: ${_id.schemeType}, transferStatus: ${_id.transferStatus.getOrElse("EmptyTransferStatus")}, count: $count"
}

object AggregatedLog {
  implicit val idFormat: Format[Id] = Json.format[Id]
  implicit val dataFormat: Format[AggregatedLog] = Json.format[AggregatedLog]
}
