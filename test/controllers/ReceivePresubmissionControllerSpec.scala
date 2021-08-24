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

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.Source
import akka.testkit.TestKit
import akka.util.ByteString
import config.ApplicationConfig
import controllers.auth.AuthAction
import fixtures.{Fixtures, SIP, WithMockedAuthActions}
import helpers.ERSTestHelper
import metrics.Metrics
import models.{SchemeData, SubmissionsSchemeData, UpscanCallback}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.test.Helpers._
import org.mockito.internal.verification.VerificationModeFactory
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status
import play.api.libs.json.JsObject
import play.api.mvc.{PlayBodyParsers, Request}
import play.api.test.FakeRequest
import services.audit.AuditEvents
import services.{FileDownloadService, PresubmissionService, ValidationService}
import uk.gov.hmrc.auth.core.AuthConnector
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing

import scala.concurrent.Future
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

class ReceivePresubmissionControllerSpec extends TestKit(ActorSystem("ReceivePresubmissionControllerSpec"))
  with ERSTestHelper with BeforeAndAfterEach with WithMockedAuthActions {

  val mockAuthAction: AuthAction = mock[AuthAction]
  val mockPresubmissionService: PresubmissionService = mock[PresubmissionService]
  val mockValidationService: ValidationService = mock[ValidationService]
  val mockErsLoggingAndAuditing: ErsLoggingAndAuditing = mock[ErsLoggingAndAuditing]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockAuditEvents: AuditEvents = mock[AuditEvents]
  val mockBodyParser: PlayBodyParsers = mock[PlayBodyParsers]
  val mockApplicationConfig: ApplicationConfig = mock[ApplicationConfig]

  def testFileDownloadService(downloadResponse: String): FileDownloadService = new FileDownloadService(mockApplicationConfig) {
    override def makeRequest(request: HttpRequest): Future[HttpResponse] = Future.successful(HttpResponse(entity = downloadResponse))

    when(mockApplicationConfig.uploadCsvSizeLimit).thenReturn(104857600)
    when(mockApplicationConfig.maxGroupSize).thenReturn(10000)
  }

  val mockMetrics: Metrics = mock[Metrics]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMetrics)
    reset(mockErsLoggingAndAuditing)
    reset(mockAuthAction)
    reset(mockValidationService)
    reset(mockAuthConnector)
    reset(mockAuditEvents)
  }

  def buildPresubmissionController(validationResult: Boolean = true,
                                   storeJsonResult: Boolean = true,
                                   downloadResponse: String = Fixtures.submissionsSchemeData.toString,
                                   mockSubmissionResult: Boolean = false,
                                   failSubmitJson: Option[Throwable] = None
                                  ): ReceivePresubmissionController =
    new ReceivePresubmissionController(
      mockPresubmissionService,
      mockValidationService,
      testFileDownloadService(downloadResponse),
      mockErsLoggingAndAuditing,
      mockAuthConnector,
      mockAuditEvents,
      mockMetrics,
      mockCc,
      mockBodyParser,
      mockApplicationConfig
    ) {
      mockJsValueAuthAction

      when(mockApplicationConfig.submissionParallelism).thenReturn(2)
      when(mockPresubmissionService.storeJson(any[SchemeData])(any[Request[_]](), any[HeaderCarrier]()))
        .thenReturn(Future(storeJsonResult))
      when(mockPresubmissionService.storeJsonV2(any[SubmissionsSchemeData], any[SchemeData])(any[Request[_]](), any[HeaderCarrier]()))
        .thenReturn(Future(storeJsonResult))
      when(mockValidationService.validateSchemeData(any[JsObject]))
        .thenReturn(if (validationResult) Some(Fixtures.schemeData) else None)
      when(mockValidationService.validateSubmissionsSchemeData(any[JsObject]))
        .thenReturn(if (validationResult) Some(Fixtures.submissionsSchemeData) else None)

      override def authorisedAction(empRef: String): AuthAction = mockAuthAction

      override def submitJson(fileSource: Source[(Seq[Seq[ByteString]], Long), _], submissionsSchemeData: SubmissionsSchemeData)
                             (implicit request: Request[_], hc: HeaderCarrier): Future[(Boolean, Long)] = {
        if (mockSubmissionResult) {
          Future((false, 3L))
        } else if (failSubmitJson.isDefined) {
          Future.failed(failSubmitJson.get)
        } else {
          super.submitJson(fileSource, submissionsSchemeData)
        }
      }
    }

  "calling receivePresubmissionJson" should {

    "return BadRequest if invalid json is given" in {
      val presubmissionController = buildPresubmissionController(validationResult = false)
      val result = presubmissionController.receivePresubmissionJson("")(FakeRequest().withBody(Fixtures.invalidJson))
      status(result) shouldBe BAD_REQUEST
      verify(mockMetrics, VerificationModeFactory.times(0)).storePresubmission(_, _)
      verify(mockMetrics, VerificationModeFactory.times(0)).failedStorePresubmission()
    }

    "return InvalidServerError if valid json is given but storage fails" in {
      val presubmissionController = buildPresubmissionController(storeJsonResult = false)
      val result = presubmissionController.receivePresubmissionJson("")(FakeRequest().withBody(Fixtures.schemeDataJson))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      verify(mockMetrics, VerificationModeFactory.times(0)).storePresubmission(_, _)
      verify(mockMetrics, VerificationModeFactory.times(1)).failedStorePresubmission()
    }

    "return OK if valid json is given and storage succeeds" in {
      val presubmissionController = buildPresubmissionController()
      val result = presubmissionController.receivePresubmissionJson("")(FakeRequest().withBody(Fixtures.schemeDataJson))
      status(result) shouldBe OK
      verify(mockMetrics, VerificationModeFactory.times(1)).storePresubmission(_, _)
      verify(mockMetrics, VerificationModeFactory.times(0)).failedStorePresubmission()
    }
  }

  "calling receivePresubmissionJsonV2" should {

    "return BadRequest if invalid json is given" in {
      val presubmissionController = buildPresubmissionController(validationResult = false)
      val result = presubmissionController.receivePresubmissionJsonV2("")(FakeRequest().withBody(Fixtures.invalidJson))
      status(result) shouldBe BAD_REQUEST
      verify(mockMetrics, VerificationModeFactory.times(0)).storePresubmission(_, _)
      verify(mockMetrics, VerificationModeFactory.times(0)).failedStorePresubmission()
    }

    "return INTERNAL_SERVER_ERROR if valid json is given but storage fails" in {
      val presubmissionController = buildPresubmissionController(storeJsonResult = false)
      when(mockPresubmissionService.removeJson(any())(any(), any())).thenReturn(Future(true))
      val result = presubmissionController.receivePresubmissionJsonV2("")(FakeRequest().withBody(Fixtures.submissionsSchemeDataJson))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      verify(mockMetrics, VerificationModeFactory.times(0)).storePresubmission(_, _)
      verify(mockMetrics, VerificationModeFactory.times(1)).failedStorePresubmission()
    }

    "return INTERNAL_SERVER_ERROR and log a warning if valid json is given, some data is stored and then failed to be removed" in {
      val presubmissionController = buildPresubmissionController(mockSubmissionResult = true)
      when(mockPresubmissionService.removeJson(any())(any(), any())).thenReturn(Future(false))
      val result = presubmissionController.receivePresubmissionJsonV2("")(FakeRequest().withBody(Fixtures.submissionsSchemeDataJson))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      verify(mockMetrics, VerificationModeFactory.times(0)).storePresubmission(_, _)
      verify(mockMetrics, VerificationModeFactory.times(1)).failedStorePresubmission()
    }

    "return OK if valid json and storage succeeds" in {
      val presubmissionController = buildPresubmissionController()
      val result = presubmissionController.receivePresubmissionJsonV2("")(FakeRequest().withBody(Fixtures.submissionsSchemeDataJson))
      status(result) shouldBe OK
      verify(mockMetrics, VerificationModeFactory.times(1)).storePresubmission(_, _)
      verify(mockMetrics, VerificationModeFactory.times(0)).failedStorePresubmission()
    }

  }

  "calling storePresubmission" should {
    "return InternalServerError when receiving an UpstreamErrorResponse from submitJson" in {
      val submissionsSchemeData: SubmissionsSchemeData = SubmissionsSchemeData(SIP.schemeInfo, "sip sheet name",
        UpscanCallback("name", "/download/url"), 1)

      val presubmissionController = buildPresubmissionController(failSubmitJson = Some(UpstreamErrorResponse("a message", 500)))
      val result = await(presubmissionController.storePresubmission(submissionsSchemeData)(FakeRequest().withBody(Fixtures.submissionsSchemeDataJson),
        HeaderCarrier.apply()))

      result.header.status shouldBe Status.INTERNAL_SERVER_ERROR
      bodyOf(result) shouldBe "a message"
    }

    "return InternalServerError when receiving an unknown exception from submitJson" in {
      val submissionsSchemeData: SubmissionsSchemeData = SubmissionsSchemeData(SIP.schemeInfo, "sip sheet name",
        UpscanCallback("name", "/download/url"), 1)

      val presubmissionController = buildPresubmissionController(failSubmitJson = Some(new RuntimeException("a different message")))
      val result = await(presubmissionController.storePresubmission(submissionsSchemeData)(FakeRequest().withBody(Fixtures.submissionsSchemeDataJson),
        HeaderCarrier.apply()))

      result.header.status shouldBe Status.INTERNAL_SERVER_ERROR
      bodyOf(result) shouldBe "a different message"
    }
  }
}
