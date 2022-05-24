/*
 * Copyright 2022 HM Revenue & Customs
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
import org.mongodb.scala.bson.{BsonDocument, BsonInt64, BsonString}
import org.mongodb.scala.model.Sorts._
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import play.api.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PresubmissionMongoRepository @Inject()(applicationConfig: ApplicationConfig, mc: MongoComponent)
                                            (implicit ec: ExecutionContext)
  extends PlayMongoRepository[SchemeData](
    mongoComponent = mc,
    collectionName = applicationConfig.presubmissionCollection,
    domainFormat = SchemeData.format,
    indexes = Seq(
      IndexModel(ascending("schemeInfo.schemeRef"), IndexOptions().name("schemeRef")),
      IndexModel(ascending("schemeInfo.timestamp"), IndexOptions().name("timestamp"))
    )
  ) with Logging {

  def buildSelector(schemeInfo: SchemeInfo): BsonDocument = BsonDocument(
    "schemeInfo.schemeRef" -> BsonString(schemeInfo.schemeRef),
    "schemeInfo.timestamp" -> BsonInt64(schemeInfo.timestamp.getMillis)
  )

  def storeJson(presubmissionData: SchemeData): Future[Boolean] = {
    collection.insertOne(presubmissionData).toFuture.map { res =>
      res.wasAcknowledged()
    }
  }

  def storeJsonV2(schemeInfo: String, presubmissionData: SchemeData): Future[Boolean] = {
      collection.insertOne(presubmissionData).toFuture.map { res =>
      res.wasAcknowledged()
    }.recover {
      case e: Throwable =>
        logger.error(s"Failed storing presubmission data. Error: ${e.getMessage} for schemeInfo: ${schemeInfo}")
        throw e
    }
  }

  def getJson(schemeInfo: SchemeInfo): Future[Seq[SchemeData]] = {
    logger.debug("LFP -> 4. PresubmissionMongoRepository.getJson () ")
    collection.find(
      buildSelector(schemeInfo)
    ).batchSize(Int.MaxValue).toFuture()
  }

  def count(schemeInfo: SchemeInfo): Future[Long] = {
    collection.countDocuments(
        buildSelector(schemeInfo)
    ).toFuture()
  }

  def removeJson(schemeInfo: SchemeInfo): Future[Boolean] = {
    val selector = buildSelector(schemeInfo)
    collection.deleteOne(selector).toFuture.map { res =>
      res.wasAcknowledged()
    }
  }

}
