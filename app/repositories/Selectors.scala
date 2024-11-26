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

package repositories

import org.mongodb.scala.bson.{BsonDocument, BsonString}

import java.time.{LocalDate, ZoneId}
import java.time.format.DateTimeFormatter
import services.resubmission.ProcessFailedSubmissionsConfig
import repositories.helpers.BsonDocumentHelper.BsonOps

case class Selectors(processFailedSubmissionsConfig: ProcessFailedSubmissionsConfig) {
  val baseSelector: BsonDocument = BsonDocument(
    "transferStatus" -> BsonDocument(
      "$in" -> processFailedSubmissionsConfig.searchStatusList.map(Some(_))
    )
  )

  val preSubmissionSchemeRefSelector: BsonDocument = BsonDocument(
    processFailedSubmissionsConfig.schemeRefList.map(schemeList => "schemeInfo.schemeRef" -> BsonDocument("$in" -> schemeList))
  )

  val metadataSchemeRefSelector: BsonDocument = BsonDocument(
    processFailedSubmissionsConfig.schemeRefList.map(schemeList => "metaData.schemeInfo.schemeRef" -> BsonDocument("$in" -> schemeList))
  )

  val schemeSelector: BsonDocument = BsonDocument(
    processFailedSubmissionsConfig.resubmitScheme.map(scheme => "metaData.schemeInfo.schemeType" -> BsonString(scheme))
  )

  val dateRangeSelector: BsonDocument = BsonDocument(processFailedSubmissionsConfig.dateTimeFilter.map(date =>
    "metaData.schemeInfo.timestamp" -> BsonDocument("$gte" -> LocalDate.parse(date, DateTimeFormatter.ofPattern("dd/MM/yyyy")).atStartOfDay(ZoneId.of("UTC")).toInstant.toEpochMilli))
  )

  val preSubDateRangeSelector: BsonDocument = BsonDocument(processFailedSubmissionsConfig.dateTimeFilter.map(date =>
    "schemeInfo.timestamp" -> BsonDocument("$gte" -> LocalDate.parse(date, DateTimeFormatter.ofPattern("dd/MM/yyyy")).atStartOfDay(ZoneId.of("UTC")).toInstant.toEpochMilli))
  )

  val allMetadataSelectors: BsonDocument = Seq(
    baseSelector,
    metadataSchemeRefSelector,
    schemeSelector,
    dateRangeSelector
  ).foldLeft(BsonDocument())(_ +:+ _)
}
