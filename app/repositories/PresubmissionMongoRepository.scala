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
import models._
import org.mongodb.scala.bson.{BsonDocument, BsonInt64, BsonString, ObjectId}
import org.mongodb.scala.model.Sorts._
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model._
import org.mongodb.scala.result.UpdateResult
import play.api.Logging
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json.{Format, JsObject, Json}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoFormats

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class PresubmissionMongoRepository @Inject()(applicationConfig: ApplicationConfig, mc: MongoComponent)
                                            (implicit ec: ExecutionContext)
  extends PlayMongoRepository[JsObject](
    mongoComponent = mc,
    collectionName = applicationConfig.presubmissionCollection,
    domainFormat = implicitly[Format[JsObject]],
    indexes = Seq(
      IndexModel(ascending("schemeInfo.schemeRef"), IndexOptions().name("schemeRef")),
      IndexModel(ascending("schemeInfo.timestamp"), IndexOptions().name("timestamp")),
      IndexModel(ascending("createdAt"), indexOptions = IndexOptions().name("timeToLive")
          .expireAfter(applicationConfig.presubmissionCollectionTTL, TimeUnit.DAYS)
      )
    ),
    replaceIndexes = applicationConfig.presubmissionCollectionIndexReplace
  ) with Logging {

  private val objectIdKey: String = "_id"

  def buildSelector(schemeInfo: SchemeInfo): BsonDocument = BsonDocument(
    "schemeInfo.schemeRef" -> BsonString(schemeInfo.schemeRef),
    "schemeInfo.timestamp" -> BsonInt64(schemeInfo.timestamp.getMillis)
  )

  def storeJson(presubmissionData: SchemeData): Future[Boolean] = {
    val document: JsObject =
      Json.toJsObject(presubmissionData) ++
        Json.obj("createdAt" -> Json.obj("$date" -> Instant.now.toEpochMilli))

    collection.insertOne(document).toFuture().map { res =>
      res.wasAcknowledged()
    }
  }

  def storeJsonV2(schemeInfo: String, presubmissionData: SchemeData): Future[Boolean] = {
    val document: JsObject =
      Json.toJsObject(presubmissionData) ++
        Json.obj("createdAt" -> Json.obj("$date" -> Instant.now.toEpochMilli))

      collection.insertOne(document).toFuture().map { res =>
      res.wasAcknowledged()
    }.recover {
      case e: Throwable =>
        logger.error(s"Failed storing presubmission data. Error: ${e.getMessage} for schemeInfo: $schemeInfo")
        throw e
    }
  }

  def getJson(schemeInfo: SchemeInfo): Future[Seq[JsObject]] = {
    logger.info(s"Searching for pre-submission data for " +
      s"scheme reference: ${schemeInfo.schemeRef}, " +
      s"timestamp: ${schemeInfo.timestamp}, " +
      s"taxYear: ${schemeInfo.taxYear}," +
      s"schemeType: ${schemeInfo.schemeType}")

    collection.find(
      buildSelector(schemeInfo)
    ).batchSize(Int.MaxValue).toFuture()
  }

  def count(schemeInfo: SchemeInfo): Future[Long] = {
    collection.countDocuments(
        buildSelector(schemeInfo)
    ).toFuture()
  }

  def removeJson(schemeInfo: SchemeInfo): Future[Boolean] = {
    val selector = buildSelector(schemeInfo)
    collection.deleteOne(selector).toFuture().map { res =>
      res.wasAcknowledged()
    }
  }

  def getDocumentIdsWithoutCreatedAtField(updateLimit: Int): Future[Seq[ObjectId]] = {
    val selector = Filters.exists("createdAt", exists = false)
    val projection = Projections.include(objectIdKey)

    collection
      .find(selector)
      .projection(projection)
      .limit(updateLimit)
      .map(json => (json \ objectIdKey).as[ObjectId](MongoFormats.objectIdFormat))
      .toFuture()
  }

  def addCreatedAtField(documentIds: Seq[ObjectId]): Future[Long] = {
    val selector = Filters.in(objectIdKey, documentIds: _*)
    val update = Seq(set("createdAt", BsonDocument("$toDate" -> "$schemeInfo.timestamp")))
    val options = UpdateOptions().upsert(false)

    val result = collection.updateMany(selector, update, options).toFuture()

    result.map { updateResult: UpdateResult =>
      Try(updateResult.getModifiedCount).getOrElse(0L)
    }.recover {
      case e: Exception => throw new RuntimeException(s"Failed to add createdAt field: ${e.getMessage}")
    }
  }
}
