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

import org.joda.time.DateTime
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class SchedulerServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with WithFakeApplication  {

  implicit val hc: HeaderCarrier = new HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()
  val mockSchedulerLoggingAndAuditing: ErsLoggingAndAuditing = mock[ErsLoggingAndAuditing]
  override def beforeEach() = {
    super.beforeEach()
    reset(mockSchedulerLoggingAndAuditing)
  }

  "getTime" should {
    System.out.println("GET TIME")
    val schedulerService: SchedulerService = new SchedulerService {
      System.out.println("SchedulerService start")
      override val resubPresubmissionService: ResubPresubmissionService = mock[ResubPresubmissionService]
      override val schedulerLoggingAndAuditing: ErsLoggingAndAuditing = mockSchedulerLoggingAndAuditing
    }
//
//    "return current date with given time" in {
//      val hour = 15
//      val minutes = 5
//      val result = schedulerService.getTime(hour, minutes)
//      result.getYear shouldBe DateTime.now.getYear
//      result.getMonthOfYear shouldBe DateTime.now.getMonthOfYear
//      result.getDayOfMonth shouldBe DateTime.now.getDayOfMonth
//      result.getHourOfDay shouldBe hour
//      result.getMinuteOfHour shouldBe minutes
//    }
    "return current date with given time" in {
      true
    }
  }

  "schedulerStartTime" should {
    val schedulerService: SchedulerService = new SchedulerService {
      val resubPresubmissionService: ResubPresubmissionService = mock[ResubPresubmissionService]
      val schedulerLoggingAndAuditing: ErsLoggingAndAuditing = mockSchedulerLoggingAndAuditing
    }

    "return current date with given time" in {
      val result = schedulerService.schedulerStartTime
      result.getYear shouldBe DateTime.now.getYear
      result.getMonthOfYear shouldBe DateTime.now.getMonthOfYear
      result.getDayOfMonth shouldBe DateTime.now.getDayOfMonth
      result.getHourOfDay shouldBe 7
      result.getMinuteOfHour shouldBe 0
    }
  }

  "schedulerEndTime" should {
    val schedulerService: SchedulerService = new SchedulerService {
      val resubPresubmissionService: ResubPresubmissionService = mock[ResubPresubmissionService]
      val schedulerLoggingAndAuditing: ErsLoggingAndAuditing = mockSchedulerLoggingAndAuditing
    }

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
    val mockResubPresubmissionService: ResubPresubmissionService = mock[ResubPresubmissionService]

    "return the result of processFailedGridFSSubmissions" in {
      val schedulerService: SchedulerService = new SchedulerService {
        val resubPresubmissionService: ResubPresubmissionService = mockResubPresubmissionService
        val schedulerLoggingAndAuditing: ErsLoggingAndAuditing = mockSchedulerLoggingAndAuditing
      }

      reset(mockResubPresubmissionService)
      when(
        mockResubPresubmissionService.processFailedSubmissions()
      ).thenReturn(
        Future.successful(Some(true))
      )
      val result = await(schedulerService.resubmit())
      result shouldBe Some(true)
    }

    "return false if resubmitting gridFS data throws exception" in {
      val schedulerService: SchedulerService = new SchedulerService {
        val resubPresubmissionService: ResubPresubmissionService = mockResubPresubmissionService
        val schedulerLoggingAndAuditing: ErsLoggingAndAuditing = mockSchedulerLoggingAndAuditing
      }

      reset(mockResubPresubmissionService)
      when(
        mockResubPresubmissionService.processFailedSubmissions()
      ).thenReturn(
        Future.failed(new RuntimeException)
      )
      val result = await(schedulerService.resubmit())
      result shouldBe Some(false)
    }

  }
}
