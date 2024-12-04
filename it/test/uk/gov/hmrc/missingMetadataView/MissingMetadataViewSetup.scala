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

package uk.gov.hmrc.missingMetadataView

import org.mongodb.scala.{MongoCollection, Observable}
import play.api.libs.json.JsObject
import repositories.{MetadataMongoRepository, PreSubWithoutMetadataView, PresubmissionMongoRepository}
import config.ApplicationConfig
import models.{ErsMetaData, ErsSummary, PreSubWithoutMetadata, SchemeInfo}
import org.mongodb.scala.bson.Document
import uk.gov.hmrc.mongo.MongoComponent

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}

class MissingMetadataViewSetup(applicationConfig: ApplicationConfig, mc: MongoComponent)(implicit ec: ExecutionContext){

  val metadataMongoRepository: MetadataMongoRepository = new MetadataMongoRepository(applicationConfig, mc)
  val metadataCollection: MongoCollection[JsObject] = metadataMongoRepository.collection
  val presubmissionMongoRepository: PresubmissionMongoRepository = new PresubmissionMongoRepository(applicationConfig, mc)
  val presubmissionCollection: MongoCollection[JsObject] = presubmissionMongoRepository.collection

  val preSubWithoutMetadataView: PreSubWithoutMetadataView = new PreSubWithoutMetadataView(mc, applicationConfig)

  def resetCollections(): Observable[Unit] = {
    for {
      clearMetaData <- metadataCollection.deleteMany(Document())
      clearPresubmissionData <- presubmissionCollection.deleteMany(Document())
    } yield ()
  }

  def storeMultipleErsSummary(ersSummaries: Seq[JsObject])(implicit ec: ExecutionContext): Future[Boolean] = {
    metadataCollection.insertMany(ersSummaries).toFuture().map { res =>
      res.wasAcknowledged()
    }
  }

  def countMetadataRecords: Future[Long] =
    metadataCollection.countDocuments()
      .toFuture()

  def countPresubmissionRecords: Future[Long] =
    presubmissionCollection.countDocuments()
      .toFuture()

  def storeMultiplePresubmissionData(presubmissionData: Seq[JsObject])(implicit ec: ExecutionContext): Future[Boolean] = {
    presubmissionCollection.insertMany(presubmissionData).toFuture().map { res =>
      res.wasAcknowledged()
    }
  }

  def getViewResult(metaData: Seq[JsObject], presubmissionData: Seq[JsObject]): Future[(Long, Long, Seq[PreSubWithoutMetadata])] =
    resetCollections()
      .toFuture()
      .flatMap(_ => storeMultipleErsSummary(metaData))
      .flatMap(_ => storeMultiplePresubmissionData(presubmissionData))
      .flatMap(_ => preSubWithoutMetadataView.initView)
      .flatMap((view: MongoCollection[PreSubWithoutMetadata]) =>
        for {
          metadataCount <- countMetadataRecords
          preSubCount <- countPresubmissionRecords
          viewRecords <- view.find().toFuture()
        } yield (metadataCount, preSubCount, viewRecords)
      )

  def schemeInfo(taxYear: String,
                 schemeRef: String,
                 timestamp: Instant): SchemeInfo = SchemeInfo(
    schemeRef = schemeRef,
    timestamp = timestamp,
    schemeId = "123PA12345678",
    taxYear = taxYear,
    schemeName = "EMI",
    schemeType = "EMI"
  )

  def ersMetaData(taxYear: String,
                  schemeRef: String,
                  timestamp: Instant): ErsMetaData = ErsMetaData(
    schemeInfo = schemeInfo(taxYear, schemeRef, timestamp),
    ipRef = "127.0.0.0",
    aoRef = Some("123PA12345678"),
    empRef = "EMI - MyScheme - XA1100000000000 - 2014/15",
    agentRef = None,
    sapNumber = Some("sap-123456")
  )

  def createMetadataRecord(taxYear: String,
                           schemeRef: String = "XA1100000000000",
                           timestamp: Instant): ErsSummary = ErsSummary(
    bundleRef = "testbundle",
    isNilReturn = "2",
    fileType = None,
    confirmationDateTime = Instant.now().truncatedTo(ChronoUnit.MILLIS),
    metaData = ersMetaData(taxYear, schemeRef, timestamp),
    altAmendsActivity = None,
    alterationAmends = None,
    groupService = None,
    schemeOrganiser = None,
    companies = None,
    trustees = None,
    nofOfRows = None,
    transferStatus = Some("saved")
  )


}
