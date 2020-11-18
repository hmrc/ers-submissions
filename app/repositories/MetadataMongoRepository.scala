/*
 * Copyright 2020 HM Revenue & Customs
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
import models._
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.Cursor
import reactivemongo.api.commands.WriteResult.Message
import reactivemongo.bson._
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MetadataMongoRepository @Inject()(applicationConfig: ApplicationConfig, rmc: ReactiveMongoComponent)
  extends ReactiveRepository[ErsSummary, BSONObjectID](applicationConfig.metadataCollection,
    rmc.mongoConnector.db,
    ErsSummary.format,
    ReactiveMongoFormats.objectIdFormats) {

  def buildSelector(schemeInfo: SchemeInfo): BSONDocument = BSONDocument(
    "metaData.schemeInfo.schemeRef" -> BSONString(schemeInfo.schemeRef),
    "metaData.schemeInfo.timestamp" -> BSONLong(schemeInfo.timestamp.getMillis)
  )

  def storeErsSummary(ersSummary: ErsSummary): Future[Boolean] = {
    collection.insert(ersSummary).map { res =>
      if(res.writeErrors.nonEmpty) {
        Logger.error(s"Faling storing metadata. Error: ${Message.unapply(res).getOrElse("")} for ${ersSummary.metaData.schemeInfo}")
      }
      res.ok
    }
  }

  def getJson(schemeInfo: SchemeInfo): Future[List[ErsSummary]] = {
    collection.find(
      buildSelector(schemeInfo)
    ).cursor[ErsSummary]().collect[List](Int.MaxValue, Cursor.FailOnError[List[ErsSummary]]())
  }

  def updateStatus(schemeInfo: SchemeInfo, status: String): Future[Boolean] = {
    val selector = buildSelector(schemeInfo)
    val update = BSONDocument("$set" -> BSONDocument("transferStatus" ->  status))

    collection.update(selector, update).map { res =>
      if (res.writeErrors.nonEmpty) {
        Logger.warn(s"Faling updating metadata status. Error: ${Message.unapply(res).getOrElse("")} for ${schemeInfo.toString}, status: ${status}")
      }
      res.ok
    }
  }

  def findAndUpdateByStatus(statusList: List[String], resubmitWithNilReturn: Boolean =  true, isResubmitBeforeDate:Boolean = true, schemeRefList: Option[List[String]], schemeType: Option[String]): Future[Option[ErsSummary]] = {
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

    val dateRangeSelector: BSONDocument = if(isResubmitBeforeDate){
      BSONDocument(
        "metaData.schemeInfo.timestamp" -> BSONDocument(
          "$gte" -> DateTime.parse(applicationConfig.scheduleStartDate).getMillis,
          "$lte" -> DateTime.parse(applicationConfig.scheduleEndDate).getMillis
        )
      )
    } else {
      BSONDocument()
    }

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
        val futureTotal = collection.count(Option((statusSelector(status) ++ schemeSelector ++ dateRangeSelector).as[collection.pack.Document]))
        for{
          total <- futureTotal
        }yield {
          Logger.warn(s"The number of ${status} files in the database is: ${total}")
        }
      }
    }

    val selector = baseSelector ++ schemeRefSelector ++ schemeSelector ++ dateRangeSelector

    collection.findAndUpdate(
      selector,
      modifier,
      fetchNewObject = false,
      sort = Some(Json.obj("metaData.schemeInfo.timestamp" -> 1))
    ).map { res =>
      res.result[ErsSummary]
    }
  }

  def findAndUpdateBySchemeType(statusList: List[String], schemeType: String): Future[Option[ErsSummary]] = {
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
