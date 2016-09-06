/*
 * Copyright 2016 HM Revenue & Customs
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

import models._
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.Json
import reactivemongo.api.DB
import reactivemongo.bson._
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.mongo.{ReactiveRepository, Repository}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait JsonStoreInfoRepository extends Repository[ErsJsonStoreInfo, BSONObjectID] {
  def createErsJsonStoreInfo(ersJsonStoreInfo: ErsJsonStoreInfo): Future[Boolean]
  def updateStatus(status: String, schemeInfo: SchemeInfo): Future[Boolean]
  def findPostsubmission(schemeInfo: SchemeInfo): Future[List[ErsJsonStoreInfo]]
  def findJsonStoreInfoByStatus(statuses: List[String], limit: Int): Future[List[ErsJsonStoreInfo]]
  def findAndUpdateByStatus(statusList: List[String], schemeRefList: Option[List[String]]): Future[Option[ErsJsonStoreInfo]]
  def getSchemeInfoForPeriod(startDate: DateTime, endDate: DateTime): Future[List[SchemeInfo]]
  def getSchemeInfoBySchemeRefs(schemeRefs: List[String]): Future[List[SchemeInfo]]
}

class JsonStoreInfoMongoRepository()(implicit mongo: () => DB)
  extends ReactiveRepository[ErsJsonStoreInfo, BSONObjectID]("ErsJsonStoreInfo", mongo, ErsJsonStoreInfo.format, ReactiveMongoFormats.objectIdFormats)
  with JsonStoreInfoRepository {

  override def createErsJsonStoreInfo(ersJsonStoreInfo: ErsJsonStoreInfo): Future[Boolean] = {
    collection.insert(ersJsonStoreInfo).map { res =>
      Logger.debug("LFP -> 12. In PostsubmissionMongoRepository.createErsJsonStoreInfo ")
      if(res.hasErrors) {
        Logger.error(s"Faling storing ersJsonStoreInfo Error: ${res.errmsg.getOrElse("")} for ${ersJsonStoreInfo.schemeInfo.toString}")
      }
      res.ok
    }
  }

  override def updateStatus(status: String, schemeInfo: SchemeInfo): Future[Boolean] = {
    val selector = Json.obj(
      "schemeInfo.schemeRef" -> schemeInfo.schemeRef,
      "schemeInfo.timestamp" -> schemeInfo.timestamp
    )
    val update = Json.obj("$set" -> Json.obj("status" ->  status))

    collection.update(selector, update).map { res =>
      if (res.hasErrors) {
        Logger.warn(s"Faling updating postsubmission status. Error: ${res.errmsg.getOrElse("")} for ${schemeInfo.toString}, status: ${status}")
      }
      res.ok
    }
  }

  override def findPostsubmission(schemeInfo: SchemeInfo): Future[List[ErsJsonStoreInfo]] = {
    collection.find(
      BSONDocument(
        "schemeInfo.schemeRef" -> BSONString(schemeInfo.schemeRef),
        "schemeInfo.timestamp" -> BSONLong(schemeInfo.timestamp.getMillis)
      )
    ).cursor[ErsJsonStoreInfo]().collect[List]()
  }

  override def findJsonStoreInfoByStatus(statusList: List[String], limit: Int): Future[List[ErsJsonStoreInfo]] = {
    val selector: BSONDocument = BSONDocument(
      "status" -> BSONDocument(
        "$in" -> statusList
      ),
      "fileId" -> BSONDocument(
        "$exists" -> false
      )
    )
    collection.find(selector).cursor[ErsJsonStoreInfo]().collect[List](upTo = limit)
  }

  override def findAndUpdateByStatus(statusList: List[String], schemeRefList: Option[List[String]]): Future[Option[ErsJsonStoreInfo]] = {
    val baseSelector: BSONDocument = BSONDocument(
      "status" -> BSONDocument(
        "$in" -> statusList
      )
    )

    val selector: BSONDocument = if(schemeRefList.isDefined) {
      baseSelector ++ BSONDocument("schemeInfo.schemeRef" -> BSONDocument("$in" -> schemeRefList.get))
    }
    else {
      baseSelector
    }

    val modifier: BSONDocument = BSONDocument(
      "$set" -> BSONDocument(
        "status" -> Statuses.Process.toString
      )
    )

    collection.findAndUpdate(selector, modifier, fetchNewObject = false, sort = Some(Json.obj("schemeInfo.timestamp" -> 1))).map { res =>
      res.result[ErsJsonStoreInfo]
    }
  }

  override def getSchemeInfoForPeriod(startDate: DateTime, endDate: DateTime): Future[List[SchemeInfo]] = {
    collection.find(
      BSONDocument(
        "schemeInfo.timestamp" -> BSONDocument(
          "$gte" -> startDate.getMillis,
          "$lte" -> endDate.getMillis
        )
      ),
      BSONDocument(
        "schemeInfo" -> 1
      )
    ).cursor[FullSchemeInfoContainer]().collect[List]().map(_.map(_.schemeInfo))
  }

  override def getSchemeInfoBySchemeRefs(schemeRefs: List[String]): Future[List[SchemeInfo]] = {
    collection.find(
      BSONDocument(
        "schemeInfo.schemeRef" -> BSONDocument(
          "$in" -> schemeRefs
        )
      ),
      BSONDocument(
        "schemeInfo" -> 1
      )
    ).cursor[FullSchemeInfoContainer]().collect[List]().map(_.map(_.schemeInfo))
  }
}
