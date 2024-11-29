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
import models.{GeneratingViewFailed, PreSubWithoutMetadata}
import org.mongodb.scala.{MongoCollection, SingleObservable}
import repositories.PreSubWithoutMetadataView
import scheduler.ScheduledService
import utils.LoggingAndRexceptions.ErsLogger

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

class PresSubWithoutMetadataViewService @Inject()(preSubWithoutMetadataView: PreSubWithoutMetadataView)
  extends ScheduledService[Unit]
    with ErsLogger {

  override val jobName: String = "pres-sub-without-metadata-view-service"

  override def invoke(implicit ec: ExecutionContext): ERSEnvelope[Unit] = {
    for {
      viewRecords: MongoCollection[PreSubWithoutMetadata] <- preSubWithoutMetadataView.initView
      numberOfRecords: SingleObservable[Long] = viewRecords.countDocuments()
      _ = numberOfRecords.subscribe(
        (count: Long) => logger.info(s"Number of records: $count"), // TODO: If the count is below a threshold we should try and log out the fields etc
        (error: Throwable) => logger.info(s"Failed with error: ${error.getMessage}"),
        () => logger.info("Counting completed.")
      )
    } yield ()
    ERSEnvelope[Unit](logger.info("Finished running pres-sub-without-metadata-view-service"))
  }
}
