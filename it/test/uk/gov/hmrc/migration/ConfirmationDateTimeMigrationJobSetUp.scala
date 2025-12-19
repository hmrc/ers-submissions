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

import org.bson.{BsonDocument, BsonType}
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters
import play.api.Application
import play.api.libs.json.JsObject
import repositories.MetadataMongoRepository
import scheduler.ConfirmationDateTimeMigrationJobImpl

import scala.concurrent.{ExecutionContext, Future}
import play.api.test.Helpers._

case class ConfirmationDateTimeMigrationJobSetUp(app: Application) {

  val metadataMongoRepository: MetadataMongoRepository = app.injector.instanceOf[MetadataMongoRepository]
  val collection: MongoCollection[JsObject] = metadataMongoRepository.collection

  // Get the raw BSON collection for direct BSON document insertion
  val bsonCollection: MongoCollection[BsonDocument] = metadataMongoRepository
    .collection
    .withDocumentClass[BsonDocument]()

  await(collection.drop().toFuture())

  val getJob: ConfirmationDateTimeMigrationJobImpl = app.injector.instanceOf[ConfirmationDateTimeMigrationJobImpl]

  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  def countMetadataRecordsWithSelector(filter: Bson): Long = await(
    collection.countDocuments(filter)
      .toFuture()
  )

  def storeMultipleErsSummary(ersSummaries: Seq[JsObject])(implicit ec: ExecutionContext): Future[Boolean] = {
    collection.insertMany(ersSummaries).toFuture().map { res =>
      res.wasAcknowledged()
    }
  }

  def storeMultipleBsonDocuments(bsonDocuments: Seq[BsonDocument])(implicit ec: ExecutionContext): Future[Boolean] = {
    bsonCollection.insertMany(bsonDocuments).toFuture().map { res =>
      res.wasAcknowledged()
    }
  }

  // Filter to find documents where confirmationDateTime is stored as Long (old format)
  val documentsNeedingMigrationSelector: Bson = Filters.and(
    Filters.exists("confirmationDateTime"),
    Filters.not(Filters.`type`("confirmationDateTime", BsonType.DATE_TIME))
  )

  // Filter to find documents where confirmationDateTime is stored as proper date (new format)
  val migratedDocumentsSelector: Bson = Filters.and(
    Filters.exists("confirmationDateTime"),
    Filters.`type`("confirmationDateTime", BsonType.DATE_TIME)
  )
}

