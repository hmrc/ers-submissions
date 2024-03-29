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
import models._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.internal.verification.VerificationModeFactory
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.{JsError, JsObject, JsSuccess}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services._
import services.audit.AuditEvents
import uk.gov.hmrc.http.HeaderCarrier

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext

class SubmissionControllerSpec extends ERSTestHelper with BeforeAndAfterEach {

  val mockMetrics: Metrics = mock[Metrics]
  val mockSubmissionCommonService: SubmissionService = mock[SubmissionService]
  val mockMetaService: MetadataService = mock[MetadataService]
  val mockAuditEvents: AuditEvents = mock[AuditEvents]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMetrics)
  }

  "calling receiveMetadataJson" should {

    def buildSubmissionController(isValidJson: Boolean = true, expectedResult: ERSEnvelope[Boolean] = ERSEnvelope(true))
                                 (implicit ec: ExecutionContext): SubmissionController = {
      new SubmissionController(mockSubmissionCommonService,
        mockMetaService,
        mockMetrics,
        mockAuditEvents,
        mockCc) {

      when(mockMetaService.validateErsSummaryFromJson(any[JsObject])).thenReturn(
        if (isValidJson) { JsSuccess(Fixtures.metadata) } else { JsError("validation error") }
      )

      when(
        mockSubmissionCommonService.callProcessData(any[ErsSummary], anyString(), anyString())(any[Request[_]](), any[HeaderCarrier]())
      ).thenReturn(expectedResult)
      }
    }

    "return BadRequest if request data is not valid" in {
      val submissionController = buildSubmissionController(isValidJson = false)
      val result = submissionController.receiveMetadataJson()(FakeRequest().withBody(Fixtures.invalidJson))
      status(result) shouldBe BAD_REQUEST
    }

    "return OK if data is successfully send to ADR and recorded in mongo" in {
      val submissionController = buildSubmissionController()
      val result = submissionController.receiveMetadataJson()(FakeRequest().withBody(Fixtures.metadataJson))
      status(result) shouldBe OK
    }

    "report INTERNAL_SERVER_ERROR when service returns an error" in {
      val submissionController = buildSubmissionController(isValidJson = true, ERSEnvelope(ADRTransferError()))
      val result = submissionController.receiveMetadataJson()(FakeRequest().withBody(Fixtures.metadataJson))
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "report INTERNAL_SERVER_ERROR when the service returns false" in {
      val submissionController = buildSubmissionController(isValidJson = true, ERSEnvelope(false))
      val result = submissionController.receiveMetadataJson()(FakeRequest().withBody(Fixtures.metadataJson))
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "calling saveMetadata" should {
    def buildSubmissionController(validateErsSummaryFromJsonResult: Boolean, storeErsSummaryResult: Option[Boolean]): SubmissionController =
      new SubmissionController(mockSubmissionCommonService,
        mockMetaService,
        mockMetrics,
        mockAuditEvents,
        mockCc) {

      when(
        mockMetaService.validateErsSummaryFromJson(any[JsObject]())
      ).thenReturn(if (validateErsSummaryFromJsonResult) JsSuccess(Fixtures.metadataNilReturn) else JsError("validation error"))

      when(
        mockMetaService.storeErsSummary(any[ErsSummary]())(any[HeaderCarrier]())
      ).thenReturn(
        ERSEnvelope(storeErsSummaryResult.toRight(MongoUnavailableError("mongo error")))
      )
    }

    "return BadRequest if json is invalid" in {
      val submissionController = buildSubmissionController(validateErsSummaryFromJsonResult = false, storeErsSummaryResult = Some(true))
      val result = submissionController.saveMetadata()(FakeRequest().withBody(Fixtures.invalidJson))
      status(result) shouldBe BAD_REQUEST
      verify(mockMetrics, VerificationModeFactory.times(0)).saveMetadata(any[Long](), any[TimeUnit]())
    }

    "return InternalServerError if json is valid but storing fails" in {
      val submissionController = buildSubmissionController(validateErsSummaryFromJsonResult = true, storeErsSummaryResult = Some(false))
      val result = submissionController.saveMetadata()(FakeRequest().withBody(Fixtures.metadataJson))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      verify(mockMetrics, VerificationModeFactory.times(0)).saveMetadata(any[Long](), any[TimeUnit]())
    }

    "return InternalServerError if json is valid but service returns an error" in {
      val submissionController = buildSubmissionController(validateErsSummaryFromJsonResult = true, storeErsSummaryResult = None)
      val result = submissionController.saveMetadata()(FakeRequest().withBody(Fixtures.metadataJson))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      verify(mockMetrics, VerificationModeFactory.times(0)).saveMetadata(any[Long](), any[TimeUnit]())
    }

    "return OK if json is valid and storing is successful" in {
      val submissionController = buildSubmissionController(validateErsSummaryFromJsonResult = true, storeErsSummaryResult = Some(true))
      val result = submissionController.saveMetadata()(FakeRequest().withBody(Fixtures.metadataJson))
      status(result) shouldBe OK
      verify(mockMetrics, VerificationModeFactory.times(1)).saveMetadata(any[Long](), any[TimeUnit]())
    }
  }
}
