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

import models.PreSubWithoutMetadata
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.{BsonArray, BsonDocument, BsonString}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import uk.gov.hmrc.mongo.MongoComponent

import javax.inject.{Inject, Singleton}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.DurationInt

@Singleton
class PreSubWithoutMetadataView @Inject() (mongoComponent: MongoComponent)(implicit ec: ExecutionContext) {

  private val preSubWithoutMetadataViewName: String = "preSubWithoutMetadataView"

  lazy val view: MongoCollection[PreSubWithoutMetadata] =
    Await.result(awaitable = initView, atMost = 30.seconds) // TODO: Modify the duration to make it a parameter

  private def createView(viewName: String, viewOn: String): Future[_] =
    mongoComponent.database.createView(viewName, viewOn, pipeline).toFuture()

  private def dropView(viewName: String = preSubWithoutMetadataViewName): Future[Unit] =
    getView(viewName)
      .drop()
      .toFuture()

  private def getView(viewName: String): MongoCollection[PreSubWithoutMetadata] =
    mongoComponent.database
      .getCollection[PreSubWithoutMetadata](viewName)

  def initView: Future[MongoCollection[PreSubWithoutMetadata]] =
    dropView(preSubWithoutMetadataViewName)
      .map(_ => createView(preSubWithoutMetadataViewName, "ers-presubmission"))
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

  private val projectFields: Bson = Aggregates.project(
    Projections.fields(
      Projections.computed("schemeRef", "$schemeInfo.schemeRef"),
      Projections.computed("taxYear", "$schemeInfo.taxYear"),
      Projections.computed("timestamp", "$schemeInfo.timestamp"),
      Projections.excludeId()
    )
  )


  protected val pipeline: Seq[Bson] =
    Seq(
      matchMetadata,
      filterForNoMetadata,
      projectFields
    )

}

