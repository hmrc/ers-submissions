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
import javax.inject.Inject
import models.{ERSMetaDataResults, ERSQuery, ErsSummary}
import org.joda.time.DateTime
import play.api.libs.json.JsObject
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.{Cursor, DB}
import reactivemongo.bson._
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MetaDataVerificationMongoRepository @Inject()(applicationConfig: ApplicationConfig, rmc: ReactiveMongoComponent)
  extends ReactiveRepository[ErsSummary, BSONObjectID](applicationConfig.metadataCollection,
    rmc.mongoConnector.db,
    ErsSummary.format,
    ReactiveMongoFormats.objectIdFormats) {

  def getCountBySchemeTypeWithInDateRange(ersQuery: ERSQuery): Future[Int] = {

    val dateRangeSelector: BSONDocument = BSONDocument(
      "metaData.schemeInfo.timestamp" -> BSONDocument(
        "$gte" -> DateTime.parse(ersQuery.startDate.getOrElse(applicationConfig.defaultScheduleStartDate)).getMillis,
        "$lte" -> DateTime.parse(ersQuery.endDate.getOrElse(applicationConfig.defaultScheduleStartDate)).getMillis
      )
    )

    val schemeSelector: BSONDocument = if(ersQuery.schemeType.nonEmpty) {
      BSONDocument(
        "metaData.schemeInfo.schemeType" -> BSONString(ersQuery.schemeType.getOrElse(applicationConfig.ersQuerySchemeType))
      )
    }
    else {
      BSONDocument()
    }

    collection.count(Option((schemeSelector ++ dateRangeSelector).as[collection.pack.Document]))
  }

  def getBundleRefAndSchemeRefBySchemeTypeWithInDateRange(ersQuery: ERSQuery): Future[List[(String,String,String)]] = {
    val dateRangeSelector: BSONDocument = BSONDocument(
      "metaData.schemeInfo.timestamp" -> BSONDocument(
        "$gte" -> DateTime.parse(ersQuery.startDate.getOrElse(applicationConfig.defaultScheduleStartDate)).getMillis,
        "$lte" -> DateTime.parse(ersQuery.endDate.getOrElse(applicationConfig.defaultScheduleStartDate)).getMillis
      )
    )

    val schemeSelector: BSONDocument = if (ersQuery.schemeType.nonEmpty) {
      BSONDocument(
        "metaData.schemeInfo.schemeType" -> BSONString(ersQuery.schemeType.getOrElse(applicationConfig.ersQuerySchemeType))
      )
    }
    else {
      BSONDocument()
    }

    val selector = (schemeSelector ++ dateRangeSelector).as[collection.pack.Document]

    collection.find(selector).cursor[ErsSummary]().collect[List](Int.MaxValue, Cursor.FailOnError[List[ErsSummary]]()).map(
      _.map{ results =>
        (results.bundleRef,results.metaData.schemeInfo.schemeRef,results.transferStatus.getOrElse("Unknown"))
      }
    )
  }

  def getSchemeRefsInfo(ersQuery: ERSQuery): Future[List[ERSMetaDataResults]] = {
    val dateRangeSelector: BSONDocument = BSONDocument(
      "metaData.schemeInfo.timestamp" -> BSONDocument(
        "$gte" -> DateTime.parse(ersQuery.startDate.getOrElse(applicationConfig.defaultScheduleStartDate)).getMillis,
        "$lte" -> DateTime.parse(ersQuery.endDate.getOrElse(applicationConfig.defaultScheduleStartDate)).getMillis
      )
    )

    val schemeSelector: BSONDocument = if (ersQuery.schemeType.nonEmpty) {
      BSONDocument(
        "metaData.schemeInfo.schemeType" -> BSONString(ersQuery.schemeType.getOrElse(applicationConfig.ersQuerySchemeType))
      )
    }
    else {
      BSONDocument()
    }

    val schemeRefsSelector: BSONDocument = if(!ersQuery.schemeRefsList.isEmpty) {
      BSONDocument("metaData.schemeInfo.schemeRef" -> BSONDocument("$in" -> ersQuery.schemeRefsList))
    }
    else {
      BSONDocument()
    }

    val selector = (schemeSelector ++ dateRangeSelector ++ schemeRefsSelector).as[collection.pack.Document]

    collection.find(selector).cursor[ErsSummary]().collect[List](Int.MaxValue, Cursor.FailOnError[List[ErsSummary]]()).map(
      _.map{results =>
        ERSMetaDataResults(results.bundleRef, results.metaData.schemeInfo.schemeRef,
          results.transferStatus.getOrElse("Unknown"),
          results.fileType.getOrElse(""), results.metaData.schemeInfo.timestamp.toString,
          results.metaData.schemeInfo.taxYear)
      }
    )
  }
}
