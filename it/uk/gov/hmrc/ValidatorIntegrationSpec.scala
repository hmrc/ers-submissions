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
import _root_.play.api.libs.json.{JsObject, Json}
import _root_.play.api.libs.ws.WSClient
import _root_.play.api.test.FakeRequest
import _root_.play.api.test.Helpers._
import controllers.{PresubmissionController, ReceivePresubmissionController}
import models.SubmissionsSchemeData
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import repositories.PresubmissionMongoRepository

class ValidatorIntegrationSpec extends AnyWordSpecLike with Matchers
  with BeforeAndAfterEach with FakeAuthService with EitherValues {

  lazy val app: Application = new GuiceApplicationBuilder().configure(
    Map("microservice.services.auth.port" -> "18500",
      "auditing.enabled" -> false
    )
  ).build()

  def wsClient: WSClient = app.injector.instanceOf[WSClient]

  private lazy val presubmissionRepository = app.injector.instanceOf[PresubmissionMongoRepository]

  val receivePresubmissionController: ReceivePresubmissionController = app.injector.instanceOf[ReceivePresubmissionController]

  val presubmissionController: PresubmissionController = app.injector.instanceOf[PresubmissionController]

  override protected def afterEach(): Unit = {
    super.afterEach()
    await(presubmissionRepository.collection.drop().toFuture())
  }

  // /submit-presubmission
  "Sending data from validator" should {

    "return BAD_REQUEST if invalid object is sent" in {
      val response = await(receivePresubmissionController.receivePresubmissionJsonV2("ABC%2F1234")
        .apply(FakeRequest().withBody(Json.parse("{ \"key\": \"value\" }"))
          .withHeaders(("Authorization", "Bearer123"))))
      response.header.status shouldBe BAD_REQUEST
    }

    "be stored successfully in database" in {
      val submissionsSchemeData: SubmissionsSchemeData = Fixtures.submissionsSchemeData
      val response = await(receivePresubmissionController.receivePresubmissionJsonV2("ABC%2F1234")
        .apply(FakeRequest().withBody(Fixtures.submissionsSchemeDataJson(submissionsSchemeData))
          .withHeaders(("Authorization", "Bearer123"))))
      response.header.status shouldBe OK

      val presubmissionData = await(presubmissionRepository.getJson(submissionsSchemeData.schemeInfo, "").value).value
      presubmissionData.length shouldBe 1
      presubmissionData.head.equals(Fixtures.schemeData)
    }

  }

  // /remove-presubmission
  "Removing data" should {

    "successfully remove data by session Id and scheme ref" in {
      val submissionsSchemeData: SubmissionsSchemeData = Fixtures.submissionsSchemeData
      val schemeInfo = submissionsSchemeData.schemeInfo
      val response = await(receivePresubmissionController.receivePresubmissionJsonV2("ABC%2F1234")
        .apply(FakeRequest().withBody(Fixtures.submissionsSchemeDataJson(submissionsSchemeData))
          .withHeaders(("Authorization", "Bearer123"))))
      
      response.header.status shouldBe OK

      val presubmissionData = await(presubmissionRepository.getJson(submissionsSchemeData.schemeInfo, "").value).value
      presubmissionData.length shouldBe 1
      presubmissionData.head.equals(Fixtures.schemeData)

      val removeResponse = await(presubmissionController.removePresubmissionJson()
        .apply(FakeRequest().withBody(Fixtures.schemeInfoPayload(schemeInfo).as[JsObject])))
      removeResponse.header.status shouldBe OK

      val presubmissionDataAfterRemove = await(presubmissionRepository.getJson(Fixtures.schemeInfo(), "").value).value
      presubmissionDataAfterRemove.length shouldBe 0
    }

  }

  // /check-for-presubmission/:validatedSheets
  "Checking for received presubmission data" should {

    "return OK if expected records are equal to existing ones" in {
      val submissionsSchemeData: SubmissionsSchemeData = Fixtures.submissionsSchemeData
      val schemeInfo = submissionsSchemeData.schemeInfo
      val response = await(receivePresubmissionController.receivePresubmissionJsonV2("ABC%2F1234")
        .apply(FakeRequest().withBody(Fixtures.submissionsSchemeDataJson(submissionsSchemeData))
          .withHeaders(("Authorization", "Bearer123"))))

      response.header.status shouldBe OK
      val presubmissionData = await(presubmissionRepository.getJson(schemeInfo, "").value).value
      presubmissionData.length shouldBe 1
      presubmissionData.head.equals(Fixtures.schemeData)

      val checkResponse = await(presubmissionController.checkForExistingPresubmission(1)
        .apply(FakeRequest().withBody(Fixtures.schemeInfoPayload(schemeInfo).as[JsObject])))

      checkResponse.header.status shouldBe OK
    }

    "return InternalServerError if expected records are not equal to existing ones" in {
      val submissionsSchemeData: SubmissionsSchemeData = Fixtures.submissionsSchemeData
      val schemeInfo = submissionsSchemeData.schemeInfo
      val response = await(receivePresubmissionController.receivePresubmissionJsonV2("ABC%2F1234")
        .apply(FakeRequest().withBody(Fixtures.submissionsSchemeDataJson(submissionsSchemeData))
        .withHeaders(("Authorization", "Bearer123"))))

      response.header.status shouldBe OK
      val presubmissionData = await(presubmissionRepository.getJson(schemeInfo, "").value).value
      presubmissionData.length shouldBe 1
      presubmissionData.head.equals(Fixtures.schemeData)

      val checkResponse = await(presubmissionController.checkForExistingPresubmission(2)
        .apply(FakeRequest().withBody(Fixtures.schemeInfoPayload(schemeInfo).as[JsObject])))
      checkResponse.header.status shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
