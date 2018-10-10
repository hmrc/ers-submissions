/*
 * Copyright 2018 HM Revenue & Customs
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

package config

import play.api.Play._
import uk.gov.hmrc.play.config.ServicesConfig
import scala.util.Try
import scala.collection.JavaConversions._

object ApplicationConfig extends ServicesConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing key: $key"))

  lazy val presubmissionCollection = Try(loadConfig("settings.presubmission-collection")).getOrElse("")
  lazy val metadataCollection = Try(loadConfig("settings.metadata-collection")).getOrElse("")

  lazy val adrBaseURI: String = baseUrl("ers-stub")
  lazy val adrFullSubmissionURI: String = config("ers-stub").getString("full-submission-url").get
  lazy val UrlHeaderEnvironment: String = config("ers-stub").getString("environment").get
  lazy val UrlHeaderAuthorization: String = s"Bearer ${config("ers-stub").getString("authorization-token").get}"

  lazy val isSchedulerEnabled = Try(loadConfig("scheduling.enabled").toBoolean).getOrElse(false)
  lazy val schedulerStatuses = Try(loadConfig("scheduling.statuses").split(",").toList).getOrElse(List())
  lazy val schedulerRepeatIntervalInSeconds = Try(loadConfig("scheduling.repeat-interval-sec").toInt).getOrElse(60)
  lazy val schedulerMaxRepeatIntervalInSeconds = Try(loadConfig("scheduling.max-repeat-interval-sec").toInt).getOrElse(60)
  lazy val schedulerInitialDelayInMilliseconds = Try(loadConfig("scheduling.initial-delay-ms").toInt).getOrElse(10000)
  lazy val schedulerMaxDelayInMilliseconds = Try(loadConfig("scheduling.max-delay-ms").toInt).getOrElse(10000)
  lazy val schedulerStartHour = Try(loadConfig("scheduling.start-hour").toInt).getOrElse(0)
  lazy val schedulerStartMinute = Try(loadConfig("scheduling.start-minute").toInt).getOrElse(0)
  lazy val schedulerEndHour = Try(loadConfig("scheduling.end-hour").toInt).getOrElse(0)
  lazy val schedulerEndMinute = Try(loadConfig("scheduling.end-minute").toInt).getOrElse(0)
  lazy val schedulerLockExpireMin = Try(loadConfig("scheduling.lock-expire-min").toInt).getOrElse(15)
  lazy val schedulerLockName = Try(loadConfig("scheduling.lock-name")).getOrElse("ers-resubmission")

  lazy val schedulerSchemeRefListEnabled = Try(loadConfig("scheduling.resubmit-list-enable").toBoolean).getOrElse(false)
  lazy val schedulerSchemeRefList: List[String] = Try(loadConfig("scheduling.resubmit-list-schemeRefs").split(",").toList).getOrElse(List())
  lazy val schedulerSchemeRefStatusList: List[String] = Try(loadConfig("scheduling.resubmit-list-statuses").split(",").toList).getOrElse(List())
  lazy val schedulerSchemeRefFailStatus: String = Try(loadConfig("scheduling.resubmit-list-failStatus")).getOrElse("failedScheduler")

  lazy val schedulerEnableResubmitByScheme: Boolean = Try(loadConfig("scheduling.resubmit-scheme-enable").toBoolean).getOrElse(true)
  lazy val schedulerResubmitScheme: String = Try(loadConfig("scheduling.resubmit-scheme")).getOrElse("SAYE")
  lazy val schedulerSuccessStatus: String = Try(loadConfig("scheduling.resubmit-successful-status")).getOrElse("successResubmit")
  lazy val schedulerResubmitWithNilReturn: Boolean = Try(loadConfig("scheduling.resubmit-scheme-with-nil-returns").toBoolean).getOrElse(false)
  lazy val isSchedulerResubmitBeforeDate: Boolean = Try(loadConfig("scheduling.resubmit-scheme-before-date").toBoolean).getOrElse(true)

  lazy val defaultScheduleStartDate: String = Try(loadConfig("scheduling.default-resubmit-start-date")).getOrElse("2016-04-01")
  lazy val rescheduleStartDate: String = Try(loadConfig("scheduling.resubmit-start-date")).getOrElse("2016-04-01")
  lazy val scheduleEndDate: String = Try(loadConfig("scheduling.resubmit-end-date")).getOrElse("2016-04-01")
  lazy val scheduleStartDate:String = if(ApplicationConfig.isSchedulerResubmitBeforeDate){
    ApplicationConfig.defaultScheduleStartDate
  } else {
    ApplicationConfig.rescheduleStartDate
  }

  lazy val isErsQueryEnabled: Boolean = Try(loadConfig("ers-query.enabled").toBoolean).getOrElse(false)
  lazy val ersQuerySchemeType: String = Try(loadConfig("ers-query.schemetype")).getOrElse("SAYE")
  lazy val ersQueryStartDate: String = Try(loadConfig("ers-query.start-date")).getOrElse("2016-04-01")
  lazy val ersQueryEndDate: String = Try(loadConfig("ers-query.end-date")).getOrElse("2016-04-01")

}
