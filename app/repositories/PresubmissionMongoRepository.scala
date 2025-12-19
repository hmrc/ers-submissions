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

import cats.data.EitherT
import cats.syntax.all._
import common.ERSEnvelope.ERSEnvelope
import config.ApplicationConfig
import models._
import org.bson.BsonType
import org.mongodb.scala.bson.{BsonDocument, BsonInt64, BsonString}
import org.mongodb.scala.model.Sorts._
import org.mongodb.scala.model._
import org.mongodb.scala.result.DeleteResult
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json.{Format, JsObject, Json}
import repositories.helpers.RepositoryHelper
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class PresubmissionMongoRepository @Inject()(applicationConfig: ApplicationConfig, mc: MongoComponent)
                                            (implicit ec: ExecutionContext)
  extends PlayMongoRepository[JsObject](
    mongoComponent = mc,
    collectionName = applicationConfig.presubmissionCollection,
    domainFormat = implicitly[Format[JsObject]],
    indexes = scala.Seq(
      IndexModel(ascending("schemeInfo.schemeRef"), IndexOptions().name("schemeRef")),
      IndexModel(ascending("createdAt"), indexOptions = IndexOptions().name("schemeInfoTimeToLive")
          .expireAfter(applicationConfig.presubmissionCollectionTTL, TimeUnit.DAYS)
      )
    ),
    replaceIndexes = applicationConfig.presubmissionCollectionIndexReplace
  ) with RepositoryHelper {

  private val className = getClass.getSimpleName

  def buildSelector(schemeInfo: SchemeInfo): BsonDocument = BsonDocument(
    "schemeInfo.schemeRef" -> BsonString(schemeInfo.schemeRef),
    "schemeInfo.timestamp" -> BsonInt64(schemeInfo.timestamp.toEpochMilli)
  )

  def storeJson(presubmissionData: SchemeData, sessionId: String): ERSEnvelope[Boolean] = EitherT {
    collection
      .insertOne(createDocumentToInsert(presubmissionData))
      .toFuture()
      .map(_.wasAcknowledged())
      .map(_.asRight)
      .recover {
        mongoRecover(
          repository = className,
          method = "storeJson",
          sessionId = sessionId,
          message = "operation failed due to exception from Mongo",
          optSchemaRefs = Some(scala.Seq(presubmissionData.schemeInfo.schemeRef))
        )
      }
  }

  def getJson(schemeInfo: SchemeInfo, sessionId: String): ERSEnvelope[scala.Seq[JsObject]] = EitherT {
    logInfo(s"[PresubmissionMongoRepository][getJson][selector]: ${buildSelector(schemeInfo).toJson}")
    collection
      .find(buildSelector(schemeInfo))
      .batchSize(Int.MaxValue)
      .toFuture()
      .map(_.asRight)
      .recover {
        mongoRecover(
          repository = className,
          method = "getJson",
          sessionId = sessionId,
          message = "operation failed due to exception from Mongo",
          optSchemaRefs = Some(scala.Seq(schemeInfo.schemeRef))
        )
      }
  }

  def count(schemeInfo: SchemeInfo, sessionId: String): ERSEnvelope[Long] = EitherT {
    collection
      .countDocuments(buildSelector(schemeInfo))
      .toFuture()
      .map(_.asRight)
      .recover {
        mongoRecover(
          repository = className,
          method = "count",
          sessionId = sessionId,
          message = "operation failed due to exception from Mongo",
          optSchemaRefs = Some(scala.Seq(schemeInfo.schemeRef))
        )
      }
  }

  def removeJson(schemeInfo: SchemeInfo, sessionId: String): ERSEnvelope[DeleteResult] = EitherT {
    collection.deleteMany(buildSelector(schemeInfo))
      .toFuture()
      .map(_.asRight)
      .recover {
        mongoRecover(
          repository = className,
          method = "removeJson",
          sessionId = sessionId,
          message = "operation failed due to exception from Mongo",
          optSchemaRefs = Some(scala.Seq(schemeInfo.schemeRef))
        )
      }
  }

  private def createDocumentToInsert(schemeData: SchemeData): JsObject =
    Json.toJsObject(schemeData) ++
      Json.obj("createdAt" -> MongoJavatimeFormats.instantWrites.writes(schemeData.schemeInfo.timestamp))

  def  getStatusForSelectedSchemes(sessionId: String, selectors: Selectors): ERSEnvelope[Seq[JsObject]] = EitherT {
    collection
      .find(filter = selectors.preSubmissionSchemeRefSelector)
      .toFuture()
      .map(_.asRight)
      .recover {
        mongoRecover(
          repository = className,
          method = "getStatusForSelectedSchemes",
          sessionId = sessionId,
          message = "operation failed due to exception from Mongo",
          optSchemaRefs = None
        )
      }
  }

  def migrateCreatedAtField(sessionId: String = ""): ERSEnvelope[Int] = EitherT {
    val batchSize = applicationConfig.createdAtMigrationBatchSize

    // Query documents where createdAt either doesn't exist OR is not stored as proper date type
    val filter = Filters.or(
      Filters.exists("createdAt", exists = false),
      Filters.not(Filters.`type`("createdAt", BsonType.DATE_TIME))
    )

    collection
      .find(filter)
      .limit(batchSize)
      .toFuture()
      .flatMap { documents =>
        val updateFutures: scala.Seq[scala.concurrent.Future[org.mongodb.scala.result.UpdateResult]] = documents.map { doc =>
          // Parse the document to get SchemeData
          val schemeData = doc.as[SchemeData]
          val selector = buildSelector(schemeData.schemeInfo)

          // Create updated document with proper createdAt field
          val updatedDoc = createDocumentToInsert(schemeData)

          collection.replaceOne(selector, updatedDoc).toFuture()
        }

        scala.concurrent.Future.sequence(updateFutures).map(_.count(_.wasAcknowledged()))
      }
      .map(_.asRight)
      .recover {
        mongoRecover(
          repository = className,
          method = "migrateCreatedAtField",
          sessionId = sessionId,
          message = "operation failed due to exception from Mongo",
          optSchemaRefs = None
        )
      }
  }

}
