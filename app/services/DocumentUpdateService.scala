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

import com.google.inject.Inject
import models.{FailedToLockRepositoryForUpdate, RepositoryUpdateMessage, UpdateRequestAcknowledged, UpdateRequestNothingToUpdate}
import play.api.Logging
import repositories.{LockRepositoryProvider, PresubmissionMongoRepository}
import scheduler.ScheduledService
import uk.gov.hmrc.mongo.lock.LockService
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{ExecutionContext, Future}

trait DocumentUpdateService extends ScheduledService[Long] with Logging
class DefaultDocumentUpdateService @Inject()(repository: PresubmissionMongoRepository,
                                             lockRepositoryProvider: LockRepositoryProvider,
                                             servicesConfig: ServicesConfig
                                            )(implicit ec: ExecutionContext) extends DocumentUpdateService {

  override val jobName: String = "update-created-at-field-job"
  private val updateLimit: Int = servicesConfig.getInt(s"schedules.$jobName.updateLimit")
  private lazy val lockoutTimeout: Int = servicesConfig.getInt(s"schedules.$jobName.lockTimeout")
  private val lockService: LockService = LockService(lockRepositoryProvider.repo, lockId = "update-created-at-job-lock",
    ttl = Duration.create(lockoutTimeout, SECONDS))

  private[services] def updateMissingCreatedAtFields(): Future[(Long, RepositoryUpdateMessage)] = {
    repository.getDocumentIdsWithoutCreatedAtField(updateLimit).flatMap { documentIds =>
      if(documentIds.nonEmpty) {
        for {
          updateResult <- repository.addCreatedAtField(documentIds)
        } yield updateResult match {
          case Left(error) => (0, error)
          case Right(result) => (result.getModifiedCount, UpdateRequestAcknowledged(result.getModifiedCount))

        }
      } else {
        Future.successful((0, UpdateRequestNothingToUpdate))
      }
    }
  }

  override def invoke(implicit ec: ExecutionContext): Future[Long] =
    lockService.withLock(updateMissingCreatedAtFields()) map {
      case Some((updatedCount, updateMessage)) =>
        logger.info("[DocumentUpdateService] - " + updateMessage.message)
        updatedCount
      case None =>
        logger.info("[DocumentUpdateService] - " + FailedToLockRepositoryForUpdate.message)
        0
    }
}
