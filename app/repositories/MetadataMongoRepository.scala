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

import org.joda.time.DateTime
import play.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import reactivemongo.api.DB
import reactivemongo.bson._
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.mongo.{ReactiveRepository, Repository}
import models._
import config.ApplicationConfig

import scala.util.Try

trait MetadataRepository extends Repository[ErsSummary, BSONObjectID] {

  def storeErsSummary(ersSummary: ErsSummary): Future[Boolean]

  def getErsSummary(schemeInfo: SchemeInfo): Future[Option[ErsSummary]]

  def getJson(schemeInfo: SchemeInfo): Future[List[ErsSummary]]

  def getSchemeRefs(startDate: DateTime, endDate: DateTime, exclude: List[String]): Future[List[String]]

  def getSchemeInfo(startDate: DateTime, endDate: DateTime, exclude: List[SchemeInfo]): Future[List[SchemeInfo]]

  def getSchemeInfoBySchemeRefs(schemeRefs: List[String]): Future[List[SchemeInfo]]
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

  override def getErsSummary(schemeInfo: SchemeInfo): Future[Option[ErsSummary]] = {
    val selector: BSONDocument = buildSelector(schemeInfo)
    collection.find(selector).one[ErsSummary]
  }

  override def getJson(schemeInfo: SchemeInfo): Future[List[ErsSummary]] = {
    collection.find(
      buildSelector(schemeInfo)
    ).cursor[ErsSummary]().collect[List]()
  }

  override def getSchemeRefs(startDate: DateTime, endDate: DateTime, exclude: List[String]): Future[List[String]] = {
    collection.find(
      BSONDocument(
        "metaData.schemeInfo.timestamp" -> BSONDocument(
          "$gte" -> startDate.getMillis,
          "$lte" -> endDate.getMillis
        ),
        "isNilReturn" -> IsNilReturn.False.toString,
        "metaData.schemeInfo.schemeRef" -> BSONDocument(
          "$nin" -> exclude
        )
      ),
      BSONDocument(
        "metaData.schemeInfo" -> 1
      )
    ).cursor[MetaDataContainer]().collect[List]().map(_.map(_.metaData.schemeInfo.schemeRef))
  }

  override def getSchemeInfo(startDate: DateTime, endDate: DateTime, exclude: List[SchemeInfo]): Future[List[SchemeInfo]] = {
    val schemeInfoPerDay = collection.find(
      BSONDocument(
        "metaData.schemeInfo.timestamp" -> BSONDocument(
          "$gte" -> startDate.getMillis,
          "$lte" -> endDate.getMillis
        )
      ),
      BSONDocument(
        "metaData.schemeInfo" -> 1
      )
    ).cursor[FullMetaDataContainer]().collect[List]().map(_.map(_.metaData.schemeInfo))

    if(exclude.isEmpty) {
      schemeInfoPerDay
    }
    else {
      schemeInfoPerDay.map { res =>
        res.filterNot(exclude.contains(_))
      }
    }
  }

  override def getSchemeInfoBySchemeRefs(schemeRefs: List[String]): Future[List[SchemeInfo]] = {
    collection.find(
      BSONDocument(
        "metaData.schemeInfo.schemeRef" -> BSONDocument(
          "$in" -> schemeRefs
        )
      ),
      BSONDocument(
        "metaData.schemeInfo" -> 1
      )
    ).cursor[FullMetaDataContainer]().collect[List]().map(_.map(_.metaData.schemeInfo))
  }
}
