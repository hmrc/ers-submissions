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

package uk.gov.hmrc.missingMetadataQuery

import org.mongodb.scala.{MongoCollection, Observable}
import play.api.libs.json.JsObject
import repositories.{MetadataMongoRepository, PreSubWithoutMetadataQuery, PresubmissionMongoRepository}
import config.ApplicationConfig
import models.{ErsMetaData, ErsSummary, SchemeInfo}
import org.mongodb.scala.bson.Document
import uk.gov.hmrc.mongo.test.MongoSupport

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.duration.{FiniteDuration, DurationLong}
import scala.concurrent.{ExecutionContext, Future}

class MissingMetadataQuerySetup(applicationConfig: ApplicationConfig)(implicit ec: ExecutionContext) extends MongoSupport {

  override def initTimeout: FiniteDuration = 30.seconds

  val metadataMongoRepository: MetadataMongoRepository = new MetadataMongoRepository(applicationConfig, mongoComponent)
  val metadataCollection: MongoCollection[JsObject] = metadataMongoRepository.collection
  val presubmissionMongoRepository: PresubmissionMongoRepository = new PresubmissionMongoRepository(applicationConfig, mongoComponent)
  val presubmissionCollection: MongoCollection[JsObject] = presubmissionMongoRepository.collection

  val preSubWithoutMetadataQuery: PreSubWithoutMetadataQuery = new PreSubWithoutMetadataQuery(presubmissionMongoRepository, applicationConfig)

  def resetCollections(): Observable[Unit] = {
    for {
      _ <- metadataCollection.deleteMany(Document())
      _ <- presubmissionCollection.deleteMany(Document())
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

  def getQueryResult(metaData: Seq[JsObject], presubmissionData: Seq[JsObject]): Future[TestQueryResults] =
    resetCollections()
      .toFuture()
      .flatMap(_ => storeMultipleErsSummary(metaData))
      .flatMap(_ => storeMultiplePresubmissionData(presubmissionData))
      .flatMap(_ => preSubWithoutMetadataQuery.runQuery)
      .flatMap(results => {
        for {
          numMetadataRecords: Long <- countMetadataRecords
          numPreSubRecords: Long <- countPresubmissionRecords
        } yield TestQueryResults(numMetadataRecords, numPreSubRecords, results._2, results._1)
      })

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
