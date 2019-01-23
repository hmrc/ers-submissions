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

import config.ApplicationConfig
import models.Statuses
import org.joda.time.{DateTimeZone, DateTime}

trait SchedulerConfig {

  val failedStatus: String = if(ApplicationConfig.schedulerSchemeRefListEnabled) {
    ApplicationConfig.schedulerSchemeRefFailStatus
  }
  else {
    Statuses.FailedScheduler.toString
  }

  val searchStatusList = if(ApplicationConfig.schedulerSchemeRefListEnabled) {
    ApplicationConfig.schedulerSchemeRefStatusList
  }
  else {
    ApplicationConfig.schedulerStatuses
  }

  val schemeRefList = if(ApplicationConfig.schedulerSchemeRefListEnabled) {
    Some(ApplicationConfig.schedulerSchemeRefList)
  }
  else {
    None
  }

  val resubmitBySchemeEnabled: Boolean = ApplicationConfig.schedulerEnableResubmitByScheme
  val resubmitScheme: Option[String] = if(resubmitBySchemeEnabled) {
    Some(ApplicationConfig.schedulerResubmitScheme)
  }
  else {
    None
  }

  val resubmitSuccessStatus: String = ApplicationConfig.schedulerSuccessStatus
  val resubmitWithNilReturn: Boolean = ApplicationConfig.schedulerResubmitWithNilReturn
  val isResubmitBeforeDate: Boolean = ApplicationConfig.isSchedulerResubmitBeforeDate

  val r = scala.util.Random
  val delay = r.nextInt(ApplicationConfig.schedulerMaxDelayInMilliseconds) + ApplicationConfig.schedulerInitialDelayInMilliseconds
  val repeat = r.nextInt(ApplicationConfig.schedulerMaxRepeatIntervalInSeconds) + ApplicationConfig.schedulerRepeatIntervalInSeconds

  def schedulerStartTime: DateTime = getTime(ApplicationConfig.schedulerStartHour, ApplicationConfig.schedulerStartMinute)

  def schedulerEndTime: DateTime = {
    val endTime = getTime(ApplicationConfig.schedulerEndHour, ApplicationConfig.schedulerEndMinute)
    if (ApplicationConfig.schedulerStartHour > ApplicationConfig.schedulerEndHour) {
      endTime.plusDays(1)
    }
    else {
      endTime
    }
  }

  def getTime(hour: Int, minuteOfHour: Int): DateTime = {
    DateTime.now.withZone(DateTimeZone.UTC).withHourOfDay(hour).withMinuteOfHour(minuteOfHour).withSecondOfMinute(0)
  }
}
