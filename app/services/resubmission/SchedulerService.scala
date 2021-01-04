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

import akka.actor.{ActorSystem, Cancellable}
import akka.actor.TypedActor.context
import config.ApplicationConfig
import javax.inject.Inject
import models.ResubmissionLock
import org.joda.time.{DateTime, Duration}
import play.api.Logger
import play.api.libs.json.JsObject
import play.api.mvc.Request
import repositories.Repositories

import scala.concurrent.ExecutionContext.Implicits.global
import concurrent.duration._
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing

class SchedulerService @Inject()(val applicationConfig: ApplicationConfig,
                                 repositories: Repositories,
                                 resubPresubmissionService: ResubPresubmissionService,
                                 schedulerLoggingAndAuditing: ErsLoggingAndAuditing,
                                 actorSystem: ActorSystem) extends SchedulerConfig {

  val request: Request[JsObject] = ERSRequest.createERSRequest()
  val hc: HeaderCarrier = HeaderCarrier()

  def run(): Cancellable = {
    val lock = ResubmissionLock(
      applicationConfig.schedulerLockName,
      new Duration(applicationConfig.schedulerLockExpireMin * 60000),
      repositories.lockRepository
    )
    actorSystem.scheduler.schedule(delay milliseconds, repeat seconds) {
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
      case ex: Exception =>
       schedulerLoggingAndAuditing.handleException("Resubmission failed with Exception", ex, "SchedulerService.resubmit")
        Some(false)
    }
  }

}
