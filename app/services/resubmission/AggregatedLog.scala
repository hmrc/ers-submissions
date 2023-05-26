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

package services.resubmission

import play.api.libs.json.{Format, Json}

case class SubmissionInfo(timestamp: Long, schemeRef: String){
  val logLine = s"($schemeRef, $timestamp)"
}
case class Id(schemeType: String, transferStatus: String)
case class AggregatedLog(_id: Id, count: Int, submissionInfo: Seq[SubmissionInfo]){
  val logLine: String = s"schemaType: ${_id.schemeType}, " +
    s"transferStatus: ${_id.transferStatus}, " +
    s"count: $count, " +
    s"submissionInfo: ${submissionInfo.map(_.logLine).mkString(",")}"
}

object AggregatedLog {
  implicit val submissionInfoFormat: Format[SubmissionInfo] = Json.format[SubmissionInfo]
  implicit val idFormat: Format[Id] = Json.format[Id]
  implicit val dataFormat: Format[AggregatedLog] = Json.format[AggregatedLog]
}
