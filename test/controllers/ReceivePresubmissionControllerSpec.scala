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

import controllers.auth.AuthAction
import fixtures.{Fixtures, WithMockedAuthActions}
import metrics.Metrics
import models.SchemeData
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.test.Helpers._
import org.mockito.internal.verification.VerificationModeFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.JsObject
import play.api.mvc.{ControllerComponents, PlayBodyParsers, Request}
import play.api.test.FakeRequest
import services.audit.AuditEvents
import services.{PresubmissionService, ValidationService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier

class ReceivePresubmissionControllerSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with GuiceOneAppPerSuite with WithMockedAuthActions{

  val mockAuthAction: AuthAction = mock[AuthAction]
  val mockPresubmissionService: PresubmissionService = mock[PresubmissionService]
  val mockValidationService: ValidationService = mock[ValidationService]
  val mockErsLoggingAndAuditing: ErsLoggingAndAuditing = mock[ErsLoggingAndAuditing]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockAuditEvents: AuditEvents = mock[AuditEvents]
  val mockCc: ControllerComponents = stubControllerComponents()
  val mockBodyParser: PlayBodyParsers = mock[PlayBodyParsers]

  val mockMetrics: Metrics = mock[Metrics]
  override def beforeEach() = {
    super.beforeEach()
    reset(mockMetrics)
    reset(mockErsLoggingAndAuditing)
    reset(mockAuthAction)
    reset(mockValidationService)
    reset(mockAuthConnector)
    reset(mockAuditEvents)
  }

  "calling receivePresubmissionJson" should {

    def buildPresubmissionController(validationResult: Boolean = true, storeJsonResult: Boolean = true): ReceivePresubmissionController =
      new ReceivePresubmissionController(
        mockPresubmissionService,
        mockValidationService,
        mockErsLoggingAndAuditing,
        mockAuthConnector,
        mockAuditEvents,
        mockMetrics,
        mockCc,
        mockBodyParser
      ) {
        mockJsValueAuthAction

      when(mockPresubmissionService.storeJson(any[SchemeData])(any[Request[_]](), any[HeaderCarrier]()))
        .thenReturn(Future(storeJsonResult))
      when(mockValidationService.validateSchemeData(any[JsObject]))
        .thenReturn(if (validationResult) Some(Fixtures.schemeData) else None)

      override def authorisedAction(empRef: String): AuthAction = mockAuthAction
    }

    "return BadRequest if invalid json is given" in {
      val presubmissionController = buildPresubmissionController(validationResult = false)
      val result = await(presubmissionController.receivePresubmissionJson("")(FakeRequest().withBody(Fixtures.schemeDataJson)))
      status(result) shouldBe BAD_REQUEST
      verify(mockMetrics, VerificationModeFactory.times(0)).storePresubmission(_, _)
      verify(mockMetrics, VerificationModeFactory.times(0)).failedStorePresubmission()
    }

    "return InvalidServerError if valid json is given but storage fails" in {
      val presubmissionController = buildPresubmissionController(storeJsonResult = false)
      val result = await(presubmissionController.receivePresubmissionJson("")(FakeRequest().withBody(Fixtures.schemeDataJson)))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      verify(mockMetrics, VerificationModeFactory.times(0)).storePresubmission(_, _)
      verify(mockMetrics, VerificationModeFactory.times(1)).failedStorePresubmission()
    }

    "return OK if valid json is given and storage succeeds" in {
      val presubmissionController = buildPresubmissionController(validationResult = true, storeJsonResult = true)
      val result = await(presubmissionController.receivePresubmissionJson("")(FakeRequest().withBody(Fixtures.schemeDataJson)))
      status(result) shouldBe OK
      verify(mockMetrics, VerificationModeFactory.times(1)).storePresubmission(_, _)
      verify(mockMetrics, VerificationModeFactory.times(0)).failedStorePresubmission()
    }

  }
}
