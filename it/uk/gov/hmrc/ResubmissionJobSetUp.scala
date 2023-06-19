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
import repositories.{MetadataMongoRepository, PresubmissionMongoRepository}
import scheduler.ResubmissionServiceImpl
import _root_.play.api.Application
import _root_.play.api.libs.json.JsObject
import _root_.play.api.test.Helpers._
import _root_.play.api.libs.json._
import models.SchemeData
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.conversions.Bson
import services.resubmission.ProcessFailedSubmissionsConfig

import scala.concurrent.{ExecutionContext, Future}

case class ResubmissionJobSetUp(app: Application) {

  val metadataMongoRepository: MetadataMongoRepository = app.injector.instanceOf[MetadataMongoRepository]
  val collection: MongoCollection[JsObject] = metadataMongoRepository.collection
  val presubmissionMongoRepository: PresubmissionMongoRepository = app.injector.instanceOf[PresubmissionMongoRepository]
  val collectionPs: MongoCollection[JsObject] = presubmissionMongoRepository.collection
  await(collection.drop().toFuture())
  await(collectionPs.drop().toFuture())

  def getJob: ResubmissionServiceImpl = app.injector.instanceOf[ResubmissionServiceImpl]

  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  def countMetadataRecordsWithSelector(filter: Bson): Long = await(
    collection.countDocuments(filter)
      .toFuture()
  )

  def storeMultipleErsSummary(ersSummaries: Seq[JsObject])(implicit ec: ExecutionContext): Future[Boolean] = {
    collection.insertMany(ersSummaries).toFuture().map { res =>
      res.wasAcknowledged()
    }
  }

  def storeMultiplePresubmissionData(presubmissionData: Seq[JsObject])(implicit ec: ExecutionContext): Future[Boolean] = {
    collectionPs.insertMany(presubmissionData).toFuture().map { res =>
      res.wasAcknowledged()
    }
  }

  val processFailedSubmissionsConfig: ProcessFailedSubmissionsConfig =
    getJob.resubmissionService.getProcessFailedSubmissionsConfig(
      app.configuration.get[Int]("schedules.resubmission-service.resubmissionLimit")
    )

  val failedJobSelector: BsonDocument = metadataMongoRepository.createFailedJobSelector(processFailedSubmissionsConfig)

  val successResubmitTransferStatusSelector: Bson = Filters.eq("transferStatus", "successResubmit")
}
