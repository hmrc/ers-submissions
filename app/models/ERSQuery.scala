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

import play.api.libs.json.{Json, OFormat}

case class ERSQuery(schemeType: Option[String],
                    startDate: Option[String],
                    endDate: Option[String],
                    transferStatus:Option[String],
                    schemeRefsList:List[String]
                   )
object ERSQuery {
  implicit val format: OFormat[ERSQuery] = Json.format[ERSQuery]
}

case class ERSMetaDataResults(bundleRef:String,
                              schemeRef:String,
                              transferStatus:String,
                              fileType:String,
                              timestamp:String,
                              taxYear:String )
object ERSMetaDataResults {
  implicit val format: OFormat[ERSMetaDataResults] = Json.format[ERSMetaDataResults]
}

case class ERSDataResults(schemeRef: String,taxYear:String,timestamp:String, sheetName:String)
object ERSDataResults {
  implicit val format: OFormat[ERSDataResults] = Json.format[ERSDataResults]
}
