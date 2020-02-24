/*
 * Copyright 2020 HM Revenue & Customs
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

import config.ApplicationConfig
import models.ResubmissionLock
import org.joda.time.{Duration, DateTime}
import play.api.Logger
import play.api.mvc.Request
import play.libs.Akka
import repositories.Repositories
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing
import scala.concurrent.ExecutionContext.Implicits.global
import concurrent.duration._
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

object SchedulerService extends SchedulerService {

  override val resubPresubmissionService: ResubPresubmissionService = ResubPresubmissionService
  override val schedulerLoggingAndAuditing: ErsLoggingAndAuditing = ErsLoggingAndAuditing
}

trait SchedulerService extends SchedulerConfig {
  val resubPresubmissionService: ResubPresubmissionService
  val schedulerLoggingAndAuditing: ErsLoggingAndAuditing
  val request = ERSRequest.createERSRequest()
  val hc: HeaderCarrier = new HeaderCarrier()

  def run() = {
    val lock = ResubmissionLock(ApplicationConfig.schedulerLockName, new Duration(ApplicationConfig.schedulerLockExpireMin * 60000), Repositories.lockRepository)
    Akka.system.scheduler.schedule(delay milliseconds, repeat seconds) {
      if ((schedulerStartTime.isEqualNow || schedulerStartTime.isBeforeNow) && schedulerEndTime.isAfterNow) {
        lock.tryToAcquireOrRenewLock {
          Logger.info(s"Start scheduling ${DateTime.now.toString}")
          resubmit()(request, hc).map { res => res }
        }
      }
    }
  }

  def resubmit()(implicit request: Request[_], hc: HeaderCarrier): Future[Option[Boolean]] = {
    resubPresubmissionService.processFailedSubmissions().map { result =>
      schedulerLoggingAndAuditing.handleResult(result, Some("Resubmission was successful"), Some("Resubmission failed"))
      result
    }.recover {
      case ex: Exception => {
        schedulerLoggingAndAuditing.handleException("Resubmission failed with Exception", ex, "SchedulerService.resubmit")
        Some(false)
      }
    }
  }

}
