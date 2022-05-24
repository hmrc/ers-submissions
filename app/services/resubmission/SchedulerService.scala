/*
 * Copyright 2022 HM Revenue & Customs
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

import akka.actor.{ActorSystem, Cancellable}
import config.ApplicationConfig
import org.joda.time.DateTime
import play.api.Logging
import play.api.libs.json.JsObject
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.{MongoLockRepository, TimePeriodLockService}
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing

import javax.inject.Inject
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class SchedulerService @Inject()(val applicationConfig: ApplicationConfig,
                                 lockRepository: MongoLockRepository,
                                 resubPresubmissionService: ResubPresubmissionService,
                                 schedulerLoggingAndAuditing: ErsLoggingAndAuditing,
                                 actorSystem: ActorSystem)(implicit ec: ExecutionContext) extends SchedulerConfig with Logging {

  val request: Request[JsObject] = ERSRequest.createERSRequest()
  val hc: HeaderCarrier = HeaderCarrier()

  def run(): Cancellable = {
    val lock = TimePeriodLockService(lockRepository, applicationConfig.schedulerLockName, applicationConfig.schedulerLockExpireMin.minutes)
    actorSystem.scheduler.schedule(delay.milliseconds, repeat.seconds) {
      if ((schedulerStartTime.isEqualNow || schedulerStartTime.isBeforeNow) && schedulerEndTime.isAfterNow) {
        lock.withRenewedLock {
          logger.info(s"Start scheduling ${DateTime.now.toString}")
          resubmit()(request, hc)
        }
      }
    }
  }

  def resubmit()(implicit request: Request[_], hc: HeaderCarrier): Future[Option[Boolean]] = {
    resubPresubmissionService.processFailedSubmissions().map { result =>
      schedulerLoggingAndAuditing.handleResult(result, Some("Resubmission was successful"), Some("Resubmission failed"))
      result
    }.recover {
      case ex: Exception =>
       schedulerLoggingAndAuditing.handleException("Resubmission failed with Exception", ex, "SchedulerService.resubmit")
        Some(false)
    }
  }

}
