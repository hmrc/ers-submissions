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
import org.joda.time.DateTime
import org.mongodb.scala.bson.{BsonDocument, BsonInt64, BsonString}
import org.mongodb.scala.model.FindOneAndUpdateOptions
import play.api.Logging
import repositories.helpers.BaseVerificationRepository
import repositories.helpers.BsonDocumentHelper.BsonOps
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MetadataMongoRepository @Inject()(val applicationConfig: ApplicationConfig, mc: MongoComponent)
                                       (implicit ec: ExecutionContext)
  extends PlayMongoRepository[ErsSummary](
    mongoComponent = mc,
    collectionName = applicationConfig.metadataCollection,
    domainFormat = ErsSummary.format,
    indexes = Seq.empty
  ) with BaseVerificationRepository with Logging {

  def buildSelector(schemeInfo: SchemeInfo): BsonDocument = BsonDocument(
    "metaData.schemeInfo.schemeRef" -> BsonString(schemeInfo.schemeRef),
    "metaData.schemeInfo.timestamp" -> BsonInt64(schemeInfo.timestamp.getMillis)
  )

  def storeErsSummary(ersSummary: ErsSummary): Future[Boolean] = {
    collection.insertOne(ersSummary).toFuture.map { res =>
      res.wasAcknowledged()
    }
  }

  def getJson(schemeInfo: SchemeInfo): Future[Seq[ErsSummary]] = {
    collection.find(
      buildSelector(schemeInfo)
    ).batchSize(Int.MaxValue).toFuture()
  }

  def updateStatus(schemeInfo: SchemeInfo, status: String): Future[Boolean] = {
    val selector = buildSelector(schemeInfo)
    val update = BsonDocument("$set" -> BsonDocument("transferStatus" ->  status))

    collection.updateOne(selector, update).toFuture.map { res =>
      res.wasAcknowledged()
    }
  }

  def getNumberOfFailedJobs(statusList: List[String]): Future[Long] = {
    val baseSelector: BsonDocument = BsonDocument(
      "transferStatus" -> BsonDocument(
        "$in" -> statusList.map(Some(_))
      )
    )
    collection.countDocuments(
      filter = baseSelector
    ).toFuture()
  }

  def findAndUpdateByStatus(statusList: List[String],
                            isResubmitBeforeDate: Boolean = true,
                            schemeRefList: Option[List[String]],
                            schemeType: Option[String]): Future[Option[ErsSummary]] = {

    val baseSelector: BsonDocument = BsonDocument(
      "transferStatus" -> BsonDocument(
        "$in" -> statusList.map(Some(_))
      )
    )

    val schemeRefSelector: BsonDocument = BsonDocument(
      schemeRefList.map(schemeList => "metaData.schemeInfo.schemeRef" -> BsonDocument("$in" -> schemeList))
    )

    val schemeSelector: BsonDocument = BsonDocument(
      schemeType.map(scheme => "metaData.schemeInfo.schemeType" -> BsonString(scheme))
    )

    val dateRangeSelector: BsonDocument = BsonDocument(
      Some("metaData.schemeInfo.timestamp" -> BsonDocument(
        "$gte" -> DateTime.parse(applicationConfig.scheduleStartDate).getMillis,
        "$lte" -> DateTime.parse(applicationConfig.scheduleEndDate).getMillis
      )).filter(_ => isResubmitBeforeDate)
    )

    val modifier: BsonDocument = BsonDocument(
      "$set" -> BsonDocument(
        "transferStatus" -> Statuses.Process.toString
      )
    )

    val selector = Seq(baseSelector, schemeRefSelector, schemeSelector, dateRangeSelector).foldLeft(BsonDocument())(_ +:+ _)

    collection.findOneAndUpdate(
      filter = selector,
      update = Seq(modifier),
      options = FindOneAndUpdateOptions().sort(BsonDocument("metaData.schemeInfo.timestamp" -> 1))
    ).toFutureOption()
  }

}
