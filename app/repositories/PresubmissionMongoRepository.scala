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
import models.{SchemeData, SchemeInfo}
import play.api.Logger
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.Cursor
import reactivemongo.api.commands.WriteResult.Message
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson._
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import javax.inject.Inject

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, ExecutionContext, Future}

class PresubmissionMongoRepository @Inject()(applicationConfig: ApplicationConfig, rmc: ReactiveMongoComponent)
  extends ReactiveRepository[SchemeData, BSONObjectID](applicationConfig.presubmissionCollection,
    rmc.mongoConnector.db,
    SchemeData.format,
    ReactiveMongoFormats.objectIdFormats) {

  ensureIndexes(ExecutionContext.Implicits.global)

  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] = {
    Await.result(collection.indexesManager(ec).list(), scala.concurrent.duration.Duration.Inf) //do this to make the connection to the DB for ensure indexes
    super.ensureIndexes(ec)
  }

  override def indexes: Seq[Index] = {
    Seq(
      Index(
        Seq(("schemeInfo.schemeRef", IndexType.Ascending)),
        name = Some("schemeRef")
      ),
      Index(
        Seq(("schemeInfo.timestamp", IndexType.Ascending)),
        name = Some("timestamp")
      )
    )
  }

  def buildSelector(schemeInfo: SchemeInfo): BSONDocument = BSONDocument(
    "schemeInfo.schemeRef" -> BSONString(schemeInfo.schemeRef),
    "schemeInfo.timestamp" -> BSONLong(schemeInfo.timestamp.getMillis)
  )

  def storeJson(presubmissionData: SchemeData): Future[Boolean] = {
    collection.insert(presubmissionData).map { res =>
      if(res.writeErrors.nonEmpty) {
        Logger.error(s"[PresubmissionMongoRepository][storeJson] Failed storing presubmission data. Error: " +
          s"${Message.unapply(res).getOrElse("")} for schemeInfo:" +
          s" ${presubmissionData.schemeInfo.toString}"
        )
      }
      res.ok
    }
  }

  def storeJsonV2(schemeInfo: String, presubmissionData: JsObject): Future[Boolean] = {
    collection.insert(ordered = false).one(presubmissionData).map { res =>
      if(res.writeErrors.nonEmpty) {
        Logger.error(s"[PresubmissionMongoRepository][storeJsonV2] Failed storing presubmission data. Error: " +
          s"${Message.unapply(res).getOrElse("")} for schemeInfo: ${schemeInfo}")
      }
      res.ok
    }.recover {
      case e: Throwable =>
        Logger.error(s"Failed storing presubmission data. Error: ${e.getMessage} for schemeInfo: ${schemeInfo}")
        throw e
    }
  }

  def getJson(schemeInfo: SchemeInfo): Future[List[SchemeData]] = {
    Logger.debug("LFP -> 4. PresubmissionMongoRepository.getJson () ")
    collection.find(
      buildSelector(schemeInfo)
    ).cursor[SchemeData]().collect[List](Int.MaxValue, Cursor.FailOnError[List[SchemeData]]())
  }

  def count(schemeInfo: SchemeInfo): Future[Int] = {
    collection.count(
      Option(
        buildSelector(schemeInfo).as[collection.pack.Document]
      )
    )
  }

  def removeJson(schemeInfo: SchemeInfo): Future[Boolean] = {
    val selector = buildSelector(schemeInfo)
    collection.remove(selector).map { res =>
      if(res.writeErrors.nonEmpty) {
        Logger.error(s"Deleting presubmission error message: ${Message.unapply(res).getOrElse("")} for schemeInfo: ${schemeInfo.toString}")
      }
      res.ok
    }
  }

}
