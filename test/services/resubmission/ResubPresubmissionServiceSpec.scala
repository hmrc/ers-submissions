/*
 * Copyright 2019 HM Revenue & Customs
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

package services.resubmission

import fixtures.Fixtures
import models.{ADRTransferException, ErsSummary, ResubmissionException}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.mockito.ArgumentMatchers._
import play.api.mvc.Request
import play.api.test.FakeRequest
import repositories.MetadataMongoRepository
import services.SubmissionCommonService
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class ResubPresubmissionServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with WithFakeApplication {
  implicit val hc: HeaderCarrier = new HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()

  val mockMetadataRepository: MetadataMongoRepository = mock[MetadataMongoRepository]
  val mockSubmissionCommonService: SubmissionCommonService = mock[SubmissionCommonService]
  val mockSchedulerLoggingAndAuditing: ErsLoggingAndAuditing = mock[ErsLoggingAndAuditing]

  override def beforeEach() = {
    super.beforeEach()
    reset(mockMetadataRepository)
    reset(mockSubmissionCommonService)
    reset(mockSchedulerLoggingAndAuditing)
  }

  "processFailedSubmissions" should {

    val resubPresubmissionService: ResubPresubmissionService = new ResubPresubmissionService {
      override lazy val metadataRepository: MetadataMongoRepository = mockMetadataRepository
      override val submissionCommonService: SubmissionCommonService = mockSubmissionCommonService
      override val schedulerLoggingAndAuditing: ErsLoggingAndAuditing = mockSchedulerLoggingAndAuditing

      override def startResubmission(ersSummary: ErsSummary)(implicit request: Request[_], hc: HeaderCarrier): Future[Boolean] = {
        Future.successful(false)
      }
    }

    "return the result of startResubmission if findAndUpdateByStatus is successful and returns a record" in {
      when(
        mockMetadataRepository.findAndUpdateByStatus(any[List[String]](), anyBoolean(), anyBoolean(),any[Option[List[String]]], any[Option[String]])
      ).thenReturn(
        Future.successful(Some(Fixtures.metadata))
      )
      val result = await(resubPresubmissionService.processFailedSubmissions())
      result shouldBe Some(false)
    }

    "return None if findAndUpdateByStatus is successful but returns None" in {
      when(
        mockMetadataRepository.findAndUpdateByStatus(any[List[String]](), anyBoolean(), anyBoolean(), any[Option[List[String]]], any[Option[String]])
      ).thenReturn(
        Future.successful(None)
      )
      val result = await(resubPresubmissionService.processFailedSubmissions())
      result shouldBe None
    }

    "rethrow ResubmissionException if such one occurs" in {
      when(
        mockMetadataRepository.findAndUpdateByStatus(any[List[String]](), anyBoolean(), anyBoolean(), any[Option[List[String]]], any[Option[String]])
      ).thenReturn(
        Future.failed(ResubmissionException("test message", "test context", Some(Fixtures.schemeInfo)))
      )
      val result = intercept[ResubmissionException] {
        await(resubPresubmissionService.processFailedSubmissions())
      }
      result.message shouldBe "test message"
      result.context shouldBe "test context"
      result.schemeInfo.get shouldBe Fixtures.schemeInfo
    }

    "throw ResubmissionException if Exception occurs" in {
      when(
        mockMetadataRepository.findAndUpdateByStatus(any[List[String]](), anyBoolean(), anyBoolean(), any[Option[List[String]]], any[Option[String]])
      ).thenReturn(
        Future.failed(new Exception("test message"))
      )
      val result = intercept[ResubmissionException] {
        await(resubPresubmissionService.processFailedSubmissions())
      }
      result.message shouldBe "Searching for data to be resubmitted"
      result.context shouldBe "ResubPresubmissionService.processFailedSubmissions.findAndUpdateByStatus"
      result.schemeInfo shouldBe None
    }

  }

  "startResubmission" should {
    val resubPresubmissionService: ResubPresubmissionService = new ResubPresubmissionService {
      override lazy val metadataRepository: MetadataMongoRepository = mockMetadataRepository
      override val submissionCommonService: SubmissionCommonService = mockSubmissionCommonService
      override val schedulerLoggingAndAuditing: ErsLoggingAndAuditing = mockSchedulerLoggingAndAuditing
    }

    "return the result of callProcessData if ErsSubmissions is successfully extracted" in {
      when(
        mockSubmissionCommonService.callProcessData(any[ErsSummary](), anyString(), anyString())(any(), any())
      ).thenReturn(
        Future.successful(true)
      )
      val result = await(resubPresubmissionService.startResubmission(Fixtures.metadata))
      result shouldBe true
    }

    "audit failed submission if callProcessData throws exception" in {
      when(
        mockSubmissionCommonService.callProcessData(any[ErsSummary](), anyString(), anyString())(any(), any())
      ).thenReturn(
        Future.failed(new RuntimeException("test message"))
      )
      val result = intercept[ResubmissionException] {
        await(resubPresubmissionService.startResubmission(Fixtures.metadata))
      }
      result.message shouldBe "Resubmitting data to ADR - Exception: test message"
      result.context shouldBe "ResubPresubmissionService.startResubmission.callProcessData"
      result.schemeInfo.get shouldBe Fixtures.metadata.metaData.schemeInfo
    }

    "throw ResubmissionException if ADRTransferException occurs" in {
      when(
        mockSubmissionCommonService.callProcessData(any[ErsSummary](), anyString(), anyString())(any(), any())
      ).thenReturn(
        Future.failed(ADRTransferException(Fixtures.EMIMetaData, "test message", "test context"))
      )
      val result = intercept[ResubmissionException] {
        await(resubPresubmissionService.startResubmission(Fixtures.metadata))
      }
      result.message shouldBe "Resubmitting data to ADR - ADRTransferException: test message"
      result.context shouldBe "test context"
      result.schemeInfo.get shouldBe Fixtures.metadata.metaData.schemeInfo
    }

    "throw ResubmissionException if Exception occurs" in {
      when(
        mockSubmissionCommonService.callProcessData(any[ErsSummary](), anyString(), anyString())(any(), any())
      ).thenReturn(
        Future.failed(new Exception("test message"))
      )
      val result = intercept[ResubmissionException] {
        await(resubPresubmissionService.startResubmission(Fixtures.metadata))
      }
      result.message shouldBe "Resubmitting data to ADR - Exception: test message"
      result.context shouldBe "ResubPresubmissionService.startResubmission.callProcessData"
      result.schemeInfo.get shouldBe Fixtures.metadata.metaData.schemeInfo
    }
  }

}
