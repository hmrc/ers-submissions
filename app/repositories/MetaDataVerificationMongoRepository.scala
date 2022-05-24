/*
 * Copyright 2022 HM Revenue & Customs
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
import org.mongodb.scala.bson.BsonDocument
import repositories.helpers.BaseVerificationRepository
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MetaDataVerificationMongoRepository @Inject()(val applicationConfig: ApplicationConfig, mc: MongoComponent)(implicit ec: ExecutionContext)
  extends PlayMongoRepository[ErsSummary](
    mongoComponent = mc,
    collectionName = applicationConfig.metadataCollection,
    domainFormat = ErsSummary.format,
    indexes = Seq.empty
  ) with BaseVerificationRepository {

  override val mongoKeyPrefix: String = "metadata."

  def getCountBySchemeTypeWithInDateRange(ersQuery: ERSQuery): Future[Long] = {

    val selector: BsonDocument = combineSelectors(Seq(schemeSelector, dateRangeSelector), ersQuery)

    collection.countDocuments(selector).toFuture()
  }

  def getBundleRefAndSchemeRefBySchemeTypeWithInDateRange(ersQuery: ERSQuery): Future[Seq[(String, String, String)]] = {

    val selector: BsonDocument = combineSelectors(Seq(schemeSelector, dateRangeSelector), ersQuery)

    collection.find(selector).batchSize(Int.MaxValue).toFuture().map(
      _.map { results =>
        (results.bundleRef, results.metaData.schemeInfo.schemeRef, results.transferStatus.getOrElse("Unknown"))
      }
    )
  }

  def getSchemeRefsInfo(ersQuery: ERSQuery): Future[Seq[ERSMetaDataResults]] = {

    val selector: BsonDocument = combineSelectors(Seq(schemeSelector, dateRangeSelector, schemeRefsSelector), ersQuery)

    collection.find(selector).batchSize(Int.MaxValue).toFuture().map(
      _.map { results =>
        ERSMetaDataResults(results.bundleRef, results.metaData.schemeInfo.schemeRef,
          results.transferStatus.getOrElse("Unknown"),
          results.fileType.getOrElse(""), results.metaData.schemeInfo.timestamp.toString,
          results.metaData.schemeInfo.taxYear)
      }
    )
  }
}
