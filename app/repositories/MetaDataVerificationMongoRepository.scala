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
import models.{ERSMetaDataResults, ERSQuery, ErsSummary}
import org.mongodb.scala.bson.{BsonDocument, Document}
import org.mongodb.scala.model.Accumulators.sum
import org.mongodb.scala.model.Aggregates
import play.api.libs.json.{Format, JsObject}
import repositories.helpers.BaseVerificationRepository
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MetaDataVerificationMongoRepository @Inject()(val applicationConfig: ApplicationConfig, mc: MongoComponent)(implicit ec: ExecutionContext)
  extends PlayMongoRepository[JsObject](
    mongoComponent = mc,
    collectionName = applicationConfig.metadataCollection,
    domainFormat = implicitly[Format[JsObject]],
    indexes = Seq.empty
  ) with BaseVerificationRepository {

  override val mongoKeyPrefix: String = "metadata."

  def mapResultToERSMetaDataResults(ersSummary: ErsSummary): ERSMetaDataResults =
    ERSMetaDataResults(
      ersSummary.bundleRef,
      ersSummary.metaData.schemeInfo.schemeRef,
      ersSummary.transferStatus.getOrElse("Unknown"),
      ersSummary.fileType.getOrElse(""),
      ersSummary.metaData.schemeInfo.timestamp.toString,
      ersSummary.metaData.schemeInfo.taxYear
    )

  def getCountBySchemeTypeWithInDateRange(ersQuery: ERSQuery): Future[Long] = {

    val selector: BsonDocument = combineSelectors(Seq(schemeSelector, dateRangeSelector), ersQuery)

    collection.countDocuments(selector).toFuture()
  }

  def getBundleRefAndSchemeRefBySchemeTypeWithInDateRange(ersQuery: ERSQuery): Future[Seq[(String, String, String)]] = {

    val selector: BsonDocument = combineSelectors(Seq(schemeSelector, dateRangeSelector), ersQuery)

    collection.find[ErsSummary](selector).batchSize(Int.MaxValue).toFuture().map(
      _.map { results =>
        (results.bundleRef, results.metaData.schemeInfo.schemeRef, results.transferStatus.getOrElse("Unknown"))
      }
    )
  }

  def getSchemeRefsInfo(ersQuery: ERSQuery): Future[Seq[ERSMetaDataResults]] = {

    val selector: BsonDocument = combineSelectors(Seq(schemeSelector, dateRangeSelector), ersQuery)

    collection.find[ErsSummary](selector).batchSize(Int.MaxValue).toFuture().map(
      _.map(mapResultToERSMetaDataResults)
    )
  }

  def getRecordsWithTransferStatus(ersQuery: ERSQuery): Future[Seq[ERSMetaDataResults]] = {
    val selector: BsonDocument = combineSelectors(Seq(transferStatusSelector), ersQuery)
    collection
      .find[ErsSummary](selector)
      .batchSize(Int.MaxValue).toFuture().map(
        _.map(mapResultToERSMetaDataResults)
      )
  }

  def getAggregateCountOfSubmissions: Future[Seq[JsObject]] =
    collection
      .aggregate(pipeline = Seq(
        Aggregates.group(
          Document("schemeType" -> "$metaData.schemeInfo.schemeType", "transferStatus" -> "$transferStatus"),
          sum("count", 1)
        )
      ))
      .toFuture()
}
