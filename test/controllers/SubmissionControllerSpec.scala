/*
 * Copyright 2017 HM Revenue & Customs
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
import fixtures.Fixtures
import metrics.Metrics
import models._
import org.mockito.internal.verification.VerificationModeFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services._
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}
import org.mockito.Matchers._
import org.mockito.Mockito._
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.mvc._

class SubmissionControllerSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with WithFakeApplication {

  val mockMetrics: Metrics = mock[Metrics]
  val mockErsLoggingAndAuditing : ErsLoggingAndAuditing = mock[ErsLoggingAndAuditing]

  override def beforeEach() = {
    super.beforeEach()
    reset(mockMetrics)
    reset(mockErsLoggingAndAuditing)
  }

  "calling receiveMetadataJson" should {

    def buildSubmissionController(isValidJson: Boolean = true, expectedResult: Future[Boolean]= Future(true)): SubmissionController = new SubmissionController {
      val mockPresubmissionService: PresubmissionService = mock[PresubmissionService]
      val mockSubmissionCommonService: SubmissionCommonService = mock[SubmissionCommonService]
      val mockMetaService = mock[MetadataService]

      when(mockMetaService.validateErsSummaryFromJson(any[JsObject])).thenReturn(
        if (isValidJson) { Some(Fixtures.metadata) } else { None }
      )

      when(
        mockSubmissionCommonService.callProcessData(any[ErsSummary], anyString())(any[Request[_]](), any[HeaderCarrier]())
      ).thenReturn(
        expectedResult
      )

      override val submissionCommonService: SubmissionCommonService = mockSubmissionCommonService
      override val metadataService = mockMetaService
      override val metrics = mockMetrics
      override val ersLoggingAndAuditing = mockErsLoggingAndAuditing
      override val validationService: ValidationService = mock[ValidationService]
    }

    "return BadRequest if request data is not valid" in {
      val submissionController = buildSubmissionController(false)
      val result = await(submissionController.receiveMetadataJson()(FakeRequest().withBody(Fixtures.invalidJson)))
      status(result) shouldBe BAD_REQUEST
    }

    "return OK if data is successfully send to ADR and recorded in mongo" in {
      val submissionController = buildSubmissionController(true)
      val result = await(submissionController.receiveMetadataJson()(FakeRequest().withBody(Fixtures.metadataJson)))
      status(result) shouldBe OK
    }

    "report an error when data is not sent to ADR and recorded succesfully" in {
      val submissionController = buildSubmissionController(true, Future.failed(new ADRTransferException(mock[ErsMetaData],"","")))
      val result = await(submissionController.receiveMetadataJson()(FakeRequest().withBody(Fixtures.metadataJson)))
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "report an error when the process of sending data to adr fails to complete" in {
      val submissionController = buildSubmissionController(true, Future.failed(new Exception))
      val result = await(submissionController.receiveMetadataJson()(FakeRequest().withBody(Fixtures.metadataJson)))
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "report an error when the process of sending data to adr throws exception" in {
      val submissionController = new SubmissionController {
        val mockPresubmissionService: PresubmissionService = mock[PresubmissionService]
        val mockSubmissionCommonService: SubmissionCommonService = mock[SubmissionCommonService]
        val mockMetaService = mock[MetadataService]

        when(mockMetaService.validateErsSummaryFromJson(any[JsObject])).thenReturn(
          Some(Fixtures.metadata)
        )

        when(
          mockSubmissionCommonService.callProcessData(any[ErsSummary], anyString())(any[Request[_]](), any[HeaderCarrier]())
        ).thenThrow(
          new RuntimeException
        )

        override val submissionCommonService: SubmissionCommonService = mockSubmissionCommonService
        override val metadataService = mockMetaService
        override val metrics = mockMetrics
        override val ersLoggingAndAuditing = mockErsLoggingAndAuditing
        override val validationService: ValidationService = mock[ValidationService]
      }
      val result = await(submissionController.receiveMetadataJson()(FakeRequest().withBody(Fixtures.metadataJson)))
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

  }

  "calling saveMetadata" should {

    def buildSubmissionController(validateErsSummaryFromJsonResult: Boolean = true, storeErsSummaryResult: Boolean = true): SubmissionController = new SubmissionController {
      val mockPresubmissionService: PresubmissionService = mock[PresubmissionService]
      val mockSubmissionCommonService: SubmissionCommonService = mock[SubmissionCommonService]
      val mockMetaService = mock[MetadataService]

      when(
        mockMetaService.validateErsSummaryFromJson(any[JsObject]())
      ).thenReturn(
        Future.successful(
          validateErsSummaryFromJsonResult match {
            case true => Some(Fixtures.metadataNilReturn)
            case false => None
          }
        )
      )

      when(
        mockMetaService.storeErsSummary(any[ErsSummary]())(any[Request[_]](), any[HeaderCarrier]())
      ).thenReturn(
        Future.successful(storeErsSummaryResult)
      )

      override val submissionCommonService: SubmissionCommonService = mockSubmissionCommonService
      override val metadataService = mockMetaService
      override val metrics = mockMetrics
      override val ersLoggingAndAuditing = mockErsLoggingAndAuditing
      override val validationService: ValidationService = mock[ValidationService]

    }

    "return BadRequest if json is invalid" in {
      val submissionController = buildSubmissionController(false, true)
      val result = await(submissionController.saveMetadata(FakeRequest().withBody(Fixtures.invalidJson)))
      status(result) shouldBe BAD_REQUEST
      verify(mockMetrics, VerificationModeFactory.times(0)).saveMetadata(any[Long](), any[TimeUnit]())
    }

    "return InternalServerError if json is valid but storing fails" in {
      val submissionController = buildSubmissionController(true, false)
      val result = await(submissionController.saveMetadata(FakeRequest().withBody(Fixtures.metadataJson)))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      verify(mockMetrics, VerificationModeFactory.times(0)).saveMetadata(any[Long](), any[TimeUnit]())
    }

    "return OK if json is valid and storing is successful" in {
      val submissionController = buildSubmissionController(true, true)
      val result = await(submissionController.saveMetadata(FakeRequest().withBody(Fixtures.metadataJson)))
      status(result) shouldBe OK
      verify(mockMetrics, VerificationModeFactory.times(1)).saveMetadata(any[Long](), any[TimeUnit]())
    }
  }
}
