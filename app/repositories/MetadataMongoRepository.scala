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

import com.fasterxml.jackson.databind.JsonNode
import config.ApplicationConfig
import models._
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.mongodb.scala.FindObservable
import org.mongodb.scala.bson.{BsonDocument, BsonInt64, BsonString, ObjectId}
import org.mongodb.scala.model.{Filters, Projections}
import org.mongodb.scala.result.UpdateResult
import play.api.Logging
import play.api.libs.json.{Format, JsObject}
import repositories.helpers.BaseVerificationRepository
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoFormats

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import models.{ErsSummary, Statuses}
import play.api.libs.json.Json
import repositories.helpers.BsonDocumentHelper.BsonOps
import services.resubmission.ProcessFailedSubmissionsConfig

@Singleton
class MetadataMongoRepository @Inject()(val applicationConfig: ApplicationConfig, mc: MongoComponent)
                                       (implicit ec: ExecutionContext)
  extends PlayMongoRepository[JsObject](
    mongoComponent = mc,
    collectionName = applicationConfig.metadataCollection,
    domainFormat = implicitly[Format[JsObject]],
    indexes = Seq.empty
  ) with BaseVerificationRepository with Logging {

  private val objectIdKey: String = "_id"

  def buildSelector(schemeInfo: SchemeInfo): BsonDocument = BsonDocument(
    "metaData.schemeInfo.schemeRef" -> BsonString(schemeInfo.schemeRef),
    "metaData.schemeInfo.timestamp" -> BsonInt64(schemeInfo.timestamp.getMillis)
  )

  def storeErsSummary(ersSummary: ErsSummary): Future[Boolean] = {
    collection.insertOne(Json.toJsObject(ersSummary)).toFuture.map { res =>
      res.wasAcknowledged()
    }
  }

  def updateStatus(schemeInfo: SchemeInfo, status: String): Future[Boolean] = {
    val selector = buildSelector(schemeInfo)
    val update = BsonDocument("$set" -> BsonDocument("transferStatus" ->  status))

    collection.updateOne(selector, update).toFuture.map { res =>
      res.wasAcknowledged()
    }
  }

  def createFailedJobSelector()(implicit processFailedSubmissionsConfig: ProcessFailedSubmissionsConfig): BsonDocument = {
    val baseSelector: BsonDocument = BsonDocument(
      "transferStatus" -> BsonDocument(
        "$in" -> processFailedSubmissionsConfig.searchStatusList.map(Some(_))
      )
    )

    val schemeRefSelector: BsonDocument = BsonDocument(
      processFailedSubmissionsConfig.schemeRefList.map(schemeList => "metaData.schemeInfo.schemeRef" -> BsonDocument("$in" -> schemeList))
    )

    val schemeSelector: BsonDocument = BsonDocument(
      processFailedSubmissionsConfig.resubmitScheme.map(scheme => "metaData.schemeInfo.schemeType" -> BsonString(scheme))
    )

    val formatter: DateTimeFormatter = DateTimeFormat.forPattern("dd/MM/yyyy");

    val dateRangeSelector: BsonDocument = BsonDocument(processFailedSubmissionsConfig.dateTimeFilter.map(date =>
      "metaData.schemeInfo.timestamp" -> BsonDocument("$gte" -> DateTime.parse(date, formatter).getMillis))
    )

    Seq(baseSelector, schemeRefSelector, schemeSelector, dateRangeSelector).foldLeft(BsonDocument())(_ +:+ _)
  }

  def getFailedJobs(failedJobSelector: BsonDocument)(implicit processFailedSubmissionsConfig: ProcessFailedSubmissionsConfig): Future[Seq[ObjectId]] = {

    val projection = Projections.include(objectIdKey)
    val ersSubmissionsWithObjectIds: FindObservable[JsObject] = collection
      .find(failedJobSelector)
      .projection(projection)
      .limit(processFailedSubmissionsConfig.resubmissionLimit)

    ersSubmissionsWithObjectIds
      .map(json => (json \ objectIdKey).as[ObjectId](MongoFormats.objectIdFormat))
      .toFuture()
  }

  def findAndUpdateByStatus(jobIdsToUpdate: Seq[ObjectId]): Future[UpdateResult] = {
    val selector = Filters.in(objectIdKey, jobIdsToUpdate: _*)
    val modifier: BsonDocument = BsonDocument(
      "$set" -> BsonDocument(
        "transferStatus" -> Statuses.Process.toString
      )
    )
    collection.updateMany(
      filter = selector,
      update = Seq(modifier)
    )
      .toFuture()
  }

  def findErsSummaries(jobIdsToUpdate: Seq[ObjectId]): Future[Seq[ErsSummary]] = {
    val selector = Filters.in(objectIdKey, jobIdsToUpdate: _*)
    collection.find(
      filter = selector
    )
      .map(_.as[ErsSummary])
      .toFuture()
  }

  def getNumberOfFailedJobs(failedJobSelector: BsonDocument): Future[Long] = {
    collection.countDocuments(
      filter = failedJobSelector
    ).toFuture()
  }

}
