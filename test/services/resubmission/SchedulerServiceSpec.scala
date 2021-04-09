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

package services.resubmission

import akka.actor.ActorSystem
import config.ApplicationConfig
import org.joda.time.DateTime
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.Request
import play.api.test.FakeRequest
import repositories.Repositories
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing

import scala.concurrent.Future

class SchedulerServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with GuiceOneAppPerSuite {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()
  val mockSchedulerLoggingAndAuditing: ErsLoggingAndAuditing = app.injector.instanceOf[ErsLoggingAndAuditing]
  val mockApplicationConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val mockRepositories: Repositories = mock[Repositories]
  val mockResubPresubmissionService: ResubPresubmissionService = mock[ResubPresubmissionService]
  val mockActorSystem: ActorSystem = mock[ActorSystem]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockResubPresubmissionService)
  }

  "getTime" should {
    System.out.println("GET TIME")
    val schedulerService: SchedulerService = new SchedulerService(
      mockApplicationConfig,
      mockRepositories,
      mockResubPresubmissionService,
      mockSchedulerLoggingAndAuditing,
      mockActorSystem) {
      System.out.println("SchedulerService start")
    }

    "return current date with given time" in {
      val hour = 15
      val minutes = 5
      val result = schedulerService.getTime(hour, minutes)
      result.getYear shouldBe DateTime.now.getYear
      result.getMonthOfYear shouldBe DateTime.now.getMonthOfYear
      result.getDayOfMonth shouldBe DateTime.now.getDayOfMonth
      result.getHourOfDay shouldBe hour
      result.getMinuteOfHour shouldBe minutes
    }
  }

  "schedulerStartTime" should {
    val schedulerServiceTest: SchedulerService = new SchedulerService(
      mockApplicationConfig,
      mockRepositories,
      mockResubPresubmissionService,
      mockSchedulerLoggingAndAuditing,
      mockActorSystem)

    "return current date with given time" in {
      val result = schedulerServiceTest.schedulerStartTime
      result.getYear shouldBe DateTime.now.getYear
      result.getMonthOfYear shouldBe DateTime.now.getMonthOfYear
      result.getDayOfMonth shouldBe DateTime.now.getDayOfMonth
      result.getHourOfDay shouldBe 7
      result.getMinuteOfHour shouldBe 0
    }
  }

  "schedulerEndTime" should {
    val schedulerService: SchedulerService = new SchedulerService(
      mockApplicationConfig,
      mockRepositories,
      mockResubPresubmissionService,
      mockSchedulerLoggingAndAuditing,
      mockActorSystem)

    "return current date with given time" in {
      val result = schedulerService.schedulerEndTime
      result.getYear shouldBe DateTime.now.getYear
      result.getMonthOfYear shouldBe DateTime.now.getMonthOfYear
      result.getDayOfMonth shouldBe DateTime.now.getDayOfMonth
      result.getHourOfDay shouldBe 18
      result.getMinuteOfHour shouldBe 0
    }
  }

  "resubmit" should {

    "return the result of processFailedGridFSSubmissions" in {
      val schedulerService: SchedulerService = new SchedulerService(
        mockApplicationConfig,
        mockRepositories,
        mockResubPresubmissionService,
        mockSchedulerLoggingAndAuditing,
        mockActorSystem)

      when(mockResubPresubmissionService.processFailedSubmissions())
        .thenReturn(Future.successful(Some(true)))

      val result = await(schedulerService.resubmit())
      result shouldBe Some(true)
    }

    "return false if resubmitting gridFS data throws exception" in {
      val schedulerService: SchedulerService = new SchedulerService(
        mockApplicationConfig,
        mockRepositories,
        mockResubPresubmissionService,
        mockSchedulerLoggingAndAuditing,
        mockActorSystem)

      when(mockResubPresubmissionService.processFailedSubmissions()).thenReturn(Future.failed(new RuntimeException))

      val result = await(schedulerService.resubmit())
      result shouldBe Some(false)
    }

  }
}
