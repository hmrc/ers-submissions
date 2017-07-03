/*
 * Copyright 2017 HM Revenue & Customs
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

import config.ApplicationConfig._
import models.{ERSQuery, ErsSummary}
import org.joda.time.DateTime
import uk.gov.hmrc.mongo.{ReactiveRepository, Repository}
import reactivemongo.api.DB
import reactivemongo.bson._
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait MetaDataVerificationRepository extends Repository[ErsSummary, BSONObjectID] {
  def getCountBySchemeTypeWithInDateRange(ersQuery: ERSQuery): Future[Int]
  def getBundleRefAndSchemeRefBySchemeTypeWithInDateRange(ersQuery: ERSQuery): Future[List[(String,String,String)]]
}

class MetaDataVerificationMongoRepository()(implicit mongo: () => DB)
  extends ReactiveRepository[ErsSummary, BSONObjectID](metadataCollection, mongo, ErsSummary.format, ReactiveMongoFormats.objectIdFormats)
  with MetaDataVerificationRepository {

  override def getCountBySchemeTypeWithInDateRange(ersQuery: ERSQuery): Future[Int] = {

    val dateRangeSelector: BSONDocument = BSONDocument(
      "metaData.schemeInfo.timestamp" -> BSONDocument(
        "$gte" -> DateTime.parse(ersQuery.startDate.getOrElse(defaultScheduleStartDate)).getMillis,
        "$lte" -> DateTime.parse(ersQuery.endDate.getOrElse(defaultScheduleStartDate)).getMillis
      )
    )

    val schemeSelector: BSONDocument = if(ersQuery.schemeType.nonEmpty) {
      BSONDocument(
        "metaData.schemeInfo.schemeType" -> BSONString(ersQuery.schemeType.getOrElse(ersQuerySchemeType))
      )
    }
    else {
      BSONDocument()
    }

    collection.count(Option((schemeSelector ++ dateRangeSelector).as[collection.pack.Document]))
  }

  override def getBundleRefAndSchemeRefBySchemeTypeWithInDateRange(ersQuery: ERSQuery):  Future[List[(String,String,String)]] = {
    val dateRangeSelector: BSONDocument = BSONDocument(
      "metaData.schemeInfo.timestamp" -> BSONDocument(
        "$gte" -> DateTime.parse(ersQuery.startDate.getOrElse(defaultScheduleStartDate)).getMillis,
        "$lte" -> DateTime.parse(ersQuery.endDate.getOrElse(defaultScheduleStartDate)).getMillis
      )
    )

    val schemeSelector: BSONDocument = if (ersQuery.schemeType.nonEmpty) {
      BSONDocument(
        "metaData.schemeInfo.schemeType" -> BSONString(ersQuery.schemeType.getOrElse(ersQuerySchemeType))
      )
    }
    else {
      BSONDocument()
    }

    val selector = (schemeSelector ++ dateRangeSelector).as[collection.pack.Document]

    collection.find(selector).cursor[ErsSummary]().collect[List]().map(
      _.map{ results =>
        (results.bundleRef,results.metaData.schemeInfo.schemeRef,results.transferStatus.getOrElse("Unknown"))
      }
    )

  }

}
