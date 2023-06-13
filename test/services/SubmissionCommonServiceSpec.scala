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

import common.ERSEnvelope
import common.ERSEnvelope.ERSEnvelope

import java.util.concurrent.TimeUnit
import connectors.ADRConnector
import fixtures.Fixtures
import helpers.ERSTestHelper
import metrics.Metrics
import models._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.internal.verification.VerificationModeFactory
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import repositories.{MetadataMongoRepository, Repositories}
import services.audit.AuditEvents
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.LoggingAndRexceptions.{ADRExceptionEmitter, ErsLoggingAndAuditing}
import utils.{ADRSubmission, SubmissionCommon}

import scala.concurrent.{ExecutionContext, Future}

class SubmissionCommonServiceSpec extends ERSTestHelper with BeforeAndAfterEach with EitherValues {

  implicit val hc: HeaderCarrier = new HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()

  val auditEvents: AuditEvents = mock[AuditEvents]
  val mockMetadataRepository: MetadataMongoRepository = mock[MetadataMongoRepository]
  val repositories: Repositories = mock[Repositories]
  val adrExceptionEmmiter: ADRExceptionEmitter = app.injector.instanceOf[ADRExceptionEmitter]
  val adrConnector: ADRConnector = mock[ADRConnector]
  val adrSubmission: ADRSubmission = mock[ADRSubmission]
  val submissionCommon: SubmissionCommon = mock[SubmissionCommon]
  val metrics: Metrics = mock[Metrics]

  val ersLoggingAndAuditing: ErsLoggingAndAuditing = new ErsLoggingAndAuditing(auditEvents) {
    override val buildDataMessage: PartialFunction[Object, String] = {
      case _ => ""
    }
    override def handleFailure(schemeInfo: SchemeInfo, message: String)(implicit hc: HeaderCarrier): Unit = {}
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(metrics)
    reset(adrSubmission)
    reset(adrConnector)
  }

  "callProcessData" should {
    "return the result of processData if there are no errors" in {
      val submissionCommonService: SubmissionService =
        new SubmissionService(repositories, adrConnector, adrSubmission, submissionCommon, auditEvents, metrics) {

        override lazy val metadataRepository: MetadataMongoRepository = mockMetadataRepository
        override def processData(ersSummary: ErsSummary, failedStatus: String, successStatus: String)
                                (implicit request: Request[_], hc: HeaderCarrier): ERSEnvelope[Boolean] = {
          ERSEnvelope(true)
        }
      }

      val result = await(submissionCommonService.callProcessData(Fixtures.EMISummaryDate, "failed", "success").value)
      result.value shouldBe true
    }

    "recover and return false for ADRTransferError" in {
      val submissionCommonService: SubmissionService =
        new SubmissionService(repositories, adrConnector, adrSubmission, submissionCommon, auditEvents, metrics) {

        override lazy val metadataRepository: MetadataMongoRepository = mockMetadataRepository
        when(mockMetadataRepository.updateStatus(any[SchemeInfo](), anyString(), any()))
          .thenReturn(ERSEnvelope(true))

        override def processData(ersSummary: ErsSummary, failedStatus: String, successStatus: String)
                                (implicit request: Request[_], hc: HeaderCarrier): ERSEnvelope[Boolean] = {
          ERSEnvelope(ADRTransferError())
        }
      }

      val result = await(submissionCommonService.callProcessData(Fixtures.EMISummaryDate, "failed", "success").value)

      result.value shouldBe false
    }

    "recover and return false for MongoGenericError" in {
      val submissionCommonService: SubmissionService =
        new SubmissionService(repositories, adrConnector, adrSubmission, submissionCommon, auditEvents, metrics) {

          override lazy val metadataRepository: MetadataMongoRepository = mockMetadataRepository
          when(mockMetadataRepository.updateStatus(any[SchemeInfo](), anyString(), any()))
            .thenReturn(ERSEnvelope(true))

        override def processData(ersSummary: ErsSummary, failedStatus: String, successStatus: String)
                                (implicit request: Request[_], hc: HeaderCarrier): ERSEnvelope[Boolean] = {
          ERSEnvelope(MongoGenericError("test message"))
        }
      }

      val result = await(submissionCommonService.callProcessData(Fixtures.EMISummaryDate, "failed", "success").value)

      result.value shouldBe false
    }
  }

  "processData" should {
    val submissionCommonService: SubmissionService =
    new SubmissionService(repositories, adrConnector, adrSubmission, submissionCommon, auditEvents, metrics) {

      override def transformData(ersSummary: ErsSummary)(implicit request: Request[_], hc: HeaderCarrier): ERSEnvelope[JsObject] =
        ERSEnvelope.fromFuture(Future.successful(Json.obj()))

      override def sendToADRUpdatePostData(ersSummary: ErsSummary, adrData: JsObject, failedStatus: String, successStatus: String)
                                          (implicit hc: HeaderCarrier): ERSEnvelope[Boolean] = ERSEnvelope(true)
    }

    "return the result of storePostSubmissionData" in {
      val result = await(submissionCommonService.processData(Fixtures.EMISummaryDate, "failed", "success").value).value
      result shouldBe true
    }
  }

