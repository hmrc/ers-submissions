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

import config.ApplicationConfig
import models.PreSubWithoutMetadata
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.bson.{BsonArray, BsonDocument, BsonString}
import org.mongodb.scala.model._
import play.api.libs.json.{JsError, JsObject, JsPath, JsSuccess, JsonValidationError, Reads}

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, ZoneId}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PreSubWithoutMetadataQuery @Inject()(presubmissionRepository: PresubmissionMongoRepository,
                                           val applicationConfig: ApplicationConfig)(implicit ec: ExecutionContext) {

  private val letVariables: Seq[Variable[BsonString]] = Seq(
    new Variable("preSubSchemeRef", BsonString("$schemeInfo.schemeRef")),
    new Variable("preSubTaxYear", BsonString("$schemeInfo.taxYear"))
  )

  private val lookupPipeline: Seq[BsonDocument] = Seq(
    BsonDocument(
      "$match" -> BsonDocument(
        "$expr" -> BsonDocument(
          "$and" -> BsonArray(
            BsonDocument("$eq" -> BsonArray("$metaData.schemeInfo.schemeRef", "$$preSubSchemeRef")),
            BsonDocument("$eq" -> BsonArray("$metaData.schemeInfo.taxYear", "$$preSubTaxYear"))
          )
        )
      )
    )
  )

  private val matchMetadata = Aggregates.lookup(
    from = "ers-metadata",
    let = letVariables,
    pipeline = lookupPipeline,
    as = "linkedMetaDataRecords"
  )

  private val filterForNoMetadata: BsonDocument = BsonDocument(
    "$match" -> BsonDocument(
      "$expr" -> BsonDocument(
        "$eq" -> BsonArray(BsonDocument("$size" -> "$linkedMetaDataRecords"), 0)
      )
    )
  )

  private def dateFilter(startDate: String): BsonDocument = {
    BsonDocument(
      "$match" -> BsonDocument(
        "schemeInfo.timestamp" -> BsonDocument(
          "$gte" -> convertToEpoch(startDate),
          "$lte" -> Instant.now().toEpochMilli
        )
      )
    )
  }

 private def convertToEpoch(dateStr: String): Long = {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val localDate = LocalDate.parse(dateStr, formatter)
    localDate.atStartOfDay(ZoneId.systemDefault()).toInstant.toEpochMilli
  }

  private val projectFields: Bson = Aggregates.project(
    Projections.fields(
      Projections.computed("schemeRef", "$schemeInfo.schemeRef"),
      Projections.computed("taxYear", "$schemeInfo.taxYear"),
      Projections.computed("timestamp", "$schemeInfo.timestamp"),
      Projections.excludeId()
    )
  )
  
  val pipeline: Seq[Bson] =
    Seq(
      matchMetadata,
      filterForNoMetadata,
      dateFilter(applicationConfig.dateTimeFilterForQuery),
      projectFields
    )

  def validateJson[T](record: JsObject)(implicit reads: Reads[T]): Either[String, T] =
    record.validate[T] match {
      case JsSuccess(obj, _) =>
        Right(obj)
      case JsError(errors: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]) =>
         Left(
           errors
            .map((e: (JsPath, collection.Seq[JsonValidationError])) => s"${e._1}: ${e._2.mkString(", ")}")
            .mkString(", ")
         )
    }

  def runQuery: Future[(List[String], List[PreSubWithoutMetadata])] =
    for {
      preSubWithoutMetadataAsJson: Seq[JsObject] <- presubmissionRepository
        .collection
        .aggregate(pipeline)
        .toFuture()
      preSubWithoutMetadata = preSubWithoutMetadataAsJson.map(validateJson[PreSubWithoutMetadata])
    } yield preSubWithoutMetadata
      .foldLeft((List.empty[String], List.empty[PreSubWithoutMetadata])) {
        case ((errs, records), Left(error)) => (error :: errs, records)
        case ((errs, records), Right(record)) => (errs, record :: records)
      }
}
