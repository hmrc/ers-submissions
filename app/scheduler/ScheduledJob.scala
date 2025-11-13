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

package scheduler

import org.apache.pekko.actor.{ActorRef, ActorSystem}
import org.apache.pekko.extension.quartz.QuartzSchedulerExtension
import org.quartz.CronExpression
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import scheduler.SchedulingActor.ScheduledMessage
import utils.LoggingAndExceptions.ErsLogger

import java.time.ZoneId
import java.util.TimeZone
import scala.concurrent.Future

trait ScheduledJob extends ErsLogger {
  val scheduledMessage: ScheduledMessage[_]
  val config: Configuration
  val actorSystem: ActorSystem

  def jobName: String

  val applicationLifecycle: ApplicationLifecycle

  lazy val scheduler: QuartzSchedulerExtension = QuartzSchedulerExtension(actorSystem)

  private lazy val schedulingActorRef: ActorRef = actorSystem.actorOf(SchedulingActor.props())

  private[scheduler] lazy val enabled: Boolean = config.getOptional[Boolean](s"schedules.$jobName.enabled").getOrElse(false)

  private lazy val description: Option[String] = config.getOptional[String](s"schedules.$jobName.description")

  private[scheduler] lazy val expression: String = config.getOptional[String](s"schedules.$jobName.expression") map (_.replaceAll("_", " ")) getOrElse ""

  private lazy val timezone: String = config.getOptional[String](s"schedules.$jobName.timezone").getOrElse(TimeZone.getDefault.getID)

  private[scheduler] lazy val isValid = expression.nonEmpty && CronExpression.isValidExpression(expression)

  lazy val schedule: Boolean = {
    (enabled, isValid) match {
      case (true, true) =>
        scheduler.createSchedule(jobName, description, expression, None, TimeZone.getTimeZone(ZoneId.of(timezone)))
        scheduler.schedule(jobName, schedulingActorRef, scheduledMessage)
        logInfo(s"Scheduler for $jobName has been started")
        true
      case (true, false) =>
        logInfo(s"Scheduler for $jobName is disabled as there is no quartz expression or expression is not valid")
        false
      case (false, _) =>
        logInfo(s"Scheduler for $jobName is disabled by configuration")
        false
    }
  }

  applicationLifecycle.addStopHook { () =>
    Future.successful(scheduler.cancelJob(jobName))
    Future.successful(scheduler.shutdown(waitForJobsToComplete = false))
  }
}
