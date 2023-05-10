/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc

import org.mongodb.scala.model.Filters
import repositories.MetadataMongoRepository
import scheduler.{ResubmissionServiceImpl, ScheduledJob}
import _root_.play.api.Application
import _root_.play.api.libs.json.{JsObject, Json}
import _root_.play.api.test.Helpers._
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.conversions.Bson

import scala.concurrent.{ExecutionContext, Future}

case class ResubmissionJobSetUp(app: Application) {

  val metadataMongoRepository: MetadataMongoRepository = app.injector.instanceOf[MetadataMongoRepository]
  val collection: MongoCollection[JsObject] = metadataMongoRepository.collection
  await(collection.drop().toFuture())

  def getJob: ScheduledJob = app.injector.instanceOf[ResubmissionServiceImpl]

  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  def countMetadataRecordsWithSelector(filter: Bson): Long = await(
    collection.countDocuments(filter)
      .toFuture()
  )

  def storeMultipleErsSummary(ersSummaries: Seq[JsObject])(implicit ec: ExecutionContext): Future[Boolean] = {
    collection.insertMany(ersSummaries).toFuture.map { res =>
      res.wasAcknowledged()
    }
  }

  def createFailedJobSelector(dateFilter: Option[String]): BsonDocument = metadataMongoRepository.createFailedJobSelector(
    statusList = List("failed"),
    schemeRefList = None,
    schemeType = Some("CSOP"),
    dateFilter
  )

  val successResubmitTransferStatusSelector: Bson = Filters.eq("transferStatus", "successResubmit")
}
