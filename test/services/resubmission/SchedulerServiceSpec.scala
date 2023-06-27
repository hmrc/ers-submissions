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

package services.resubmission

import common.ERSEnvelope
import config.ApplicationConfig
import helpers.ERSTestHelper
import models.ResubmissionError
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import play.api.mvc.Request
import play.api.test.FakeRequest
import repositories.DefaultLockRepositoryProvider
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class SchedulerServiceSpec extends ERSTestHelper with BeforeAndAfterEach with EitherValues {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()
  val mockApplicationConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val mockMongoLockRepository: DefaultLockRepositoryProvider = mock[DefaultLockRepositoryProvider]
  val mockResubPresubmissionService: ResubPresubmissionService = mock[ResubPresubmissionService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockResubPresubmissionService)
  }

  "resubmit" should {

    "return the result of processFailedGridFSSubmissions if processFailedSubmissions returns true" in {
      val schedulerService: ReSubmissionSchedulerService = new ReSubmissionSchedulerService(
        mockApplicationConfig,
        mockMongoLockRepository,
        mockResubPresubmissionService)

      when(mockResubPresubmissionService.processFailedSubmissions(any())(any(), any())).thenReturn(ERSEnvelope(Future.successful(true)))

      val result = await(schedulerService.resubmit().value)
      result.value shouldBe true
    }

    "return the result of processFailedGridFSSubmissions if processFailedSubmissions returns false" in {
      val schedulerService: ReSubmissionSchedulerService = new ReSubmissionSchedulerService(
        mockApplicationConfig,
        mockMongoLockRepository,
        mockResubPresubmissionService)

      when(mockResubPresubmissionService.processFailedSubmissions(any())(any(), any())).thenReturn(ERSEnvelope(Future.successful(false)))

      val result = await(schedulerService.resubmit().value)
      result.value shouldBe false
    }

    "return ResubmissionError if resubmitting gridFS data returns error" in {
      val schedulerService: ReSubmissionSchedulerService = new ReSubmissionSchedulerService(
        mockApplicationConfig,
        mockMongoLockRepository,
        mockResubPresubmissionService)

      when(mockResubPresubmissionService.processFailedSubmissions(any())(any(), any())).thenReturn(ERSEnvelope(ResubmissionError()))

      val result = await(schedulerService.resubmit().value)
      result.swap.value shouldBe ResubmissionError()
    }
  }
}
