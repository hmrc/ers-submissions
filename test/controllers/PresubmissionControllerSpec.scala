/*
 * Copyright 2021 HM Revenue & Customs
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

import java.util.concurrent.TimeUnit

import metrics.Metrics
import models.SchemeInfo
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.internal.verification.VerificationModeFactory
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.{JsObject, Json}
import play.api.test._
import play.api.test.Helpers._
import services.{PresubmissionService, ValidationService}
import fixtures.Fixtures
import helpers.ERSTestHelper
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class PresubmissionControllerSpec extends ERSTestHelper with BeforeAndAfterEach {

  val mockMetrics: Metrics = mock[Metrics]
  val mockPresubmissionService: PresubmissionService = mock[PresubmissionService]
  val mockValidationService: ValidationService = mock[ValidationService]
  val mockErsLoggingAndAuditing: ErsLoggingAndAuditing = mock[ErsLoggingAndAuditing]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMetrics)
    reset(mockErsLoggingAndAuditing)
    reset(mockPresubmissionService)
    reset(mockValidationService)
  }

  val ersSchemeInfo: JsObject = Json.toJson(Fixtures.EMISchemeInfo).as[JsObject]

  "calling removePresubmissionJson" should {

    def buildPresubmissionController(validationResult: Boolean = true, removeJsonResult: Boolean = true): PresubmissionController = {
      new PresubmissionController(mockPresubmissionService, mockValidationService, mockErsLoggingAndAuditing, mockMetrics, mockCc ) {
        when(mockPresubmissionService.removeJson(any[SchemeInfo])(any[HeaderCarrier]()))
          .thenReturn(Future(removeJsonResult))
        when(mockValidationService.validateSchemeInfo(any[JsObject]))
          .thenReturn(if (validationResult) Some(mock[SchemeInfo]) else None)
      }
    }

    "return BadRequest if invalid json is given" in {
      val presubmissionController = buildPresubmissionController(validationResult = false)
      val result = presubmissionController.removePresubmissionJson()(FakeRequest().withBody(ersSchemeInfo))
      status(result) shouldBe BAD_REQUEST
      verify(mockMetrics, VerificationModeFactory.times(0)).removePresubmission(_, _)
      verify(mockMetrics, VerificationModeFactory.times(0)).failedRemovePresubmission()
    }

    "return InvalidServerError if valid json is given but storage fails" in {
      val presubmissionController = buildPresubmissionController(removeJsonResult = false)
      val result = presubmissionController.removePresubmissionJson()(FakeRequest().withBody(ersSchemeInfo))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      verify(mockMetrics, VerificationModeFactory.times(0)).removePresubmission(_, _)
      verify(mockMetrics, VerificationModeFactory.times(1)).failedRemovePresubmission()
    }

    "return OK if valid json is given and storage succeeds" in {
      val presubmissionController = buildPresubmissionController()
      val result = presubmissionController.removePresubmissionJson()(FakeRequest().withBody(ersSchemeInfo))
      status(result) shouldBe OK
      verify(mockMetrics, VerificationModeFactory.times(1)).removePresubmission(_, _)
      verify(mockMetrics, VerificationModeFactory.times(0)).failedRemovePresubmission()
    }

  }

  "calling checkForExistingPresubmission" should {

    def buildPresubmissionController(validationResult: Boolean = true, checkResult: Boolean = true): PresubmissionController =
      new PresubmissionController(mockPresubmissionService, mockValidationService, mockErsLoggingAndAuditing, mockMetrics, mockCc) {
      when(mockPresubmissionService.compareSheetsNumber(anyInt(), any[SchemeInfo])(any[HeaderCarrier]()))
        .thenReturn(Future(checkResult))
      when(mockValidationService.validateSchemeInfo(any[JsObject]))
        .thenReturn(if (validationResult) Some(mock[SchemeInfo]) else None)
    }

    "return BadRequest if invalid json is given" in {
      val presubmissionController = buildPresubmissionController(validationResult = false)
      val result = presubmissionController.checkForExistingPresubmission(5)(FakeRequest().withBody(ersSchemeInfo))
      status(result) shouldBe BAD_REQUEST
      verify(mockMetrics, VerificationModeFactory.times(0)).checkForPresubmission(anyLong(), any[TimeUnit]())
    }

    "return InvalidServerError if valid json is given but not all sheets are found" in {
      val presubmissionController = buildPresubmissionController(checkResult = false)
      val result = presubmissionController.checkForExistingPresubmission(5)(FakeRequest().withBody(ersSchemeInfo))
      status(result) shouldBe INTERNAL_SERVER_ERROR
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
