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
import org.bson.codecs.configuration.CodecRegistries
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.codecs.Macros
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.bson.{BsonArray, BsonDocument, BsonString}
import org.mongodb.scala.model._
import uk.gov.hmrc.mongo.MongoComponent

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, ZoneId}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PreSubWithoutMetadataView @Inject() (mongoComponent: MongoComponent,
                                           val applicationConfig: ApplicationConfig)(implicit ec: ExecutionContext) {

  private val preSubWithoutMetadataViewName: String = "ers-presubmission-without-metadata"

  private val codecRegistry = CodecRegistries.
    fromRegistries(CodecRegistries.fromProviders(Macros.createCodecProvider[PreSubWithoutMetadata]()), DEFAULT_CODEC_REGISTRY)

  private def createView(viewName: String, viewOn: String): Future[Unit] =
    mongoComponent.database.createView(viewName, viewOn, pipeline).toFuture()

  private def dropView(viewName: String): Future[Unit] =
    getView(viewName)
      .drop()
      .toFuture()

  def getView(viewName: String): MongoCollection[PreSubWithoutMetadata] =
    mongoComponent.database
      .getCollection[PreSubWithoutMetadata](viewName)
      .withCodecRegistry(codecRegistry)

  def initView: Future[MongoCollection[PreSubWithoutMetadata]] =
    dropView(preSubWithoutMetadataViewName)
      .map(_ => createView(preSubWithoutMetadataViewName, applicationConfig.presubmissionCollection))
      .map(_ => getView(preSubWithoutMetadataViewName))

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
      dateFilter(applicationConfig.dateTimeFilterForView),
      projectFields
    )

}

