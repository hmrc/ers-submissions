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

package services.resubmission

import config.ApplicationConfig
import models.{FinishedResubmissionJob, LockMessage}
import play.api.Logging
import play.api.libs.json.JsObject
import play.api.mvc.Request
import repositories.LockRepositoryProvider
import scheduler.ScheduledService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.LockService
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing

import javax.inject.Inject
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class ReSubmissionSchedulerService @Inject()(lockRepositoryProvider: LockRepositoryProvider,
                                                    resubPresubmissionService: ResubPresubmissionService,
                                                    servicesConfig: ServicesConfig,
                                                    schedulerLoggingAndAuditing: ErsLoggingAndAuditing)(implicit ec: ExecutionContext)
  extends ScheduledService[Boolean] with Logging {

  override val jobName: String = "resubmission-service"
  private lazy val lockoutTimeout: Int = servicesConfig.getInt(s"schedules.$jobName.lockTimeout")
  private val lockService: LockService = LockService(lockRepositoryProvider.repo, lockId = "resubmission-service-job-lock",
    ttl = Duration.create(lockoutTimeout, SECONDS))

  def resubmit()(implicit request: Request[_], hc: HeaderCarrier): Future[Boolean] = {
    resubPresubmissionService.processFailedSubmissions().map { result: Option[Boolean] =>
      schedulerLoggingAndAuditing.handleResult(result, Some("Resubmission was successful"), Some("Resubmission failed"))
      result.getOrElse(false)
    }.recover {
      case ex: Exception =>
       schedulerLoggingAndAuditing.handleException("Resubmission failed with Exception", ex, "SchedulerService.resubmit")
        false
    }
  }

  override def invoke(implicit ec: ExecutionContext): Future[Boolean] = {
    val request: Request[JsObject] = ERSRequest.createERSRequest()
    val hc: HeaderCarrier = HeaderCarrier()
    logger.info(LockMessage(lockService).message)
    lockService
      .withLock(resubmit()(request, hc)).map {
      case Some(t: Boolean) => logger.info(FinishedResubmissionJob.message)
      true
      case None => false
    }

  }
}
