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

import config.{ApplicationConfig}
import models.{SchemeInfoContainer, SchemeData, SchemeInfo}
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.api.DB
import reactivemongo.bson._
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.mongo.{ReactiveRepository, Repository}
import reactivemongo.api.commands.WriteResult

trait PresubmissionRepository extends Repository[SchemeData, BSONObjectID] {

  def storeJson(presubmissionData: SchemeData): Future[Boolean]

  def getJson(schemeInfo: SchemeInfo): Future[List[SchemeData]]

  def removeJson(schemeInfo: SchemeInfo): Future[Boolean]

  def findAndUpdate(schemeInfo: SchemeInfo): Future[Option[SchemeData]]

}

class PresubmissionMongoRepository()(implicit mongo: () => DB)
  extends ReactiveRepository[SchemeData, BSONObjectID](ApplicationConfig.presubmissionCollection, mongo, SchemeData.format, ReactiveMongoFormats.objectIdFormats)
  with PresubmissionRepository {

  def buildSelector(schemeInfo: SchemeInfo): BSONDocument = BSONDocument(
    "schemeInfo.schemeRef" -> BSONString(schemeInfo.schemeRef),
    "schemeInfo.timestamp" -> BSONLong(schemeInfo.timestamp.getMillis)
  )

  override def storeJson(presubmissionData: SchemeData): Future[Boolean] = {
    collection.insert(presubmissionData).map { res =>
      if(res.hasErrors) {
        Logger.error(s"Faling storing presubmission data. Error: ${res.errmsg.getOrElse("")} for schemeInfo: ${presubmissionData.schemeInfo.toString}")
      }
      res.ok
    }
  }

  override def getJson(schemeInfo: SchemeInfo): Future[List[SchemeData]] = {
    Logger.debug("LFP -> 4. PresubmissionMongoRepository.getJson () ")
    collection.find(
      buildSelector(schemeInfo)
    ).cursor[SchemeData]().collect[List]()
  }

  def count(schemeInfo: SchemeInfo): Future[Int] = {
    collection.count(
      Option(
        buildSelector(schemeInfo).as[collection.pack.Document]
      )
    )
  }

  override def removeJson(schemeInfo: SchemeInfo): Future[Boolean] = {
    val selector = buildSelector(schemeInfo)
    collection.remove(selector).map { res =>
      if(res.hasErrors) {
        Logger.error(s"Deleting presubmission error message: ${res.errmsg} for schemeInfo: ${schemeInfo.toString}")
      }
      res.ok
    }
  }

  override def findAndUpdate(schemeInfo: SchemeInfo): Future[Option[SchemeData]] = {

    val selector: BSONDocument = buildSelector(schemeInfo) ++ ("processed" -> BSONDocument("$exists" -> false))

    val modifier: BSONDocument = BSONDocument(
      "$set" -> BSONDocument("processed" -> true)
    )

    collection.findAndUpdate(selector, modifier).map { result =>
      if(result.lastError.isDefined && result.lastError.get.err.isDefined) {
        Logger.error(s"Error getting presubmission record: ${result.lastError.get.err.toString}")
      }
      result.result[SchemeData]
    }

  }

}
