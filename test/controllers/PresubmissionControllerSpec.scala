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

package controllers

import common.ERSEnvelope
import common.ERSEnvelope.ERSEnvelope
import fixtures.Fixtures
import helpers.ERSTestHelper
import metrics.Metrics
import models.{MongoUnavailableError, NoData, SchemeInfo}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.internal.verification.VerificationModeFactory
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import play.api.test._
import services.PresubmissionService
import services.audit.AuditEvents
import uk.gov.hmrc.http.HeaderCarrier

import java.util.concurrent.TimeUnit

class PresubmissionControllerSpec extends ERSTestHelper with BeforeAndAfterEach {

  val mockMetrics: Metrics = mock[Metrics]
  val mockPresubmissionService: PresubmissionService = mock[PresubmissionService]
  val mockAuditEvents: AuditEvents = mock[AuditEvents]
  implicit val hc: HeaderCarrier = new HeaderCarrier()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMetrics)
    reset(mockAuditEvents)
    reset(mockPresubmissionService)
  }

  val ersSchemeInfo: JsObject = Json.toJson(Fixtures.EMISchemeInfo).as[JsObject]

  "calling removePresubmissionJson" should {
    def buildPresubmissionController(removeJsonResult: ERSEnvelope[Boolean] = ERSEnvelope(true)): PresubmissionController = {
      new PresubmissionController(mockPresubmissionService, mockAuditEvents, mockMetrics, mockCc) {
        when(mockPresubmissionService.removeJson(any[SchemeInfo])(any[HeaderCarrier]()))
          .thenReturn(removeJsonResult)
      }
    }

    "return BadRequest if invalid json is given" in {
      val presubmissionController = buildPresubmissionController()
      val result = presubmissionController.removePresubmissionJson()(FakeRequest().withBody(ersSchemeInfo - "schemeRef"))
      status(result) shouldBe BAD_REQUEST
      verify(mockMetrics, VerificationModeFactory.times(0)).removePresubmission(anyLong(), any())
      verify(mockMetrics, VerificationModeFactory.times(0)).failedRemovePresubmission()
    }

    "return InvalidServerError if valid json is given but removing fails" in {
      val presubmissionController = buildPresubmissionController(removeJsonResult = ERSEnvelope(false))
      val result = presubmissionController.removePresubmissionJson()(FakeRequest().withBody(ersSchemeInfo))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      verify(mockMetrics, VerificationModeFactory.times(0)).removePresubmission(anyLong(), any())
      verify(mockMetrics, VerificationModeFactory.times(1)).failedRemovePresubmission()
    }

    "return Ok if service returns NoData()" in {
      val presubmissionController = buildPresubmissionController(removeJsonResult = ERSEnvelope(NoData()))
      val result = presubmissionController.removePresubmissionJson()(FakeRequest().withBody(ersSchemeInfo))
      status(result) shouldBe OK
      verify(mockMetrics, VerificationModeFactory.times(0)).removePresubmission(anyLong(), any())
      verify(mockMetrics, VerificationModeFactory.times(0)).failedRemovePresubmission()
    }

    "return INTERNAL_SERVER_ERROR if service returns error different than NoData()" in {
      val presubmissionController = buildPresubmissionController(removeJsonResult = ERSEnvelope(MongoUnavailableError("mongo error")))
      val result = presubmissionController.removePresubmissionJson()(FakeRequest().withBody(ersSchemeInfo))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      verify(mockMetrics, VerificationModeFactory.times(0)).removePresubmission(anyLong(), any())
      verify(mockMetrics, VerificationModeFactory.times(1)).failedRemovePresubmission()
    }

    "return OK if valid json is given and remove succeeds" in {
      val presubmissionController = buildPresubmissionController()
      val result = presubmissionController.removePresubmissionJson()(FakeRequest().withBody(ersSchemeInfo))
      status(result) shouldBe OK
      verify(mockMetrics, VerificationModeFactory.times(1)).removePresubmission(anyLong(), any())
      verify(mockMetrics, VerificationModeFactory.times(0)).failedRemovePresubmission()
    }

  }

  "calling checkForExistingPresubmission" should {
    def buildPresubmissionController(checkResult: Option[(Boolean, Long)] = Some((true, 1L))): PresubmissionController =
      new PresubmissionController(mockPresubmissionService, mockAuditEvents, mockMetrics, mockCc) {
      when(mockPresubmissionService.compareSheetsNumber(anyInt(), any[SchemeInfo])(any[HeaderCarrier]()))
        .thenReturn(ERSEnvelope(checkResult.toRight(MongoUnavailableError("mongo error"))))
    }

    "return BadRequest if invalid json is given" in {
      val presubmissionController = buildPresubmissionController()
      val result = presubmissionController.checkForExistingPresubmission(5)(FakeRequest().withBody(ersSchemeInfo - "schemeRef"))
      status(result) shouldBe BAD_REQUEST
      verify(mockMetrics, VerificationModeFactory.times(0)).checkForPresubmission(anyLong(), any[TimeUnit]())
    }

    "return InvalidServerError if valid json is given but not all sheets are found" in {
      val presubmissionController = buildPresubmissionController(checkResult = Some((false, 0)))
      val result = presubmissionController.checkForExistingPresubmission(5)(FakeRequest().withBody(ersSchemeInfo))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      verify(mockAuditEvents, VerificationModeFactory.times(1)).auditADRTransferFailure(any(), any())(any())
      verify(mockMetrics, VerificationModeFactory.times(0)).checkForPresubmission(anyLong(), any[TimeUnit]())
    }

    "return InvalidServerError if valid json is given but service returns error" in {
      val presubmissionController = buildPresubmissionController(checkResult = None)
      val result = presubmissionController.checkForExistingPresubmission(5)(FakeRequest().withBody(ersSchemeInfo))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      verify(mockAuditEvents, VerificationModeFactory.times(1)).auditADRTransferFailure(any(), any())(any())
      verify(mockMetrics, VerificationModeFactory.times(0)).checkForPresubmission(anyLong(), any[TimeUnit]())
    }

    "return Ok if valid json is given and all sheets are found" in {
      val presubmissionController = buildPresubmissionController()
      val result = presubmissionController.checkForExistingPresubmission(5)(FakeRequest().withBody(ersSchemeInfo))
      status(result) shouldBe OK
      verify(mockMetrics, VerificationModeFactory.times(1)).checkForPresubmission(anyLong(), any[TimeUnit]())
    }
  }
}
