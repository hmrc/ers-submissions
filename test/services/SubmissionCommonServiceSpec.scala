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

package services

import java.util.concurrent.TimeUnit

import connectors.ADRConnector
import fixtures.Fixtures
import metrics.Metrics
import models._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.internal.verification.VerificationModeFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import repositories.{MetadataMongoRepository, Repositories}
import services.audit.AuditEvents
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import utils.LoggingAndRexceptions.{ADRExceptionEmitter, ErsLoggingAndAuditing}
import utils.{ADRSubmission, SubmissionCommon}

import scala.concurrent.Future

class SubmissionCommonServiceSpec
  extends UnitSpec with MockitoSugar with BeforeAndAfterEach with GuiceOneAppPerSuite {
  implicit val hc: HeaderCarrier = new HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()

  val mockErsLoggingAndAuditing: ErsLoggingAndAuditing = new ErsLoggingAndAuditing(auditEvents) {
    override val buildDataMessage: PartialFunction[Object, String] = {
      case _ => ""
    }
    override def handleFailure(schemeInfo: SchemeInfo, message: String)(implicit request: Request[_], hc: HeaderCarrier): Unit = {}
  }
  val adrConnector: ADRConnector = mock[ADRConnector]
  val adrSubmission: ADRSubmission = mock[ADRSubmission]
  val submissionCommon: SubmissionCommon = mock[SubmissionCommon]
  val metrics: Metrics = mock[Metrics]
  val ersLoggingAndAuditing: ErsLoggingAndAuditing = mockErsLoggingAndAuditing
  val mockMetadataRepository: MetadataMongoRepository = mock[MetadataMongoRepository]
  val auditEvents: AuditEvents = mock[AuditEvents]
  val repositories: Repositories = mock[Repositories]
  val adrExceptionEmmiter: ADRExceptionEmitter = app.injector.instanceOf[ADRExceptionEmitter]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(metrics)
    reset(adrSubmission)
    reset(adrConnector)
  }

  "callProcessData" should {
    "return the result of processData if there are no exceptions" in {
      val submissionCommonService: SubmissionService =
        new SubmissionService(repositories, adrConnector, adrSubmission, submissionCommon, ersLoggingAndAuditing, adrExceptionEmmiter, auditEvents, metrics) {

        override lazy val metadataRepository: MetadataMongoRepository = mockMetadataRepository
        override def processData(ersSummary: ErsSummary, failedStatus: String, successStatus: String)(implicit request: Request[_], hc: HeaderCarrier): Future[Boolean] = {
          Future.successful(true)
        }
      }

      val result = await(submissionCommonService.callProcessData(Fixtures.EMISummaryDate, "failed", "success"))
      result shouldBe true
    }

    "rethrows ADRTransferException" in {
      val submissionCommonService: SubmissionService =
        new SubmissionService(repositories, adrConnector, adrSubmission, submissionCommon, ersLoggingAndAuditing, adrExceptionEmmiter, auditEvents, metrics) {

        override lazy val metadataRepository: MetadataMongoRepository = mockMetadataRepository
        when(mockMetadataRepository.updateStatus( any[SchemeInfo](), anyString()))
          .thenReturn(Future.successful(true))

        override def processData(ersSummary: ErsSummary, failedStatus: String, successStatus: String)(implicit request: Request[_], hc: HeaderCarrier): Future[Boolean] = {
          Future.failed(ADRTransferException(Fixtures.EMIMetaData, "test message", "text context"))
        }
      }

      val result = intercept[ADRTransferException] {
        await(submissionCommonService.callProcessData(Fixtures.EMISummaryDate, "failed", "success"))
      }
      result.message shouldBe "test message"
      result.context shouldBe "text context"
      result.ersMetaData shouldBe Fixtures.EMIMetaData
    }

    "throws ADRTransferException if Exception occurs" in {
      val submissionCommonService: SubmissionService =
        new SubmissionService(repositories, adrConnector, adrSubmission, submissionCommon, ersLoggingAndAuditing, adrExceptionEmmiter, auditEvents, metrics) {

          override lazy val metadataRepository: MetadataMongoRepository = mockMetadataRepository
          when(mockMetadataRepository.updateStatus( any[SchemeInfo](), anyString()))
            .thenReturn(Future.successful(true))

        override def processData(ersSummary: ErsSummary, failedStatus: String, successStatus: String)(implicit request: Request[_], hc: HeaderCarrier): Future[Boolean] = {
          Future.failed(new Exception("test message"))
        }
      }

      val result = intercept[ADRTransferException] {
        await(submissionCommonService.callProcessData(Fixtures.EMISummaryDate, "failed", "success"))
      }
      result.message shouldBe "Exception processing submission"
      result.context shouldBe "PostsubmissionService.callProcessData"
      result.ersMetaData shouldBe Fixtures.EMISummaryDate.metaData
    }
  }

  "processData" should {
    val submissionCommonService: SubmissionService =
    new SubmissionService(repositories, adrConnector, adrSubmission, submissionCommon, ersLoggingAndAuditing, adrExceptionEmmiter, auditEvents, metrics) {

      override def transformData(ersSummary: ErsSummary)(implicit request: Request[_], hc: HeaderCarrier): Future[JsObject] = {
        Future.successful(Json.obj())
      }
      override def sendToADRUpdatePostData(ersSummary: ErsSummary, adrData: JsObject, failedStatus: String, successStatus: String)(implicit request: Request[_], hc: HeaderCarrier): Future[Boolean] = {
        Future.successful(true)
      }
    }

    "return the result of storePostSubmissionData" in {
      val result = await(submissionCommonService.processData(Fixtures.EMISummaryDate, "failed", "success"))
      result shouldBe true
    }
  }

  "transformData" should {

    val submissionCommonService: SubmissionService =
      new SubmissionService(repositories, adrConnector, adrSubmission, submissionCommon, ersLoggingAndAuditing, adrExceptionEmmiter, auditEvents, metrics) {
    }

    "return json created by adrSubmission.generateSubmission" in {
      when(adrSubmission.generateSubmission()(any[Request[_]](), any[HeaderCarrier], any[ErsSummary]()))
        .thenReturn(Fixtures.schemeDataJson)

      val result = await(submissionCommonService.transformData(Fixtures.metadata))
      result shouldBe Fixtures.schemeDataJson
      verify(metrics, VerificationModeFactory.times(1)).generateJson(any[Long](), any[TimeUnit]())
    }

    "rethrows ADR exception" in {
      when(adrSubmission.generateSubmission()(any[Request[_]](), any[HeaderCarrier], any[ErsSummary]()))
        .thenReturn(Future.failed(ADRTransferException(mock[ErsMetaData], "ADRTransferException", "")))

      val result = intercept[ADRTransferException] {
        await(submissionCommonService.transformData(Fixtures.metadata))
      }
      result.getMessage shouldBe "ADRTransferException"
      verify(metrics, VerificationModeFactory.times(0)).generateJson(any[Long](), any[TimeUnit]())
    }

    "throws ADR exception" in {
      when(adrSubmission.generateSubmission()(any[Request[_]](), any[HeaderCarrier], any[ErsSummary]()))
        .thenReturn(Future.failed(new Exception("Exception")))

      val result = intercept[Exception] {
        await(submissionCommonService.transformData(Fixtures.metadata))
      }
      result.getMessage shouldBe "Exception during transformData"
      verify(metrics, VerificationModeFactory.times(0)).generateJson(any[Long](), any[TimeUnit]())
    }
  }

  "sendToADRUpdatePostData" should {
    val submissionCommonService: SubmissionService =
      new SubmissionService(repositories, adrConnector, adrSubmission, submissionCommon, ersLoggingAndAuditing, adrExceptionEmmiter, auditEvents, metrics) {

      override def updatePostsubmission(adrSubmissionStatus: Int, status: String, ersSummary: ErsSummary)(implicit request: Request[_], hc: HeaderCarrier): Future[Boolean] = Future.successful(true)
    }

    "return result from updatePostsubmission if sending to ADR is successful" in {
      when(adrConnector.sendData(any[JsObject](), anyString())(any[HeaderCarrier]()))
        .thenReturn(Future.successful(HttpResponse(202)))

      val result = await(submissionCommonService.sendToADRUpdatePostData(Fixtures.metadata, Fixtures.metadataJson, Statuses.Failed.toString, Statuses.Sent.toString))
      result shouldBe true
      verify(metrics, VerificationModeFactory.times(1)).sendToADR(any[Long](), any[TimeUnit]())
      verify(metrics, VerificationModeFactory.times(1)).successfulSendToADR()
      verify(metrics, VerificationModeFactory.times(0)).failedSendToADR()
    }

    "return result from updatePostsubmission if sending to ADR failed" in {
      when(adrConnector.sendData(any[JsObject](), anyString())(any[HeaderCarrier]()))
        .thenReturn(Future.successful(HttpResponse(500)))

      val result = await(submissionCommonService.sendToADRUpdatePostData(Fixtures.metadata, Fixtures.metadataJson, Statuses.Failed.toString, Statuses.Sent.toString))
      result shouldBe true
      verify(metrics, VerificationModeFactory.times(0)).sendToADR(any[Long](), any[TimeUnit]())
      verify(metrics, VerificationModeFactory.times(0)).successfulSendToADR()
      verify(metrics, VerificationModeFactory.times(1)).failedSendToADR()
    }

    "re-throws ADRTransferException if sending to ADR or update throws ADRTransferException exception" in {
      when(adrConnector.sendData(any[JsObject](), anyString())(any[HeaderCarrier]()))
        .thenReturn(Future.failed(new ADRTransferException(Fixtures.metadata.metaData, "errorMessage", "errorContext")))

      intercept[ADRTransferException] {
        await(submissionCommonService.sendToADRUpdatePostData(Fixtures.metadata, Fixtures.metadataJson, Statuses.Failed.toString, Statuses.Sent.toString))
      }
      verify(metrics, VerificationModeFactory.times(0)).sendToADR(any[Long](), any[TimeUnit]())
      verify(metrics, VerificationModeFactory.times(0)).successfulSendToADR()
      verify(metrics, VerificationModeFactory.times(0)).failedSendToADR()
    }

    "throws ADRTransferException if sending to ADR or update throws exception" in {
      when(adrConnector.sendData(any[JsObject](), anyString())(any[HeaderCarrier]())).thenReturn(Future.failed(new Exception("errorMessage")))

      intercept[ADRTransferException] {
        await(submissionCommonService.sendToADRUpdatePostData(Fixtures.metadata, Fixtures.metadataJson, Statuses.Failed.toString, Statuses.Sent.toString))
      }
      verify(metrics, VerificationModeFactory.times(0)).sendToADR(any[Long](), any[TimeUnit]())
      verify(metrics, VerificationModeFactory.times(0)).successfulSendToADR()
      verify(metrics, VerificationModeFactory.times(0)).failedSendToADR()
    }
  }

  "updatePostsubmission" should {

    val submissionCommonService: SubmissionService =
      new SubmissionService(repositories, adrConnector, adrSubmission, submissionCommon, ersLoggingAndAuditing, adrExceptionEmmiter, auditEvents, metrics) {
        override lazy val metadataRepository: MetadataMongoRepository = mockMetadataRepository
      }

    "true if update is successful and sending to ADR returned 202" in {
      when(mockMetadataRepository.updateStatus(any[SchemeInfo](), anyString()))
        .thenReturn(Future.successful(true))

      val result = await(submissionCommonService.updatePostsubmission(202, "sent", Fixtures.metadata))
      result shouldBe true
      verify(metrics, VerificationModeFactory.times(1)).updatePostsubmissionStatus(any[Long](), any[TimeUnit]())
    }

    "throws ADRTransferException if update failed" in {
      when(mockMetadataRepository.updateStatus(any[SchemeInfo](), anyString()))
        .thenReturn(Future.successful(false))

      intercept[ADRTransferException] {
        await(submissionCommonService.updatePostsubmission(202, "sent", Fixtures.metadata))
      }
      verify(metrics, VerificationModeFactory.times(0)).updatePostsubmissionStatus(any[Long](), any[TimeUnit]())
    }

    "throws ADRTransferException if update is successful and sending to ADR returned 500" in {
      when(mockMetadataRepository.updateStatus(any[SchemeInfo](), anyString()))
        .thenReturn(Future.successful(true))

      intercept[ADRTransferException] {
        await(submissionCommonService.updatePostsubmission(500, "failed", Fixtures.metadata))
      }
      verify(metrics, VerificationModeFactory.times(1)).updatePostsubmissionStatus(any[Long](), any[TimeUnit]())
    }
  }
}
