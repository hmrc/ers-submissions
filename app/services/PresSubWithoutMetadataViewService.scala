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
import models.PreSubWithoutMetadata
import org.mongodb.scala.MongoCollection
import repositories.PreSubWithoutMetadataView
import scheduler.ScheduledService
import utils.LoggingAndRexceptions.ErsLogger

import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

class PresSubWithoutMetadataViewService @Inject()(preSubWithoutMetadataView: PreSubWithoutMetadataView)
  extends ScheduledService[Unit]
    with ErsLogger {
  override val jobName: String = "pres-sub-without-metadata-view-service"

  override def invoke(implicit ec: ExecutionContext): ERSEnvelope[Unit] = {
    for {
      viewRecords: MongoCollection[PreSubWithoutMetadata] <- preSubWithoutMetadataView.initView
      numberOfRecords: Long <- viewRecords.countDocuments().toFuture()
      _ = if (numberOfRecords < 50) {
        logPresubmissionRecordsWithoutMetadata(viewRecords)
      } else {
        logger.info(s"Number of records > 50, $numberOfRecords records returned")
      }
    } yield ()
    ERSEnvelope[Unit](logger.info("Finished running pres-sub-without-metadata-view-service"))
  }

  private def logPresubmissionRecordsWithoutMetadata(
                                viewRecords: MongoCollection[PreSubWithoutMetadata]
                              )(implicit ec: ExecutionContext): Future[Unit] = {
    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

    viewRecords
      .find()
      .toFuture()
      .flatMap { documents: Seq[PreSubWithoutMetadata] =>
        Future {
          val logLines: Seq[String] = documents
            .map(document =>
              s"schemeRef: ${document.schemeRef}," +
                s"taxYear: ${document.taxYear}," +
                s"timestamp: ${formatter.format(Instant.ofEpochMilli(document.timestamp))}"
            )
          logger.info(s"[PresSubWithoutMetadataViewService] Presubmission data without metadata: " +
            s"${logLines.mkString("\n", "\n", "\n")}}"
          )
        }
      }
      .recover { case e: Throwable =>
        logger.error(s"Failed to fetch or log records: ${e.getMessage}")
      }
  }
}