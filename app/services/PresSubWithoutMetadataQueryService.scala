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

package services

import common.ERSEnvelope
import common.ERSEnvelope.ERSEnvelope
import config.ApplicationConfig
import models.PreSubWithoutMetadata
import repositories.PreSubWithoutMetadataQuery
import scheduler.ScheduledService
import utils.LoggingAndExceptions.ErsLogger

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

class PresSubWithoutMetadataQueryService @Inject()(val applicationConfig: ApplicationConfig,
                                                   preSubWithoutMetadataQuery: PreSubWithoutMetadataQuery)
  extends ScheduledService[Unit]
    with ErsLogger {
  override val jobName: String = "pres-sub-without-metadata-query-service"

  private val className = "PresSubWithoutMetadataQueryService"

  override def invoke(implicit ec: ExecutionContext): ERSEnvelope[Unit] = {

    val result: Future[Unit] = preSubWithoutMetadataQuery.runQuery.map {
      validationInfo => {
        val (validationErrors: List[String], validQueryRecords: List[PreSubWithoutMetadata]) = validationInfo

        if (validQueryRecords.length < applicationConfig.maxNumberOfRecordsToReturn) {
          logPresubmissionRecordsWithoutMetadata(validQueryRecords)
        } else {
          logInfo(s"[$className] Number of records > ${applicationConfig.maxNumberOfRecordsToReturn}, ${validQueryRecords.length} records returned from query")
        }

        if (validationErrors.nonEmpty) {
          logInfo(s"[$className] ${validationErrors.length} validation errors, showing first 10: ${validationErrors.take(10).mkString(", ")}")
        }
      }
    }.recover { case e: Exception =>
      logError(s"[$className] Failed to fetch or log records: ${e.getMessage}")
    }

    ERSEnvelope(result)
  }

  private def logPresubmissionRecordsWithoutMetadata(
                                                      queryResults: Seq[PreSubWithoutMetadata]
                                                    ): Unit = {
    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
    val logLines: Seq[String] = queryResults
      .map(document =>
        s"schemeRef: ${document.schemeRef}, " +
          s"taxYear: ${document.taxYear}, " +
          s"timestamp: ${formatter.format(Instant.ofEpochMilli(document.timestamp))}"
      )

    logInfo(s"[$className] Presubmission data without metadata:" +
      s"${logLines.mkString("\n", "\n", "\n")}"
    )
  }

}
