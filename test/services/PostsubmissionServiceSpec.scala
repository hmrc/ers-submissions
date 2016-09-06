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

package services

import fixtures.Fixtures
import models._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.test.FakeRequest
import repositories.JsonStoreInfoMongoRepository
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}
import utils.SubmissionCommon
import scala.concurrent.Future

class PostsubmissionServiceSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  implicit val hc: HeaderCarrier = new HeaderCarrier()
  implicit val request = FakeRequest().withBody(Fixtures.metadataJson)

  "calling processDataForADR" should {
    val mockJsonStoreInfoRepository: JsonStoreInfoMongoRepository = mock[JsonStoreInfoMongoRepository]
    val mockSubmissionCommonService: SubmissionCommonService = mock[SubmissionCommonService]

    val postsubmissionService: PostsubmissionService = new PostsubmissionService {
      override lazy val jsonStoreInfoRepository: JsonStoreInfoMongoRepository = mockJsonStoreInfoRepository
      override val submissionCommonService: SubmissionCommonService = mockSubmissionCommonService
      override val submissionCommon: SubmissionCommon = mock[SubmissionCommon]
    }

    "return ADRTransferException if ADRTransferException is thrown" in {
      reset(mockJsonStoreInfoRepository)
      reset(mockSubmissionCommonService)

      when(
        mockJsonStoreInfoRepository.createErsJsonStoreInfo(any[ErsJsonStoreInfo]())
      ).thenReturn(
        Future.failed(ADRTransferException(Fixtures.EMIMetaData, "test message", ""))
      )

      val result = intercept[ADRTransferException] {
        await(postsubmissionService.processDataForADR(Fixtures.summaryData))
      }
      result.message shouldBe "test message"
    }

    "return ADRTransferException if Exception is thrown" in {
      reset(mockJsonStoreInfoRepository)
      reset(mockSubmissionCommonService)

      when(
        mockJsonStoreInfoRepository.createErsJsonStoreInfo(any[ErsJsonStoreInfo]())
      ).thenReturn(
        Future.failed(new Exception("test message"))
      )

      val result = intercept[ADRTransferException] {
        await(postsubmissionService.processDataForADR(Fixtures.summaryData))
      }
      result.ersMetaData shouldBe Fixtures.summaryData.metaData
      result.message shouldBe "Exception during creating ErsJsonStoreInfo"
      result.context shouldBe "PostsubmissionService.processDataForADR"
    }

    "return ADRTransferException if createErsJsonStoreInfo fails" in {
      reset(mockJsonStoreInfoRepository)
      reset(mockSubmissionCommonService)

      when(
        mockJsonStoreInfoRepository.createErsJsonStoreInfo(any[ErsJsonStoreInfo]())
      ).thenReturn(
        Future.successful(false)
      )

      val result = intercept[ADRTransferException] {
        await(postsubmissionService.processDataForADR(Fixtures.summaryData))
      }
      result.ersMetaData shouldBe Fixtures.summaryData.metaData
      result.message shouldBe "Creating ErsJsonStoreInfo failed"
      result.context shouldBe "PostsubmissionService.processDataForADR"
    }

    "return the result of callProcessData if createErsJsonStoreInfo succeed" in {
      reset(mockJsonStoreInfoRepository)
      reset(mockSubmissionCommonService)

      when(
        mockJsonStoreInfoRepository.createErsJsonStoreInfo(any[ErsJsonStoreInfo]())
      ).thenReturn(
        Future.successful(true)
      )

      when(
        mockSubmissionCommonService.callProcessData(any[ErsSummary](), anyString())(any(), any())
      ).thenReturn(
        Future.successful(false)
      )

      val result = await(postsubmissionService.processDataForADR(Fixtures.summaryData))
      result shouldBe false
    }
  }
}
