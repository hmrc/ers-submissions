/*
 * Copyright 2021 HM Revenue & Customs
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
import models.{ERSDataResults, ERSQuery, SchemeData}
import org.mongodb.scala.bson.BsonDocument
import repositories.helpers.BaseVerificationRepository
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DataVerificationMongoRepository @Inject()(val applicationConfig: ApplicationConfig, mc: MongoComponent)(implicit ec: ExecutionContext)
  extends PlayMongoRepository[ERSQuery](
    mongoComponent = mc,
    collectionName = applicationConfig.presubmissionCollection,
    domainFormat = ERSQuery.format,
    indexes = Seq.empty
    ) with BaseVerificationRepository {

  def getCountBySchemeTypeWithInDateRange(ersQuery: ERSQuery): Future[Long] = {
    val selector: BsonDocument = combineSelectors(Seq(schemeSelector, dateRangeSelector), ersQuery)

    collection.countDocuments(selector).toFuture()
  }

  def getSchemeRefBySchemeTypeWithInDateRange(ersQuery: ERSQuery): Future[Seq[String]] = {

    val selector: BsonDocument = combineSelectors(Seq(schemeSelector, dateRangeSelector), ersQuery)

    collection.find[SchemeData](selector).batchSize(Int.MaxValue).toFuture().map(
      _.map(_.schemeInfo.schemeRef)
    )
  }

   def getSchemeRefsInfo(ersQuery: ERSQuery): Future[Seq[ERSDataResults]] = {

    val selector: BsonDocument = combineSelectors(Seq(schemeSelector, dateRangeSelector, schemeRefsSelector), ersQuery)

    collection.find[SchemeData](selector).batchSize(Int.MaxValue).toFuture().map(
      _.map{ results =>
        ERSDataResults(results.schemeInfo.schemeRef, results.schemeInfo.taxYear, results.schemeInfo.timestamp.toString, results.sheetName)
      }
    )
  }
}
