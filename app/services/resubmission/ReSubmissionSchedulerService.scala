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
import models._
import play.api.Logging
import play.api.libs.json.JsObject
import play.api.mvc.Request
import repositories.LockRepositoryProvider
import scheduler.ScheduledService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.LockService
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing

import javax.inject.Inject
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class ReSubmissionSchedulerService @Inject()(val applicationConfig: ApplicationConfig,
                                             lockRepositoryProvider: LockRepositoryProvider,
                                             resubPresubmissionService: ResubPresubmissionService,
                                             schedulerLoggingAndAuditing: ErsLoggingAndAuditing)(implicit ec: ExecutionContext)
  extends ScheduledService[Boolean]
    with Logging
    with SchedulerConfig {

  override val jobName: String = "resubmission-service"
  private val resubmissionLimit = getResubmissionLimit(jobName)
  private val lockoutTimeout = getLockoutTimeout(jobName)
  private val lockService: LockService = LockService(lockRepositoryProvider.repo, lockId = "resubmission-service-job-lock",
    ttl = Duration.create(lockoutTimeout, SECONDS))
  implicit val processFailedSubmissionsConfig: ProcessFailedSubmissionsConfig = getProcessFailedSubmissionsConfig(resubmissionLimit)

  def resubmit()(implicit request: Request[_], hc: HeaderCarrier): Future[Boolean] = {
    schedulerLoggingAndAuditing.logInfo(ResubmissionLimitMessage(resubmissionLimit).message)
    resubPresubmissionService.processFailedSubmissions().map { result: Boolean =>
      schedulerLoggingAndAuditing.handleResult(
        result,
        ResubmissionSuccessMessage.message,
        ResubmissionFailedMessage.message
      )
      result
    }.recover {
      case ex: Exception =>
       schedulerLoggingAndAuditing.handleException("Resubmission failed with Exception", ex, "SchedulerService.resubmit")
        false
    }
  }

  override def invoke(implicit ec: ExecutionContext): Future[Boolean] = {
    val request: Request[JsObject] = ERSRequest.createERSRequest()
    val hc: HeaderCarrier = HeaderCarrier()
    resubPresubmissionService.logAggregateMetadataMetrics()
    resubPresubmissionService.logFailedSubmissionCount()
    schedulerLoggingAndAuditing.logInfo(LockMessage(lockService).message)
    lockService
      .withLock(resubmit()(request, hc)).map {
      case Some(_) => schedulerLoggingAndAuditing.logInfo(FinishedResubmissionJob.message)
        true
      case None => false
    }

  }
}
