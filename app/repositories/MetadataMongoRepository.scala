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
import cats.implicits.catsSyntaxEitherId
import common.ERSEnvelope.ERSEnvelope
import config.ApplicationConfig
import models._
import org.mongodb.scala.FindObservable
import org.mongodb.scala.bson.{BsonDocument, BsonInt64, BsonString, Document, ObjectId}
import org.mongodb.scala.model.Accumulators._
import org.mongodb.scala.model.{Aggregates, Filters, Projections}
import org.mongodb.scala.result.UpdateResult
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{Format, JsObject, Json}
import repositories.helpers.RepositoryHelper
import services.resubmission.ProcessFailedSubmissionsConfig
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoFormats

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class MetadataMongoRepository @Inject()(val applicationConfig: ApplicationConfig, mc: MongoComponent)
                                       (implicit ec: ExecutionContext)
  extends PlayMongoRepository[JsObject](
    mongoComponent = mc,
    collectionName = applicationConfig.metadataCollection,
    domainFormat = implicitly[Format[JsObject]],
    indexes = scala.Seq.empty
  ) with RepositoryHelper {

  private val objectIdKey: String = "_id"
  private val className = getClass.getSimpleName

  def buildSelector(schemeInfo: SchemeInfo): BsonDocument = BsonDocument(
    "metaData.schemeInfo.schemeRef" -> BsonString(schemeInfo.schemeRef),
    "metaData.schemeInfo.timestamp" -> BsonInt64(schemeInfo.timestamp.toEpochMilli)
  )

  def storeErsSummary(ersSummary: ErsSummary, sessionId: String): ERSEnvelope[Boolean] = EitherT {
    collection
      .insertOne(Json.toJsObject(ersSummary))
      .toFuture()
      .map(_.wasAcknowledged())
      .map(_.asRight)
      .recover {
        mongoRecover(
          repository = className,
          method = "storeErsSummary",
          sessionId = sessionId,
          message = "operation failed due to exception from Mongo",
          optSchemaRefs = Some(scala.Seq(ersSummary.metaData.schemeInfo.schemeRef))
        )
      }
  }

  def updateStatus(schemeInfo: SchemeInfo, status: String, sessionId: String): ERSEnvelope[Boolean] = EitherT {
    val selector = buildSelector(schemeInfo)
    val update = BsonDocument("$set" -> BsonDocument("transferStatus" ->  status))

    collection
      .updateOne(selector, update)
      .toFuture()
      .map(_.wasAcknowledged())
      .map(_.asRight)
      .recover {
        mongoRecover(
          repository = className,
          method = "updateStatus",
          sessionId = sessionId,
          message = "operation failed due to exception from Mongo",
          optSchemaRefs = Some(scala.Seq(schemeInfo.schemeRef))
        )
      }
  }

  def createFailedJobSelector(processFailedSubmissionsConfig: ProcessFailedSubmissionsConfig): BsonDocument =
    Selectors(processFailedSubmissionsConfig).allMetadataSelectors

  def getFailedJobs(failedJobSelector: BsonDocument,
                    processFailedSubmissionsConfig: ProcessFailedSubmissionsConfig,
                    sessionId: String = ""): ERSEnvelope[scala.Seq[ObjectId]] = EitherT {
    val projection = Projections.include(objectIdKey)
    val ersSubmissionsWithObjectIds: FindObservable[JsObject] =
      collection
      .find(failedJobSelector)
      .projection(projection)
      .limit(processFailedSubmissionsConfig.resubmissionLimit)

    ersSubmissionsWithObjectIds
      .map(json => (json \ objectIdKey).as[ObjectId](MongoFormats.objectIdFormat))
      .toFuture()
      .map(_.asRight)
      .recover {
        mongoRecover(
          repository = className,
          method = "getFailedJobs",
          sessionId = sessionId,
          message = "operation failed due to exception from Mongo",
          optSchemaRefs = processFailedSubmissionsConfig.schemeRefList
        )
      }
  }

  def findAndUpdateByStatus(jobIdsToUpdate: scala.Seq[ObjectId], sessionId: String): ERSEnvelope[UpdateResult] = EitherT {
    val selector = Filters.in(objectIdKey, jobIdsToUpdate: _*)
    val modifier: BsonDocument = BsonDocument(
      "$set" -> BsonDocument(
        "transferStatus" -> Statuses.Process.toString
      )
    )

    collection
      .updateMany(filter = selector, update = scala.Seq(modifier))
      .toFuture()
      .map(_.asRight)
      .recover {
        mongoRecover(
          repository = className,
          method = "findAndUpdateByStatus",
          sessionId = sessionId,
          message = "operation failed due to exception from Mongo",
          optSchemaRefs = None
        )
      }
  }

  def findErsSummaries(jobIdsToUpdate: scala.Seq[ObjectId], sessionId: String): ERSEnvelope[scala.Seq[ErsSummary]] = EitherT {
    val selector = Filters.in(objectIdKey, jobIdsToUpdate: _*)

    collection
      .find(filter = selector)
      .map(_.as[ErsSummary])
      .toFuture()
      .map(_.asRight)
      .recover {
        mongoRecover(
          repository = className,
          method = "findErsSummaries",
          sessionId = sessionId,
          message = "operation failed due to exception from Mongo",
          optSchemaRefs = None
        )
      }
  }

  def getNumberOfFailedJobs(failedJobSelector: BsonDocument, sessionId: String): ERSEnvelope[Long] = EitherT {
    collection
      .countDocuments(filter = failedJobSelector)
      .toFuture()
      .map(_.asRight)
      .recover {
        mongoRecover(
          repository = className,
          method = "getNumberOfFailedJobs",
          sessionId = sessionId,
          message = "operation failed due to exception from Mongo",
          optSchemaRefs = None
        )
      }
  }

  def getAggregateCountOfSubmissions(sessionId: String): ERSEnvelope[Seq[JsObject]] = EitherT {
    collection
      .aggregate(
        pipeline = scala.Seq(
          Aggregates.group(
            Document("schemeType" -> "$metaData.schemeInfo.schemeType", "transferStatus" -> "$transferStatus"),
            sum("count", 1)
        )))
      .toFuture()
      .map(_.asRight)
      .recover {
        mongoRecover(
          repository = className,
          method = "getAggregateCountOfSubmissions",
          sessionId = sessionId,
          message = "operation failed due to exception from Mongo",
          optSchemaRefs = None
        )
      }
  }

  def getStatusForSelectedSchemes(sessionId: String, selectors: Selectors): ERSEnvelope[Seq[JsObject]] = EitherT {
    collection
      .find(filter = selectors.metadataSchemeRefSelector)
      .toFuture()
      .map(_.asRight)
      .recover {
        mongoRecover(
          repository = className,
          method = "getAggregateCountOfSubmissions",
          sessionId = sessionId,
          message = "operation failed due to exception from Mongo",
          optSchemaRefs = None
        )
      }
  }
}
