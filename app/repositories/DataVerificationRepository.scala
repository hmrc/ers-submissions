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

import config.ApplicationConfig
import models.ERSQuery
import org.joda.time.DateTime
import uk.gov.hmrc.mongo.{ReactiveRepository, Repository}
import reactivemongo.api.DB
import reactivemongo.bson._
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait DataVerificationRepository extends Repository[ERSQuery, BSONObjectID] {
  def getCountBySchemeTypeWithInDateRange(ersQuery: ERSQuery): Future[Int]
}

class DataVerificationMongoRepository()(implicit mongo: () => DB)
  extends ReactiveRepository[ERSQuery, BSONObjectID](ApplicationConfig.presubmissionCollection, mongo, ERSQuery.format, ReactiveMongoFormats.objectIdFormats)
  with DataVerificationRepository {

  override def getCountBySchemeTypeWithInDateRange(ersQuery: ERSQuery): Future[Int] = {

    val dateRangeSelector: BSONDocument = BSONDocument(
      "schemeInfo.timestamp" -> BSONDocument(
        "$gte" -> DateTime.parse(ersQuery.startDate.getOrElse("2016-04-01")).getMillis,
        "$lte" -> DateTime.parse(ersQuery.endDate.getOrElse("2016-04-01")).getMillis
      )
    )

    val schemeSelector: BSONDocument = if(ersQuery.schemeType.nonEmpty) {
      BSONDocument(
        "schemeInfo.schemeType" -> BSONString(ersQuery.schemeType.getOrElse(""))
      )
    }
    else {
      BSONDocument()
    }

    collection.count(Option((schemeSelector ++ dateRangeSelector).as[collection.pack.Document]))
  }

}