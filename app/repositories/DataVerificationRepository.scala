/*
 * Copyright 2019 HM Revenue & Customs
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
import config.ApplicationConfig._
import models.{ERSDataResults, ERSQuery, SchemeData}
import org.joda.time.DateTime
import reactivemongo.api.DB
import reactivemongo.bson._
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.mongo.{ReactiveRepository, Repository}
import reactivemongo.play.json.ImplicitBSONHandlers._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait DataVerificationRepository extends Repository[ERSQuery, BSONObjectID] {
  def getCountBySchemeTypeWithInDateRange(ersQuery: ERSQuery): Future[Int]
  def getSchemeRefBySchemeTypeWithInDateRange(ersQuery: ERSQuery): Future[List[String]]
  def getSchemeRefsInfo(ersQuery: ERSQuery): Future[List[ERSDataResults]]
}

class DataVerificationMongoRepository()(implicit mongo: () => DB)
  extends ReactiveRepository[ERSQuery, BSONObjectID](ApplicationConfig.presubmissionCollection, mongo, ERSQuery.format, ReactiveMongoFormats.objectIdFormats)
  with DataVerificationRepository {

  override def getCountBySchemeTypeWithInDateRange(ersQuery: ERSQuery): Future[Int] = {

    val dateRangeSelector: BSONDocument = BSONDocument(
      "schemeInfo.timestamp" -> BSONDocument(
        "$gte" -> DateTime.parse(ersQuery.startDate.getOrElse(defaultScheduleStartDate)).getMillis,
        "$lte" -> DateTime.parse(ersQuery.endDate.getOrElse(defaultScheduleStartDate)).getMillis
      )
    )

    val schemeSelector: BSONDocument = if(ersQuery.schemeType.nonEmpty) {
      BSONDocument(
        "schemeInfo.schemeType" -> BSONString(ersQuery.schemeType.getOrElse(ersQuerySchemeType))
      )
    }
    else {
      BSONDocument()
    }

    collection.count(Option((schemeSelector ++ dateRangeSelector).as[collection.pack.Document]))
  }

  override def getSchemeRefBySchemeTypeWithInDateRange(ersQuery: ERSQuery): Future[List[String]] = {
    val dateRangeSelector: BSONDocument = BSONDocument(
      "schemeInfo.timestamp" -> BSONDocument(
        "$gte" -> DateTime.parse(ersQuery.startDate.getOrElse(defaultScheduleStartDate)).getMillis,
        "$lte" -> DateTime.parse(ersQuery.endDate.getOrElse(defaultScheduleStartDate)).getMillis
      )
    )

    val schemeSelector: BSONDocument = if (ersQuery.schemeType.nonEmpty) {
      BSONDocument(
        "schemeInfo.schemeType" -> BSONString(ersQuery.schemeType.getOrElse(ersQuerySchemeType))
      )
    }
    else {
      BSONDocument()
    }

    val selector = (schemeSelector ++ dateRangeSelector).as[collection.pack.Document]

    collection.find(selector).cursor[SchemeData]().collect[List]().map(
      _.map(_.schemeInfo.schemeRef)
    )

  }

  override def getSchemeRefsInfo(ersQuery: ERSQuery): Future[List[ERSDataResults]] = {
    val dateRangeSelector: BSONDocument = BSONDocument(
      "schemeInfo.timestamp" -> BSONDocument(
        "$gte" -> DateTime.parse(ersQuery.startDate.getOrElse(defaultScheduleStartDate)).getMillis,
        "$lte" -> DateTime.parse(ersQuery.endDate.getOrElse(defaultScheduleStartDate)).getMillis
      )
    )

    val schemeSelector: BSONDocument = if (ersQuery.schemeType.nonEmpty) {
      BSONDocument(
        "schemeInfo.schemeType" -> BSONString(ersQuery.schemeType.getOrElse(ersQuerySchemeType))
      )
    }
    else {
      BSONDocument()
    }

    val schemeRefsSelector: BSONDocument = if(!ersQuery.schemeRefsList.isEmpty) {
      BSONDocument("schemeInfo.schemeRef" -> BSONDocument("$in" -> ersQuery.schemeRefsList))
    }
    else {
      BSONDocument()
    }

    val selector = (schemeSelector ++ dateRangeSelector ++ schemeRefsSelector).as[collection.pack.Document]

    collection.find(selector).cursor[SchemeData]().collect[List]().map(
      _.map{ results =>
        ERSDataResults(results.schemeInfo.schemeRef, results.schemeInfo.taxYear, results.schemeInfo.timestamp.toString, results.sheetName)
      }
    )

  }

}
