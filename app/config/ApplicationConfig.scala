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

package config

import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject
import scala.util.Try

class ApplicationConfig @Inject() (serviceConfig: ServicesConfig) {

  lazy val presubmissionCollection: String = serviceConfig.getString("settings.presubmission-collection")
  lazy val presubmissionCollectionTTL: Int = serviceConfig.getInt("settings.presubmission-collection-ttl-days")

  lazy val presubmissionCollectionIndexReplace: Boolean =
    serviceConfig.getBoolean("settings.presubmission-collection-index-replace")

  lazy val metadataCollection: String = serviceConfig.getString("settings.metadata-collection")
  lazy val metadataCollectionTTL: Int = serviceConfig.getInt("settings.metadata-collection-ttl-days")

  lazy val metadataCollectionIndexReplace: Boolean =
    serviceConfig.getBoolean("settings.metadata-collection-index-replace")

  lazy val uploadFileSizeLimit: Int   = serviceConfig.getInt("file-size.uploadSizeLimit")
  lazy val maxGroupSize: Int          = serviceConfig.getInt("file-size.maxGroupSize")
  // submissionParallelism refers to the number of threads used while submitting the file to the repository.
  lazy val submissionParallelism: Int = serviceConfig.getInt("file-size.submitParallelism")

  lazy val adrBaseURI: String           = serviceConfig.baseUrl("ers-stub")
  lazy val adrFullSubmissionURI: String = serviceConfig.getString("microservice.services.ers-stub.full-submission-url")
  lazy val UrlHeaderEnvironment: String = serviceConfig.getString("microservice.services.ers-stub.environment")

  lazy val UrlHeaderAuthorization: String =
    s"Bearer ${serviceConfig.getString("microservice.services.ers-stub.authorization-token")}"

  def schedulerSchemeRefListEnabled(jobName: String): Boolean =
    serviceConfig.getBoolean(s"schedules.$jobName.schemaRefsFilter.enabled")

  def schedulerSchemeRefList(jobName: String): List[String] =
    Try(serviceConfig.getString(s"schedules.$jobName.schemaRefsFilter.filter").split(",").toList)
      .getOrElse(List())

  def schedulerSchemeRefStatusList(jobName: String): List[String] =
    Try(serviceConfig.getString(s"schedules.$jobName.resubmit-list-statuses").split(",").toList)
      .getOrElse(List())

  def schedulerSchemeRefFailStatus(jobName: String): String =
    serviceConfig.getString(s"schedules.$jobName.resubmit-fail-status")

  def schedulerEnableResubmitByScheme(jobName: String): Boolean =
    serviceConfig.getBoolean(s"schedules.$jobName.schemaFilter.enabled")

  def schedulerResubmitScheme(jobName: String): String =
    serviceConfig.getString(s"schedules.$jobName.schemaFilter.filter")

  def schedulerSuccessStatus(jobName: String): String =
    serviceConfig.getString(s"schedules.$jobName.resubmit-successful-status")

  def schedulerEnableAdditionalLogs(jobName: String): Boolean =
    serviceConfig.getBoolean(s"schedules.$jobName.additional-logs.enabled")

  //  Date filter parameters
  def dateTimeFilterEnabled(jobName: String): Boolean =
    serviceConfig.getBoolean(s"schedules.$jobName.dateTimeFilter.enabled")

  def dateFilter(jobName: String): Option[String] =
    if (dateTimeFilterEnabled(jobName)) {
      Some(serviceConfig.getString(s"schedules.$jobName.dateTimeFilter.filter"))
    } else {
      None
    }

  // Presubmission with missing metadata query
  lazy val dateTimeFilterForQuery: String =
    serviceConfig.getString(s"schedules.generate-pre-sub-without-metadata-query.date-time-filter")

  lazy val maxNumberOfRecordsToReturn: Int =
    serviceConfig.getInt(s"schedules.generate-pre-sub-without-metadata-query.max-records")

  def lockoutTimeout(jobName: String): Int    = serviceConfig.getInt(s"schedules.$jobName.lockTimeout")
  def resubmissionLimit(jobName: String): Int = serviceConfig.getInt(s"schedules.$jobName.resubmissionLimit")
}
