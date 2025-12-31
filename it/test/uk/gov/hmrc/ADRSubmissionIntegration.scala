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

import _root_.play.api.Application
import _root_.play.api.inject.guice.GuiceApplicationBuilder
import _root_.play.api.libs.json.JsObject
import _root_.play.api.libs.ws.WSClient
import _root_.play.api.mvc.Result
import _root_.play.api.test.FakeRequest
import _root_.play.api.test.Helpers._
import controllers.SubmissionController
import models.ErsSummary
import org.mongodb.scala.bson.BsonDocument
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import repositories.{MetadataMongoRepository, PresubmissionMongoRepository}
import uk.gov.hmrc.Fixtures.buildErsSummary

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.Future

class ADRSubmissionIntegration extends AnyWordSpecLike with Matchers
  with BeforeAndAfterEach with FakeErsStubService {

  val app: Application = new GuiceApplicationBuilder().configure(
    "microservice.services.ers-stub.port" -> "19339",
    "auditing.enabled" -> false
  ).build()

  def wsClient: WSClient = app.injector.instanceOf[WSClient]

  private lazy val submissionController = app.injector.instanceOf[SubmissionController]
  private lazy val presubmissionRepository = app.injector.instanceOf[PresubmissionMongoRepository]
  private lazy val metadataMongoRepository = app.injector.instanceOf[MetadataMongoRepository]
  val timestamp: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    await(presubmissionRepository.storeJson(
      Fixtures.schemeData(Some(timestamp)),
      ""
    ).value)
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    await(presubmissionRepository.collection.drop().toFuture())
    await(metadataMongoRepository.collection.drop().toFuture())
  }

  "metadataMongoRepository" should {
    "have expected indexes" in {
      val indexNames = metadataMongoRepository.indexes.map(_.getOptions.getName).sorted.toSet
      val expectedIndexes = Seq(
        "metaData.schemeInfo.schemeRef",
        "metaData.schemeInfo.taxYear",
        "metaData.schemeInfo.schemeType",
        "metaData.schemeInfo.timestamp",
        "transferStatus",
        "metaData.schemeInfo.schemeType_transferStatus",
        "confirmationDateTimeToLive"
      ).sorted.toSet

      indexNames should contain allElementsOf expectedIndexes
    }
  }

  def getJson(selector: BsonDocument): Future[Seq[ErsSummary]] =
    metadataMongoRepository.collection.find(
        filter = selector
      ).batchSize(Int.MaxValue)
      .map(_.as[ErsSummary])
      .toFuture()

  //submit-metadata
  "Receiving data for submission" should {
    "return OK if valid metadata is received, filedata is extracted from database and it's successfully sent to ADR" in {
      val ersSummary = buildErsSummary(timestamp = timestamp)
      val schemaInfo = ersSummary.metaData.schemeInfo
      val selector = metadataMongoRepository.buildSelector(schemaInfo)
      val data = Fixtures.buildErsSummaryPayload(ersSummary)

      await(metadataMongoRepository.storeErsSummary(ersSummary, "").value)
      val metadataAfterSave: Seq[ErsSummary] = await(getJson(selector))

      metadataAfterSave.length shouldBe 1
      metadataAfterSave.head.transferStatus.get shouldBe "saved"

      val res: Result = await(submissionController.receiveMetadataJson()
        .apply(FakeRequest().withBody(data.as[JsObject])))
      res.header.status shouldBe OK

      val metadata = await(getJson(selector))
      metadata.length shouldBe 1
      metadata.head.transferStatus.get shouldBe "sent"
    }

    "return OK if valid metadata is received for nil return and it's successfully sent to ADR" in {
      val ersSummary = buildErsSummary(timestamp = timestamp)
      val schemaInfo = ersSummary.metaData.schemeInfo
      val selector = metadataMongoRepository.buildSelector(schemaInfo)
      val data = Fixtures.buildErsSummaryPayload(ersSummary)

      await(metadataMongoRepository.storeErsSummary(ersSummary, "").value)
      val metadataAfterSave = await(getJson(selector))
      metadataAfterSave.length shouldBe 1
      metadataAfterSave.head.transferStatus.get shouldBe "saved"

      val res: Result = await(submissionController.receiveMetadataJson()
        .apply(FakeRequest().withBody(data.as[JsObject])))
      res.header.status shouldBe OK

      val metadata = await(getJson(selector))
      metadata.length shouldBe 1
      metadata.head.transferStatus.get shouldBe "sent"
    }

    "return BAD_REQUEST if invalid request is made" in {
      val response: Result = await(submissionController.receiveMetadataJson()
        .apply(FakeRequest().withBody(Fixtures.invalidPayload.as[JsObject])))
      response.header.status shouldBe BAD_REQUEST
    }
  }

  "Calling /save-metadata" should {

    "return OK and save metadata if valid metadata is sent" in {
      val ersSummary = buildErsSummary(isNilReturn = true)
      val schemaInfo = ersSummary.metaData.schemeInfo
      val selector = metadataMongoRepository.buildSelector(schemaInfo)

      val response: Result = await(submissionController.saveMetadata()
        .apply(FakeRequest().withBody(Fixtures.buildErsSummaryPayload(ersSummary).as[JsObject])))
      response.header.status shouldBe OK
      val result = await(getJson(selector))

      result.length shouldBe 1
      result.head.toString shouldBe ersSummary.toString
    }
  }

  "return BAD_REQUEST if invalid request is made" in {
    val response: Result = await(submissionController.saveMetadata()
      .apply(FakeRequest().withBody(Fixtures.invalidPayload.as[JsObject])))
    response.header.status shouldBe BAD_REQUEST
  }
}