  "transformData" should {
    val submissionCommonService: SubmissionService =
      new SubmissionService(repositories, adrConnector, adrSubmission, submissionCommon, auditEvents, metrics) {
    }

    "return created json" in {
      when(adrSubmission.generateSubmission(any[ErsSummary]())(any[Request[_]](), any[HeaderCarrier]))
        .thenReturn(ERSEnvelope(Fixtures.schemeDataJson))

      val result = await(submissionCommonService.transformData(Fixtures.metadata).value).value
      result shouldBe Fixtures.schemeDataJson
      verify(metrics, VerificationModeFactory.times(1)).generateJson(any[Long](), any[TimeUnit]())
    }

    "return JsonFromSheetsCreationError" in {
      when(adrSubmission.generateSubmission(any[ErsSummary]())(any[Request[_]](), any[HeaderCarrier]))
        .thenReturn(ERSEnvelope(JsonFromSheetsCreationError("could not parse csv")))

      val result = await(submissionCommonService.transformData(Fixtures.metadata).value)

      result.swap.value shouldBe JsonFromSheetsCreationError("could not parse csv")
      verify(metrics, VerificationModeFactory.times(0)).generateJson(any[Long](), any[TimeUnit]())
    }
  }

  "sendToADRUpdatePostData" should {
    val submissionCommonService: SubmissionService =
      new SubmissionService(repositories, adrConnector, adrSubmission, submissionCommon, auditEvents, metrics) {

      override def updatePostsubmission(adrSubmissionStatus: Int, status: String, ersSummary: SchemeInfo)
                                       (implicit hc: HeaderCarrier): ERSEnvelope[Boolean] = ERSEnvelope(true)
    }

    "return result from updatePostsubmission if sending to ADR is successful" in {
      when(adrConnector.sendData(any[JsObject](), anyString())(any[ExecutionContext](), any[HeaderCarrier]()))
        .thenReturn(ERSEnvelope(Future.successful(HttpResponse(202, ""))))

      val result = await(submissionCommonService.sendToADRUpdatePostData(Fixtures.metadata, Fixtures.metadataJson, Statuses.Failed.toString, Statuses.Sent.toString).value).value
      result shouldBe true
      verify(metrics, VerificationModeFactory.times(1)).sendToADR(any[Long](), any[TimeUnit]())
      verify(metrics, VerificationModeFactory.times(1)).successfulSendToADR()
      verify(metrics, VerificationModeFactory.times(0)).failedSendToADR()
    }

    "return result from updatePostsubmission if sending to ADR failed" in {
      when(adrConnector.sendData(any[JsObject](), anyString())(any[ExecutionContext](), any[HeaderCarrier]()))
        .thenReturn(ERSEnvelope(Future.successful(HttpResponse(500, ""))))

      val result = await(submissionCommonService.sendToADRUpdatePostData(Fixtures.metadata, Fixtures.metadataJson, Statuses.Failed.toString, Statuses.Sent.toString).value)
      result.value shouldBe true
      verify(metrics, VerificationModeFactory.times(0)).sendToADR(any[Long](), any[TimeUnit]())
      verify(metrics, VerificationModeFactory.times(0)).successfulSendToADR()
      verify(metrics, VerificationModeFactory.times(1)).failedSendToADR()
    }

    "return ADRTransferError if sending to ADR or update returns error" in {
      when(adrConnector.sendData(any[JsObject](), anyString())(any[ExecutionContext](), any[HeaderCarrier]()))
        .thenReturn(ERSEnvelope(ADRTransferError()))

      val result =
        await(submissionCommonService
          .sendToADRUpdatePostData(Fixtures.metadata, Fixtures.metadataJson, Statuses.Failed.toString, Statuses.Sent.toString).value)

      result.swap.value shouldBe ADRTransferError()
      verify(metrics, VerificationModeFactory.times(0)).sendToADR(any[Long](), any[TimeUnit]())
      verify(metrics, VerificationModeFactory.times(0)).successfulSendToADR()
      verify(metrics, VerificationModeFactory.times(0)).failedSendToADR()
    }
  }

  "updatePostsubmission" should {
    val submissionCommonService: SubmissionService =
      new SubmissionService(repositories, adrConnector, adrSubmission, submissionCommon, auditEvents, metrics) {
        override lazy val metadataRepository: MetadataMongoRepository = mockMetadataRepository
      }

    "true if update is successful and sending to ADR returned 202" in {
      when(mockMetadataRepository.updateStatus(any[SchemeInfo](), anyString(), any()))
        .thenReturn(ERSEnvelope(true))

      val result = await(submissionCommonService
        .updatePostsubmission(202, "sent", Fixtures.metadata.metaData.schemeInfo).value).value

      result shouldBe true
      verify(metrics, VerificationModeFactory.times(1)).updatePostsubmissionStatus(any[Long](), any[TimeUnit]())
    }

    "return SubmissionStatusUpdateError if update was successful but sending to ADR was not 202" in {
      when(mockMetadataRepository.updateStatus(any[SchemeInfo](), anyString(), any()))
        .thenReturn(ERSEnvelope(true))

      val result = await(submissionCommonService
          .updatePostsubmission(INTERNAL_SERVER_ERROR, "sent", Fixtures.metadata.metaData.schemeInfo).value)

      result.swap.value shouldBe SubmissionStatusUpdateError(Some(INTERNAL_SERVER_ERROR), Some("sent"))
      verify(metrics, VerificationModeFactory.times(1)).updatePostsubmissionStatus(any[Long](), any[TimeUnit]())
    }

    "return SubmissionStatusUpdateError if update failed with error from DB" in {
      when(mockMetadataRepository.updateStatus(any[SchemeInfo](), anyString(), any()))
        .thenReturn(ERSEnvelope(MongoGenericError("update failed")))

      val result =
        await(submissionCommonService
          .updatePostsubmission(INTERNAL_SERVER_ERROR, "failed", Fixtures.metadata.metaData.schemeInfo).value)

      result.swap.value shouldBe MongoGenericError("update failed")
      verify(metrics, VerificationModeFactory.times(0)).updatePostsubmissionStatus(any[Long](), any[TimeUnit]())
    }
  }
}
