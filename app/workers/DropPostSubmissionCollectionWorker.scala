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

package workers

import cats.data.EitherT
import common.ERSEnvelope.ERSEnvelope
import scheduler.ScheduledService
import uk.gov.hmrc.mongo.MongoComponent
import utils.LoggingAndExceptions.ErsLogger

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DropPostSubmissionCollectionWorker @Inject()(mongoComponent: MongoComponent)
                                                   (implicit ec: ExecutionContext)
  extends ScheduledService[Unit] with ErsLogger {

  override val jobName: String = "drop-post-submission-collection-worker"

  private val collectionName = "ers-postsubmission"

  // Execute on startup
  logInfo(s"[$jobName] Triggering drop collection worker on application startup")
  invoke.value.map {
    case Right(_) =>
      logInfo(s"[$jobName] Drop collection worker completed successfully on startup")
    case Left(error) =>
      logWarn(s"[$jobName] Drop collection worker completed with error on startup: $error")
  }.recover { case ex =>
    logWarn(s"[$jobName] Drop collection worker failed with exception on startup: ${ex.getMessage}", ex)
  }

  override def invoke(implicit ec: ExecutionContext): ERSEnvelope[Unit] = {
    logInfo(s"[$jobName] Starting to check and drop collection: $collectionName")

    EitherT {
      checkAndDropCollection()
        .map { _ =>
          logInfo(s"[$jobName] Successfully ensured collection $collectionName is dropped")
          Right(())
        }
        .recover { case ex =>
          logWarn(s"[$jobName] Error while dropping collection $collectionName: ${ex.getMessage}", ex)
          Right(()) // Return success even if there's an error, as we want the app to start
        }
    }
  }

  private def checkAndDropCollection(): Future[Unit] = {
    val database = mongoComponent.database

    database.listCollectionNames()
      .toFuture()
      .flatMap { collections =>
        if (collections.contains(collectionName)) {
          logInfo(s"[$jobName] Collection $collectionName exists, dropping it...")
          database.getCollection(collectionName).drop().toFuture().map(_ => ())
        } else {
          logInfo(s"[$jobName] Collection $collectionName does not exist, nothing to drop")
          Future.successful(())
        }
      }
  }
}

