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

import play.api.Logger
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import reactivemongo.api.DB
import reactivemongo.bson._
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.mongo.{ReactiveRepository, Repository}
import models._
import config.ApplicationConfig
import org.joda.time.DateTime

trait MetadataRepository extends Repository[ErsSummary, BSONObjectID] {

  def storeErsSummary(ersSummary: ErsSummary): Future[Boolean]

  def getJson(schemeInfo: SchemeInfo): Future[List[ErsSummary]]

  def updateStatus(schemeInfo: SchemeInfo, status: String): Future[Boolean]

  def findAndUpdateByStatus(statusList: List[String], resubmitWithNilReturn: Boolean,resubmitAfterDate:Boolean = true, schemeRefList: Option[List[String]], schemeType: Option[String]): Future[Option[ErsSummary]]

  def findAndUpdateBySchemeType(statusList: List[String], schemeType: String): Future[Option[ErsSummary]]
}

class MetadataMongoRepository()(implicit mongo: () => DB)
  extends ReactiveRepository[ErsSummary, BSONObjectID](ApplicationConfig.metadataCollection, mongo, ErsSummary.format, ReactiveMongoFormats.objectIdFormats)
  with MetadataRepository {

  def buildSelector(schemeInfo: SchemeInfo): BSONDocument = BSONDocument(
    "metaData.schemeInfo.schemeRef" -> BSONString(schemeInfo.schemeRef),
    "metaData.schemeInfo.timestamp" -> BSONLong(schemeInfo.timestamp.getMillis)
  )

  override def storeErsSummary(ersSummary: ErsSummary): Future[Boolean] = {
    collection.insert(ersSummary).map { res =>
      if(res.hasErrors) {
        Logger.error(s"Faling storing metadata. Error: ${res.errmsg.getOrElse("")} for ${ersSummary.metaData.schemeInfo}")
      }
      res.ok
    }
  }

  override def getJson(schemeInfo: SchemeInfo): Future[List[ErsSummary]] = {
    collection.find(
      buildSelector(schemeInfo)
    ).cursor[ErsSummary]().collect[List]()
  }

  override def updateStatus(schemeInfo: SchemeInfo, status: String): Future[Boolean] = {
    val selector = buildSelector(schemeInfo)
    val update = Json.obj("$set" -> Json.obj("transferStatus" ->  status))

    collection.update(selector, update).map { res =>
      if (res.hasErrors) {
        Logger.warn(s"Faling updating metadata status. Error: ${res.errmsg.getOrElse("")} for ${schemeInfo.toString}, status: ${status}")
      }
      res.ok
    }
  }

  override def findAndUpdateByStatus(statusList: List[String], resubmitWithNilReturn: Boolean =  true, resubmitAfterDate:Boolean = true, schemeRefList: Option[List[String]], schemeType: Option[String]): Future[Option[ErsSummary]] = {
    val baseSelector: BSONDocument = BSONDocument(
      "transferStatus" -> BSONDocument(
        "$in" -> statusList
      )
    )

    val schemeRefSelector: BSONDocument = if(schemeRefList.isDefined) {
      BSONDocument("metaData.schemeInfo.schemeRef" -> BSONDocument("$in" -> schemeRefList.get))
    }
    else {
      BSONDocument()
    }

    val schemeSelector: BSONDocument = if(schemeType.isDefined) {
      BSONDocument(
        "metaData.schemeInfo.schemeType" -> schemeType.get
      )
    }
    else {
      BSONDocument()
    }

    val nilReturnSelector: BSONDocument = if(resubmitWithNilReturn) {
      BSONDocument()
    }
    else {
      BSONDocument(
        "isNilReturn" -> "1"
      )
    }

    val isAfterDateSelector: BSONDocument = if(resubmitAfterDate){
      BSONDocument(
        "metaData.schemeInfo.timestamp" -> BSONDocument(
          "$lte" -> DateTime.parse("2017-06-02").getMillis
        )
      )
    }else(
      BSONDocument()
    )

    val modifier: BSONDocument = BSONDocument(
      "$set" -> BSONDocument(
        "transferStatus" -> Statuses.Process.toString
      )
    )

    def statusSelector(status: String) = {
      BSONDocument("transferStatus" -> status)
    }

    val countByStatus = {
      for(status <- statusList) {
        val futureTotal = collection.count(Option((statusSelector(status) ++ schemeSelector ++ isAfterDateSelector).as[collection.pack.Document]))
        for{
          total <- futureTotal
        }yield {
          Logger.warn(s"The number of ${status.toUpperCase} files in the database is: ${total}")
        }
      }
    }


    collection.findAndUpdate(
      baseSelector ++ schemeRefSelector ++ schemeSelector ++ isAfterDateSelector,
      modifier,
      fetchNewObject = false,
      sort = Some(Json.obj("metaData.schemeInfo.timestamp" -> 1))
    ).map { res =>
      res.result[ErsSummary]
    }
  }

  override def findAndUpdateBySchemeType(statusList: List[String], schemeType: String): Future[Option[ErsSummary]] = {
    val baseSelector: BSONDocument = BSONDocument(
      "transferStatus" -> BSONDocument(
        "$in" -> statusList
      )
    )

    val selector: BSONDocument = baseSelector ++ BSONDocument("metaData.schemeInfo.schemeType" -> BSONDocument("$in" -> schemeType))

//    val selector: BSONDocument = if(schemeRefList.isDefined) {
//      baseSelector ++ BSONDocument("metaData.schemeInfo.schemeRef" -> BSONDocument("$in" -> schemeRefList.get))
//    }
//    else {
//      baseSelector
//    }

    val modifier: BSONDocument = BSONDocument(
      "$set" -> BSONDocument(
        "transferStatus" -> Statuses.Process.toString
      )
    )

    collection.findAndUpdate(selector, modifier, fetchNewObject = false, sort = Some(Json.obj("metaData.schemeInfo.timestamp" -> 1))).map { res =>
      res.result[ErsSummary]
    }
  }

}
