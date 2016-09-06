/*
 * Copyright 2016 HM Revenue & Customs
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
import models.{SchemeInfo, SchemeData}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.internal.verification.VerificationModeFactory
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.{Json, JsObject}
import play.api.test._
import play.api.test.Helpers._
import org.scalatest.mock.MockitoSugar
import services.{ValidationService, PresubmissionService}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}
import fixtures.Fixtures
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.mvc._

class PresubmissionControllerSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with WithFakeApplication {

  val mockErsLoggingAndAuditing: ErsLoggingAndAuditing = mock[ErsLoggingAndAuditing]

  val mockMetrics: Metrics = mock[Metrics]
  override def beforeEach() = {
    super.beforeEach()
    reset(mockMetrics)
    reset(mockErsLoggingAndAuditing)
  }

  val ersSchemeInfo: JsObject = Json.toJson(Fixtures.EMISchemeInfo).as[JsObject]

  "calling removePresubmissionJson" should {

    def buildPresubmissionController(validationResult: Boolean = true, removeJsonResult: Boolean = true): PresubmissionController = new PresubmissionController {

      val mockPresubmissionService = mock[PresubmissionService]
      when(
        mockPresubmissionService.removeJson(any[SchemeInfo])(any[Request[_]](), any[HeaderCarrier]())
      ).thenReturn(
        Future(removeJsonResult)
      )

      override val presubmissionService: PresubmissionService = mockPresubmissionService

      val mockValidationService: ValidationService = mock[ValidationService]
      when(
        mockValidationService.validateSchemeInfo(any[JsObject])
      ).thenReturn(
        validationResult  match {
          case true => Some(mock[SchemeInfo])
          case false => None
        }
      )

      override val validationService: ValidationService = mockValidationService
      override val metrics: Metrics = mockMetrics
      override val ersLoggingAndAuditing: ErsLoggingAndAuditing = mockErsLoggingAndAuditing
    }

    "return BadRequest if invalid json is given" in {
      val presubmissionController = buildPresubmissionController(false, true)
      val result = await(presubmissionController.removePresubmissionJson()(FakeRequest().withBody(ersSchemeInfo)))
      status(result) shouldBe BAD_REQUEST
      verify(mockMetrics, VerificationModeFactory.times(0)).removePresubmission(_, _)
      verify(mockMetrics, VerificationModeFactory.times(0)).failedRemovePresubmission()
    }

    "return InvalidServerError if valid json is given but storage fails" in {
      val presubmissionController = buildPresubmissionController(true, false)
      val result = await(presubmissionController.removePresubmissionJson()(FakeRequest().withBody(ersSchemeInfo)))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      verify(mockMetrics, VerificationModeFactory.times(0)).removePresubmission(_, _)
      verify(mockMetrics, VerificationModeFactory.times(1)).failedRemovePresubmission()
    }

    "return OK if valid json is given and storage succeeds" in {
      val presubmissionController = buildPresubmissionController(true, true)
      val result = await(presubmissionController.removePresubmissionJson()(FakeRequest().withBody(ersSchemeInfo)))
      status(result) shouldBe OK
      verify(mockMetrics, VerificationModeFactory.times(1)).removePresubmission(_, _)
      verify(mockMetrics, VerificationModeFactory.times(0)).failedRemovePresubmission()
    }

  }

  "calling checkForExistingPresubmission" should {

    def buildPresubmissionController(validationResult: Boolean = true, checkResult: Boolean = true): PresubmissionController = new PresubmissionController {

      val mockPresubmissionService = mock[PresubmissionService]
      when(
        mockPresubmissionService.compareSheetsNumber(anyInt(), any[SchemeInfo])(any[Request[_]](), any[HeaderCarrier]())
      ).thenReturn(
        Future(checkResult)
      )

      override val presubmissionService: PresubmissionService = mockPresubmissionService

      val mockValidationService: ValidationService = mock[ValidationService]
      when(
        mockValidationService.validateSchemeInfo(any[JsObject])
      ).thenReturn(
        validationResult  match {
          case true => Some(mock[SchemeInfo])
          case false => None
        }
      )

      override val validationService: ValidationService = mockValidationService
      override val metrics: Metrics = mockMetrics
      override val ersLoggingAndAuditing: ErsLoggingAndAuditing = mockErsLoggingAndAuditing
    }

    "return BadRequest if invalid json is given" in {
      val presubmissionController = buildPresubmissionController(false, true)
      val result = await(presubmissionController.checkForExistingPresubmission(5)(FakeRequest().withBody(ersSchemeInfo)))
      status(result) shouldBe BAD_REQUEST
      verify(mockMetrics, VerificationModeFactory.times(0)).checkForPresubmission(anyLong(), any[TimeUnit]())
    }

    "return InvalidServerError if valid json is given but not all sheets are found" in {
      val presubmissionController = buildPresubmissionController(true, false)
      val result = await(presubmissionController.checkForExistingPresubmission(5)(FakeRequest().withBody(ersSchemeInfo)))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      verify(mockMetrics, VerificationModeFactory.times(0)).checkForPresubmission(anyLong(), any[TimeUnit]())
    }

    "return Ok if valid json is given and all sheets are found" in {
      val presubmissionController = buildPresubmissionController(true, true)
      val result = await(presubmissionController.checkForExistingPresubmission(5)(FakeRequest().withBody(ersSchemeInfo)))
      status(result) shouldBe OK
      verify(mockMetrics, VerificationModeFactory.times(1)).checkForPresubmission(anyLong(), any[TimeUnit]())
    }
  }

}
