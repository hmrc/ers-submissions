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

import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject
import scala.util.Try

class ApplicationConfig @Inject()(serviceConfig: ServicesConfig) {

  lazy val presubmissionCollection: String = serviceConfig.getString("settings.presubmission-collection")
  lazy val presubmissionCollectionTTL: Int = serviceConfig.getInt("settings.presubmission-collection-ttl-days")
  lazy val presubmissionCollectionIndexReplace: Boolean = serviceConfig.getBoolean("settings.presubmission-collection-index-replace")

  lazy val metadataCollection: String = serviceConfig.getString("settings.metadata-collection")
  lazy val metadataCollectionTTL: Int = serviceConfig.getInt("settings.metadata-collection-ttl-days")
  lazy val metadataCollectionIndexReplace: Boolean = serviceConfig.getBoolean("settings.metadata-collection-index-replace")

  lazy val uploadFileSizeLimit: Int = serviceConfig.getInt("file-size.uploadSizeLimit")
  lazy val maxGroupSize: Int = serviceConfig.getInt("file-size.maxGroupSize")
  //submissionParallelism refers to the number of threads used while submitting the file to the repository.
  lazy val submissionParallelism: Int = serviceConfig.getInt("file-size.submitParallelism")

  lazy val adrBaseURI: String = serviceConfig.baseUrl("ers-stub")
  lazy val adrFullSubmissionURI: String = serviceConfig.getString("microservice.services.ers-stub.full-submission-url")
  lazy val UrlHeaderEnvironment: String = serviceConfig.getString("microservice.services.ers-stub.environment")
  lazy val UrlHeaderAuthorization: String = s"Bearer ${serviceConfig.getString("microservice.services.ers-stub.authorization-token")}"

  lazy val schedulerSchemeRefListEnabled: Boolean = serviceConfig.getBoolean("schedules.resubmission-service.schemaRefsFilter.enabled")
  lazy val schedulerSchemeRefList: List[String] = Try(serviceConfig.getString("schedules.resubmission-service.schemaRefsFilter.filter").split(",").toList)
    .getOrElse(List())
  lazy val schedulerSchemeRefStatusList: List[String] = Try(serviceConfig.getString("schedules.resubmission-service.resubmit-list-statuses").split(",").toList)
    .getOrElse(List())
  lazy val schedulerSchemeRefFailStatus: String = serviceConfig.getString("schedules.resubmission-service.resubmit-fail-status")

  lazy val schedulerEnableResubmitByScheme: Boolean = serviceConfig.getBoolean("schedules.resubmission-service.schemaFilter.enabled")
  lazy val schedulerResubmitScheme: String = serviceConfig.getString("schedules.resubmission-service.schemaFilter.filter")
  lazy val schedulerSuccessStatus: String = serviceConfig.getString("schedules.resubmission-service.resubmit-successful-status")
  lazy val schedulerEnableAdditionalLogs: Boolean = serviceConfig.getBoolean("schedules.resubmission-service.additional-logs.enabled")

  //  Date filter parameters
  lazy val dateTimeFilterEnabled: Boolean = serviceConfig.getBoolean(s"schedules.resubmission-service.dateTimeFilter.enabled")
  lazy val dateFilter: Option[String] =
    if (dateTimeFilterEnabled) {
      Some(serviceConfig.getString(s"schedules.resubmission-service.dateTimeFilter.filter"))
    } else {
      None
    }

  // Presubmission with missing metadata query
  lazy val dateTimeFilterForQuery: String = serviceConfig.getString(s"schedules.generate-pre-sub-without-metadata-query.date-time-filter")
  lazy val maxNumberOfRecordsToReturn: Int = serviceConfig.getInt(s"schedules.generate-pre-sub-without-metadata-query.max-records")

  // Confirmation date time migration
  lazy val confirmationDateTimeMigrationBatchSize: Int = Try(serviceConfig.getInt("schedules.confirmation-date-time-migration.max-records")).getOrElse(1000)

  // Created at migration
  lazy val createdAtMigrationBatchSize: Int = Try(serviceConfig.getInt("schedules.created-at-migration.max-records")).getOrElse(1000)

  def lockoutTimeout(jobName: String): Int = serviceConfig.getInt(s"schedules.$jobName.lockTimeout")
  def resubmissionLimit(jobName: String): Int = serviceConfig.getInt(s"schedules.$jobName.resubmissionLimit")
}
