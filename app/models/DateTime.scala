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

import java.time.Instant
import play.api.libs.json.{Format, Json, Reads, Writes, __}

object DateTime {
  private val dateTimeRead: Reads[Instant] =
    __.read[Long].map(Instant.ofEpochMilli)

  private val dateTimeWrite: Writes[Instant] = (dateTime: Instant) => Json.toJson(dateTime.toEpochMilli)

  implicit val dateTimeFormats: Format[Instant] = Format(dateTimeRead, dateTimeWrite)
}
