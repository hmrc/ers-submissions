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

import _root_.play.api.libs.json.JsObject
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import repositories.{MetadataMongoRepository, PresubmissionMongoRepository}
import controllers.SubmissionController

import _root_.play.api.test.Helpers._
import _root_.play.api.Application
import _root_.play.api.inject.guice.GuiceApplicationBuilder
import _root_.play.api.libs.ws.WSClient
import _root_.play.api.test.FakeRequest
import _root_.play.api.mvc.Result
import uk.gov.hmrc.play.http.ws.WSRequest

class ADRSubmissionIntegration extends AnyWordSpecLike with Matchers
 with BeforeAndAfterEach with WSRequest with FakeErsStubService {

  val app: Application = new GuiceApplicationBuilder().configure(Map("microservice.services.ers-stub.port" -> "19339")).build()
  def wsClient: WSClient = app.injector.instanceOf[WSClient]

  private lazy val submissionController = app.injector.instanceOf[SubmissionController]
  private lazy val presubmissionRepository = app.injector.instanceOf[PresubmissionMongoRepository]
  private lazy val metadataMongoRepository = app.injector.instanceOf[MetadataMongoRepository]

  override protected def beforeEach: Unit = {
    super.beforeEach()
    await(presubmissionRepository.storeJsonV2(
      Fixtures.submissionsSchemeData.schemeInfo.toString,
      Fixtures.schemeData
    ))
  }

  override protected def afterEach: Unit = {
    super.afterEach
    await(presubmissionRepository.collection.drop.toFuture)
    await(metadataMongoRepository.collection.drop.toFuture)
  }

  //submit-metadata
  "Receiving data for submission" should {

    "return OK if valid metadata is received, filedata is extracted from database and it's successfully sent to ADR" in {
      val data = Fixtures.buildErsSummaryPayload(false)

      await(metadataMongoRepository.storeErsSummary(Fixtures.buildErsSummary(false)))
      val test = Fixtures.schemeInfo
      val metadataAfterSave = await(metadataMongoRepository.getJson(test))

      metadataAfterSave.length shouldBe 1
      metadataAfterSave.head.transferStatus.get shouldBe "saved"

      val res: Result = await(submissionController.receiveMetadataJson()
        .apply(FakeRequest().withBody(data.as[JsObject])))
      res.header.status shouldBe OK

      val metadata = await(metadataMongoRepository.getJson(Fixtures.schemeInfo))
      metadata.length shouldBe 1
      metadata.head.transferStatus.get shouldBe "sent"
    }

    "return OK if valid metadata is received for nil return and it's successfully sent to ADR" in {
      val data = Fixtures.buildErsSummaryPayload(true)

      await(metadataMongoRepository.storeErsSummary(Fixtures.buildErsSummary(true)))
      val metadataAfterSave = await(metadataMongoRepository.getJson(Fixtures.schemeInfo))
      metadataAfterSave.length shouldBe 1
      metadataAfterSave.head.transferStatus.get shouldBe "saved"

      val res: Result = await(submissionController.receiveMetadataJson()
        .apply(FakeRequest().withBody(data.as[JsObject])))
      res.header.status shouldBe OK

      val metadata = await(metadataMongoRepository.getJson(Fixtures.schemeInfo))
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

      val response: Result = await(submissionController.saveMetadata()
        .apply(FakeRequest().withBody(Fixtures.buildErsSummaryPayload( true).as[JsObject])))
      response.header.status shouldBe OK
      val result = await(metadataMongoRepository.getJson(Fixtures.schemeInfo))

      result.length shouldBe 1
      result.head.toString shouldBe Fixtures.buildErsSummary(true).toString
    }
  }

    "return BAD_REQUEST if invalid request is made" in {
      val response: Result = await(submissionController.saveMetadata()
        .apply(FakeRequest().withBody(Fixtures.invalidPayload.as[JsObject])))
      response.header.status shouldBe BAD_REQUEST
    }

}
