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

package config

import javax.inject.Inject
import models.ERSQuery
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.util.Try

class ApplicationConfig @Inject()(serviceConfig: ServicesConfig) {

  lazy val presubmissionCollection: String = serviceConfig.getString("settings.presubmission-collection")
  lazy val presubmissionCollectionTTL: Int = serviceConfig.getInt("settings.presubmission-collection-ttl-days")
  lazy val metadataCollection: String = serviceConfig.getString("settings.metadata-collection")
  lazy val uploadCsvSizeLimit: Int = serviceConfig.getInt("csv.uploadSizeLimit")
  lazy val maxGroupSize: Int = serviceConfig.getInt("csv.maxGroupSize")
  //submissionParallelism refers to the number of threads used while submitting the file to the repository.
  lazy val submissionParallelism: Int = serviceConfig.getInt("csv.submitParallelism")

  lazy val adrBaseURI: String = serviceConfig.baseUrl("ers-stub")
  lazy val adrFullSubmissionURI: String = serviceConfig.getString("microservice.services.ers-stub.full-submission-url")
  lazy val UrlHeaderEnvironment: String = serviceConfig.getString("microservice.services.ers-stub.environment")
  lazy val UrlHeaderAuthorization: String = s"Bearer ${serviceConfig.getString("microservice.services.ers-stub.authorization-token")}"

  lazy val isSchedulerEnabled: Boolean = serviceConfig.getBoolean("scheduling.enabled")
  lazy val schedulerStatuses: List[String] = Try(serviceConfig.getString("scheduling.statuses").split(",").toList).getOrElse(List())
  lazy val schedulerLockExpireMin: Int = serviceConfig.getInt("scheduling.lock-expire-min")
  lazy val schedulerLockName: String = serviceConfig.getString("scheduling.lock-name")

  lazy val schedulerSchemeRefListEnabled: Boolean = serviceConfig.getBoolean("scheduling.resubmit-list-enable")
  lazy val schedulerSchemeRefList: List[String] = Try(serviceConfig.getString("scheduling.resubmit-list-schemeRefs").split(",").toList).getOrElse(List())
  lazy val schedulerSchemeRefStatusList: List[String] = Try(serviceConfig.getString("scheduling.resubmit-list-statuses").split(",").toList).getOrElse(List())
  lazy val schedulerSchemeRefFailStatus: String = serviceConfig.getConfString("scheduling.resubmit-list-failStatus", "failedScheduler")

  lazy val schedulerEnableResubmitByScheme: Boolean = serviceConfig.getBoolean("scheduling.resubmit-scheme-enable")
  lazy val schedulerResubmitScheme: String = serviceConfig.getString("scheduling.resubmit-scheme")
  lazy val schedulerSuccessStatus: String = serviceConfig.getString("scheduling.resubmit-successful-status")
  lazy val isSchedulerResubmitBeforeDate: Boolean = serviceConfig.getBoolean("scheduling.resubmit-scheme-before-date")

  lazy val defaultScheduleStartDate: String = serviceConfig.getString("scheduling.default-resubmit-start-date")
  lazy val rescheduleStartDate: String = serviceConfig.getString("scheduling.resubmit-start-date")
  lazy val scheduleEndDate: String = serviceConfig.getString("scheduling.resubmit-end-date")
  lazy val scheduleStartDate:String = if(isSchedulerResubmitBeforeDate){
    defaultScheduleStartDate
  } else {
    rescheduleStartDate
  }

  lazy val isErsQueryEnabled: Boolean = serviceConfig.getBoolean("ers-query.enabled")
  lazy val ersQuerySchemeType: String = serviceConfig.getString("ers-query.schemetype")
  lazy val ersQueryStartDate: String = serviceConfig.getString("ers-query.start-date")
  lazy val ersQueryEndDate: String = serviceConfig.getString("ers-query.end-date")

  def ersQuery: ERSQuery = {
    ERSQuery(Some(ersQuerySchemeType),Some(ersQueryStartDate),Some(ersQueryEndDate),None,schedulerSchemeRefList)
  }
}
