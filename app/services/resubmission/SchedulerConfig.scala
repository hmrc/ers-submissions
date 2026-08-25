/*
 * Copyright 2026 HM Revenue & Customs
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

trait SchedulerConfig {
  val applicationConfig: ApplicationConfig
  def jobName: String

  lazy val failedStatus: String = applicationConfig.schedulerSchemeRefFailStatus(jobName)

  lazy val searchStatusList: List[String] = applicationConfig.schedulerSchemeRefStatusList(jobName)

  lazy val schemeRefList: Option[List[String]] = if (applicationConfig.schedulerSchemeRefListEnabled(jobName)) {
    Some(applicationConfig.schedulerSchemeRefList(jobName))
  } else {
    None
  }

  lazy val resubmitScheme: Option[String] = if (applicationConfig.schedulerEnableResubmitByScheme(jobName)) {
    Some(applicationConfig.schedulerResubmitScheme(jobName))
  } else {
    None
  }

  lazy val resubmitSuccessStatus: String = applicationConfig.schedulerSuccessStatus(jobName)

  lazy val dateTimeFilter: Option[String] = applicationConfig.dateFilter(jobName)

  def getResubmissionLimit(jobName: String): Int = applicationConfig.resubmissionLimit(jobName)

  def getLockoutTimeout(jobName: String): Int = applicationConfig.lockoutTimeout(jobName)

  def getProcessFailedSubmissionsConfig(
    resubmissionLimit: Int,
    streamed: Boolean = false
  ): ProcessFailedSubmissionsConfig =
    ProcessFailedSubmissionsConfig(
      resubmissionLimit,
      searchStatusList,
      schemeRefList,
      resubmitScheme,
      dateTimeFilter,
      failedStatus,
      resubmitSuccessStatus,
      streamed
    )

}
