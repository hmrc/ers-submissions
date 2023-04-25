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

import akka.actor.ActorSystem
import config.ApplicationConfig
import helpers.ERSTestHelper
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.Request
import play.api.test.FakeRequest
import repositories.{DefaultLockRepositoryProvider, Repositories}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing

import scala.concurrent.Future

class SchedulerServiceSpec extends ERSTestHelper with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()
  val mockSchedulerLoggingAndAuditing: ErsLoggingAndAuditing = app.injector.instanceOf[ErsLoggingAndAuditing]
  val mockApplicationConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val mockRepositories: Repositories = mock[Repositories]
  val mockMongoLockRepository: DefaultLockRepositoryProvider = mock[DefaultLockRepositoryProvider]
  val mockResubPresubmissionService: ResubPresubmissionService = mock[ResubPresubmissionService]
  val mockActorSystem: ActorSystem = mock[ActorSystem]
  val mockServicesConfig: ServicesConfig = mock[ServicesConfig]
  val mockErsLoggingAndAuditing: ErsLoggingAndAuditing = mock[ErsLoggingAndAuditing]


  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockResubPresubmissionService)
  }

  "resubmit" should {

    "return the result of processFailedGridFSSubmissions" in {
      val schedulerService: ReSubmissionSchedulerService = new ReSubmissionSchedulerService(
        mockMongoLockRepository,
        mockResubPresubmissionService,
        mockServicesConfig,
        mockErsLoggingAndAuditing)

      when(mockResubPresubmissionService.processFailedSubmissions(any())(any(), any())).thenReturn(Future.successful(true))

      val result = await(schedulerService.resubmit())
      result shouldBe true
    }

    "return false if resubmitting gridFS data throws exception" in {
      val schedulerService: ReSubmissionSchedulerService = new ReSubmissionSchedulerService(
        mockMongoLockRepository,
        mockResubPresubmissionService,
        mockServicesConfig,
        mockErsLoggingAndAuditing)

      when(mockResubPresubmissionService.processFailedSubmissions(any())(any(), any())).thenReturn(Future.failed(new RuntimeException))

      val result = await(schedulerService.resubmit())
      result shouldBe false
    }
  }
}
