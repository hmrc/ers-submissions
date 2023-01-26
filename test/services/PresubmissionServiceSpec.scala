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

package services

import fixtures.{Fixtures, SIP}
import helpers.ERSTestHelper
import models.{SchemeData, SchemeInfo, SubmissionsSchemeData, UpscanCallback}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import repositories.{PresubmissionMongoRepository, Repositories}
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing

import scala.concurrent.{ExecutionContext, Future}

class PresubmissionServiceSpec extends ERSTestHelper {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[JsObject] = FakeRequest().withBody(Fixtures.metadataJson)
  val mockRepositories: Repositories = mock[Repositories]
  val mockErsLoggingAndAuditing: ErsLoggingAndAuditing = mock[ErsLoggingAndAuditing]
  val mockPresubmissionRepository: PresubmissionMongoRepository = mock[PresubmissionMongoRepository]

  def buildPresubmissionService(storeJsonResult: Option[Boolean] = Some(true),
                                getJsonResult: Boolean = true,
                                removeJsonResult: Option[Boolean] = Some(true))
                               (implicit ec: ExecutionContext): PresubmissionService =
    new PresubmissionService(mockRepositories, mockErsLoggingAndAuditing) {

      override lazy val presubmissionRepository: PresubmissionMongoRepository = mockPresubmissionRepository
      when(mockPresubmissionRepository.storeJson(any[SchemeData])).thenReturn(
        if (storeJsonResult.isDefined) Future(storeJsonResult.get) else Future.failed(new RuntimeException))
      when(mockPresubmissionRepository.storeJsonV2(any[String], any[SchemeData])).thenReturn(
        if (storeJsonResult.isDefined) Future(storeJsonResult.get) else Future.failed(new RuntimeException("here's a message")))
      when(mockPresubmissionRepository.getJson(any[SchemeInfo]))
        .thenReturn(Future(if (getJsonResult) List(Fixtures.schemeData) else List()))
      when(mockPresubmissionRepository.removeJson(any[SchemeInfo]))
        .thenReturn(if (removeJsonResult.isDefined) Future(removeJsonResult.get) else Future.failed(new RuntimeException))
    }

  "calling storeJson" should {

    "return true if storage is successful" in {
      val presubmissionService = buildPresubmissionService(Some(true))
      val result = await(presubmissionService.storeJson(Fixtures.schemeData))
      result shouldBe true
    }

    "return false if storage fails" in {
      val presubmissionService = buildPresubmissionService(Some(false))
      val result = await(presubmissionService.storeJson(Fixtures.schemeData))
      result shouldBe false
    }

    "return false if exception" in {
      val presubmissionService = buildPresubmissionService(None)
      val result = await(presubmissionService.storeJson(Fixtures.schemeData))
      result shouldBe false
    }
  }

  "calling storeJsonV2" should {
    val submissionsSchemeData: SubmissionsSchemeData = SubmissionsSchemeData(SIP.schemeInfo, "sip sheet name",
      UpscanCallback("name", "/download/url"), 1)

    val testSchemeData: SchemeData = SchemeData(SIP.schemeInfo, "sip sheet name", None, None)

    "return true if storage is successful" in {
      val presubmissionService = buildPresubmissionService(Some(true))
      val result = await(presubmissionService.storeJsonV2(submissionsSchemeData, testSchemeData))
      result shouldBe true
    }

    "return false if storage fails" in {
      val presubmissionService = buildPresubmissionService(Some(false))
      val result = await(presubmissionService.storeJsonV2(submissionsSchemeData, testSchemeData))
      result shouldBe false
    }

    "return false if exception" in {
      val presubmissionService = buildPresubmissionService(None)
      val result = await(presubmissionService.storeJsonV2(submissionsSchemeData, testSchemeData))
      result shouldBe false
    }
  }

  "calling getJson" should {

    "return List[SchemeData] if finding succeeds" in {
      val presubmissionService = buildPresubmissionService(Some(true))
      val result = await(presubmissionService.getJson(Fixtures.EMISchemeInfo))
      result.isEmpty shouldBe false
    }

    "return empty list if finding fails" in {
      val presubmissionService = buildPresubmissionService(Some(true), getJsonResult = false)
      val result = await(presubmissionService.getJson(Fixtures.EMISchemeInfo))
      result.isEmpty shouldBe true
    }
  }

  "calling removeJson" should {

    "return true if removing is sussessful" in {
      val presubmissionService = buildPresubmissionService(Some(true), getJsonResult = true, Some(true))
      val result = await(presubmissionService.removeJson(Fixtures.EMISchemeInfo))
      result shouldBe true
    }

    "return false if removing fails" in {
      val presubmissionService = buildPresubmissionService(Some(true), getJsonResult = true, Some(false))
      val result = await(presubmissionService.removeJson(Fixtures.EMISchemeInfo))
      result shouldBe false
    }

    "return false if exception" in {
      val presubmissionService = buildPresubmissionService(Some(true), getJsonResult = true, None)
      val result = await(presubmissionService.removeJson(Fixtures.EMISchemeInfo))
      result shouldBe false
    }
  }

  "calling compareSheetsNumber" should {

    def buildPresubmissionService(foundSheets: Option[Int]): PresubmissionService = new PresubmissionService(mockRepositories, mockErsLoggingAndAuditing) {
      override lazy val presubmissionRepository = mockPresubmissionRepository
      when(mockPresubmissionRepository.count(any[SchemeInfo]()))
        .thenReturn(if (foundSheets.isDefined) Future.successful(foundSheets.get.toLong) else Future.failed(new RuntimeException))
    }

    "return true if expected number of sheets is equal to found ones" in {
      val presubmissionService = buildPresubmissionService(Some(1))
      val result = await(presubmissionService.compareSheetsNumber(expectedSheets = 1, schemeInfo = Fixtures.EMISchemeInfo))
      result shouldBe true
    }

    "return false if expected number of sheets is not equal to found ones" in {
      val presubmissionService = buildPresubmissionService(Some(-1))
      val result = await(presubmissionService.compareSheetsNumber(expectedSheets = 1, schemeInfo = Fixtures.EMISchemeInfo))
      result shouldBe false
    }

    "return false if exception is thrown" in {
      val presubmissionService = buildPresubmissionService(None)
      val result = await(presubmissionService.compareSheetsNumber(expectedSheets = 1, schemeInfo = Fixtures.EMISchemeInfo))
      result shouldBe false
    }
  }

}
