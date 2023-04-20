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

import config.ApplicationConfig
import models.Statuses
import org.joda.time.{DateTime, DateTimeZone}

import scala.util.Random

trait SchedulerConfig {
  val applicationConfig: ApplicationConfig

  val failedStatus: String = if(applicationConfig.schedulerSchemeRefListEnabled) {
    applicationConfig.schedulerSchemeRefFailStatus
  } else {
    Statuses.FailedScheduler.toString
  }

  val searchStatusList: List[String] = if(applicationConfig.schedulerSchemeRefListEnabled) {
    applicationConfig.schedulerSchemeRefStatusList
  } else {
    applicationConfig.schedulerStatuses
  }

  val schemeRefList: Option[List[String]] = if(applicationConfig.schedulerSchemeRefListEnabled) {
    Some(applicationConfig.schedulerSchemeRefList)
  } else {
    None
  }

  val resubmitBySchemeEnabled: Boolean = applicationConfig.schedulerEnableResubmitByScheme
  val resubmitScheme: Option[String] = if(resubmitBySchemeEnabled) {
    Some(applicationConfig.schedulerResubmitScheme)
  } else {
    None
  }

  val resubmitSuccessStatus: String = applicationConfig.schedulerSuccessStatus
  val isResubmitBeforeDate: Boolean = applicationConfig.isSchedulerResubmitBeforeDate
}
