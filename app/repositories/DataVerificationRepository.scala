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
import models.{ERSDataResults, ERSQuery, SchemeData}
import org.joda.time.DateTime
import play.api.libs.json.JsObject
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.Cursor
import reactivemongo.bson._
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DataVerificationMongoRepository @Inject()(applicationConfig: ApplicationConfig, rmc: ReactiveMongoComponent)
  extends ReactiveRepository[ERSQuery, BSONObjectID](applicationConfig.presubmissionCollection,
    rmc.mongoConnector.db,
    ERSQuery.format,
    ReactiveMongoFormats.objectIdFormats) {

  def getCountBySchemeTypeWithInDateRange(ersQuery: ERSQuery): Future[Int] = {

    val dateRangeSelector: BSONDocument = BSONDocument(
      "schemeInfo.timestamp" -> BSONDocument(
        "$gte" -> DateTime.parse(ersQuery.startDate.getOrElse(applicationConfig.defaultScheduleStartDate)).getMillis,
        "$lte" -> DateTime.parse(ersQuery.endDate.getOrElse(applicationConfig.defaultScheduleStartDate)).getMillis
      )
    )

    val schemeSelector: BSONDocument = if(ersQuery.schemeType.nonEmpty) {
      BSONDocument(
        "schemeInfo.schemeType" -> BSONString(ersQuery.schemeType.getOrElse(applicationConfig.ersQuerySchemeType))
      )
    }
    else {
      BSONDocument()
    }

    collection.count(Option((schemeSelector ++ dateRangeSelector).as[collection.pack.Document]))
  }

  def getSchemeRefBySchemeTypeWithInDateRange(ersQuery: ERSQuery): Future[List[String]] = {
    val dateRangeSelector: BSONDocument = BSONDocument(
      "schemeInfo.timestamp" -> BSONDocument(
        "$gte" -> DateTime.parse(ersQuery.startDate.getOrElse(applicationConfig.defaultScheduleStartDate)).getMillis,
        "$lte" -> DateTime.parse(ersQuery.endDate.getOrElse(applicationConfig.defaultScheduleStartDate)).getMillis
      )
    )

    val schemeSelector: BSONDocument = if (ersQuery.schemeType.nonEmpty) {
      BSONDocument(
        "schemeInfo.schemeType" -> BSONString(ersQuery.schemeType.getOrElse(applicationConfig.ersQuerySchemeType))
      )
    }
    else {
      BSONDocument()
    }

    val selector: JsObject = (schemeSelector ++ dateRangeSelector).as[collection.pack.Document]

    collection.find(selector).cursor[SchemeData]().collect[List](Int.MaxValue, Cursor.FailOnError[List[SchemeData]]()).map(
      _.map(_.schemeInfo.schemeRef)
    )
  }

   def getSchemeRefsInfo(ersQuery: ERSQuery): Future[List[ERSDataResults]] = {
    val dateRangeSelector: BSONDocument = BSONDocument(
      "schemeInfo.timestamp" -> BSONDocument(
        "$gte" -> DateTime.parse(ersQuery.startDate.getOrElse(applicationConfig.defaultScheduleStartDate)).getMillis,
        "$lte" -> DateTime.parse(ersQuery.endDate.getOrElse(applicationConfig.defaultScheduleStartDate)).getMillis
      )
    )

    val schemeSelector: BSONDocument = if (ersQuery.schemeType.nonEmpty) {
      BSONDocument(
        "schemeInfo.schemeType" -> BSONString(ersQuery.schemeType.getOrElse(applicationConfig.ersQuerySchemeType))
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

    collection.find(selector).cursor[SchemeData]().collect[List](Int.MaxValue, Cursor.FailOnError[List[SchemeData]]()).map(
      _.map{ results =>
        ERSDataResults(results.schemeInfo.schemeRef, results.schemeInfo.taxYear, results.schemeInfo.timestamp.toString, results.sheetName)
      }
    )
  }
}
