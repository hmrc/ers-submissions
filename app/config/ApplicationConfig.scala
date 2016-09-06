/*
 * Copyright 2016 HM Revenue & Customs
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

  lazy val presubmissionCollection = Try(loadConfig(s"$env.settings.presubmission-collection")).getOrElse("")
  lazy val metadataCollection = Try(loadConfig(s"$env.settings.metadata-collection")).getOrElse("")
  lazy val gridFSCollection = Try(loadConfig(s"$env.settings.gridfs-collection")).getOrElse("")
  lazy val adrBaseURI: String = baseUrl("ers-stub")
  lazy val adrFullSubmissionURI: String = config("ers-stub").getString("full-submission-url").get
  lazy val UrlHeaderEnvironment: String = config("ers-stub").getString("environment").get
  lazy val UrlHeaderAuthorization: String = s"Bearer ${config("ers-stub").getString("authorization-token").get}"

  lazy val isSchedulerEnabled = Try(loadConfig(s"$env.scheduling.enabled").toBoolean).getOrElse(false)
  lazy val schedulerStatuses = Try(loadConfig(s"$env.scheduling.statuses").split(",").toList).getOrElse(List())
  lazy val schedulerRepeatIntervalInSeconds = Try(loadConfig(s"$env.scheduling.repeat-interval-sec").toInt).getOrElse(60)
  lazy val schedulerMaxRepeatIntervalInSeconds = Try(loadConfig(s"$env.scheduling.max-repeat-interval-sec").toInt).getOrElse(60)
  lazy val schedulerInitialDelayInMilliseconds = Try(loadConfig(s"$env.scheduling.initial-delay-ms").toInt).getOrElse(10000)
  lazy val schedulerMaxDelayInMilliseconds = Try(loadConfig(s"$env.scheduling.max-delay-ms").toInt).getOrElse(10000)
  lazy val schedulerStartHour = Try(loadConfig(s"$env.scheduling.start-hour").toInt).getOrElse(0)
  lazy val schedulerStartMinute = Try(loadConfig(s"$env.scheduling.start-minute").toInt).getOrElse(0)
  lazy val schedulerEndHour = Try(loadConfig(s"$env.scheduling.end-hour").toInt).getOrElse(0)
  lazy val schedulerEndMinute = Try(loadConfig(s"$env.scheduling.end-minute").toInt).getOrElse(0)
  lazy val schedulerLockExpireMin = Try(loadConfig(s"$env.scheduling.lock-expire-min").toInt).getOrElse(15)
  lazy val schedulerLockName = Try(loadConfig(s"$env.scheduling.lock-name")).getOrElse("ers-resubmission")

  lazy val schedulerSchemeRefListEnabled = Try(loadConfig(s"$env.scheduling.resubmit-list-enable").toBoolean).getOrElse(false)
  lazy val schedulerSchemeRefList: List[String] = Try(loadConfig(s"$env.scheduling.resubmit-list-schemeRefs").split(",").toList).getOrElse(List())
  lazy val schedulerSchemeRefStatusList: List[String] = Try(loadConfig(s"$env.scheduling.resubmit-list-statuses").split(",").toList).getOrElse(List())
  lazy val schedulerSchemeRefFailStatus: String = Try(loadConfig(s"$env.scheduling.resubmit-list-failStatus")).getOrElse("failedScheduler")
  lazy val schedulerSync: Boolean = Try(loadConfig(s"$env.scheduling.resubmit-list-sync").toBoolean).getOrElse(false)

  lazy val monitoringEnabled = Try(loadConfig(s"$env.monitoring.enable").toBoolean).getOrElse(false)
  lazy val monitoringStatuses = Try(loadConfig(s"$env.monitoring.statuses").split(",").toList).getOrElse(List())
  lazy val monitoringPresubmission = Try(loadConfig(s"$env.monitoring.check-presubmission.enable").toBoolean).getOrElse(false)
  lazy val monitoringPresubmissionInitDay = Try(loadConfig(s"$env.monitoring.check-presubmission.initial-date.day").toInt).getOrElse(23)
  lazy val monitoringPresubmissionInitMonth = Try(loadConfig(s"$env.monitoring.check-presubmission.initial-date.month").toInt).getOrElse(6)
  lazy val monitoringPresubmissionInitYear = Try(loadConfig(s"$env.monitoring.check-presubmission.initial-date.year").toInt).getOrElse(2016)
  lazy val monitoringPresubmissionPeriodDays = Try(loadConfig(s"$env.monitoring.check-presubmission.period-days").toInt).getOrElse(1)

  lazy val syncCompleteStoreInfo = Try(loadConfig(s"$env.monitoring.check-store-info.enabled").toBoolean).getOrElse(false)
  lazy val syncCompleteStoreInfoInitDay = Try(loadConfig(s"$env.monitoring.check-store-info.initial-date.day").toInt).getOrElse(23)
  lazy val syncCompleteStoreInfoInitMonth = Try(loadConfig(s"$env.monitoring.check-store-info.initial-date.month").toInt).getOrElse(6)
  lazy val syncCompleteStoreInfoInitYear = Try(loadConfig(s"$env.monitoring.check-store-info.initial-date.year").toInt).getOrElse(2016)
  lazy val syncCompleteStoreInfoFinalDay = Try(loadConfig(s"$env.monitoring.check-store-info.final-date.day").toInt).getOrElse(23)
  lazy val syncCompleteStoreInfoFinalMonth = Try(loadConfig(s"$env.monitoring.check-store-info.final-date.month").toInt).getOrElse(6)
  lazy val syncCompleteStoreInfoFinalYear = Try(loadConfig(s"$env.monitoring.check-store-info.final-date.year").toInt).getOrElse(2016)

  lazy val EnableRetrieveSubmissionData: Boolean = Try(loadConfig(s"$env.settings.enable-retrieve-submission-data").toBoolean).getOrElse(false)
}
