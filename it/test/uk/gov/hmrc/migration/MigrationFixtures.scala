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

package uk.gov.hmrc.migration

import models.ErsSummary
import org.bson.{BsonDocument, BsonInt64}
import play.api.libs.json._

object MigrationFixtures {

  /**
   * Converts an ErsSummary to JsObject with confirmationDateTime stored as Long (old format)
   * This simulates documents that were created before the migration
   */
  def ersSummaryWithLongDateTime(ersSummary: ErsSummary): JsObject = {
    val normalJson = Json.toJsObject(ersSummary)
    val epochMillis = ersSummary.confirmationDateTime.toEpochMilli

    // Replace the confirmationDateTime field with a plain Long value (not a date object)
    normalJson + ("confirmationDateTime" -> JsNumber(epochMillis))
  }

  /**
   * Converts ErsSummary to BSON with confirmationDateTime as NumberLong
   * This ensures the field is truly stored as a number in MongoDB, not as a date type
   */
  def ersSummaryToBsonWithLong(ersSummary: ErsSummary): BsonDocument = {
    val jsObject = Json.toJsObject(ersSummary)
    val epochMillis = ersSummary.confirmationDateTime.toEpochMilli

    // Create JSON string and parse to BSON, then manually set confirmationDateTime as NumberLong
    val jsonString = jsObject.toString()
    val bsonDoc = BsonDocument.parse(jsonString)

    // Remove existing confirmationDateTime and add as pure NumberLong (not date type)
    bsonDoc.remove("confirmationDateTime")
    bsonDoc.put("confirmationDateTime", new BsonInt64(epochMillis))

    bsonDoc
  }

  /**
   * Converts an ErsSummary to JsObject with confirmationDateTime in proper MongoDB date format
   * This simulates documents that are already migrated
   */
  def ersSummaryWithProperDateTime(ersSummary: ErsSummary): JsObject = {
    // Just use the normal serialization which will create proper format
    Json.toJsObject(ersSummary)
  }

  /**
   * Creates a list of ErsSummary documents with old Long format
   */
  def generateErsSummariesWithLongFormat(ersSummaries: Seq[ErsSummary]): Seq[JsObject] = {
    ersSummaries.map(ersSummaryWithLongDateTime)
  }

  /**
   * Creates a list of BSON documents with confirmationDateTime as NumberLong
   */
  def generateBsonWithLongFormat(ersSummaries: Seq[ErsSummary]): Seq[BsonDocument] = {
    ersSummaries.map(ersSummaryToBsonWithLong)
  }

  /**
   * Creates a list of ErsSummary documents with proper date format
   */
  def generateErsSummariesWithProperFormat(ersSummaries: Seq[ErsSummary]): Seq[JsObject] = {
    ersSummaries.map(ersSummaryWithProperDateTime)
  }
}

