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

import common.ERSEnvelope
import common.ERSEnvelope.ERSEnvelope
import config.ApplicationConfig
import models._
import play.api.Logging
import play.api.libs.json.JsObject
import play.api.mvc.Request
import repositories.LockRepositoryProvider
import scheduler.ScheduledService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.LockService

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class ReSubmissionSchedulerService @Inject()(val applicationConfig: ApplicationConfig,
                                             lockRepositoryProvider: LockRepositoryProvider,
                                             resubPresubmissionService: ResubPresubmissionService)(implicit ec: ExecutionContext)
  extends ScheduledService[Boolean]
    with Logging
    with SchedulerConfig {

  override val jobName: String = "resubmission-service"
  private val resubmissionLimit = getResubmissionLimit(jobName)
  private val lockoutTimeout = getLockoutTimeout(jobName)
  private val lockService: LockService = LockService(lockRepositoryProvider.repo, lockId = "resubmission-service-job-lock",
    ttl = Duration.create(lockoutTimeout, SECONDS))
  implicit val processFailedSubmissionsConfig: ProcessFailedSubmissionsConfig = getProcessFailedSubmissionsConfig(resubmissionLimit)

  def resubmit()(implicit request: Request[_], hc: HeaderCarrier): ERSEnvelope[Boolean] = {
    logger.info(ResubmissionLimitMessage(resubmissionLimit).message)
    resubPresubmissionService.processFailedSubmissions(processFailedSubmissionsConfig).map { result =>
      if (result) {
        logger.info(ResubmissionSuccessMessage.message)
      } else {
        logger.error(ResubmissionFailedMessage.message)
      }
      result
    }
  }

  override def invoke(implicit ec: ExecutionContext): ERSEnvelope[Boolean] = {
    val request: Request[JsObject] = ERSRequest.createERSRequest()
    implicit val hc: HeaderCarrier = HeaderCarrier()
    resubPresubmissionService.logAggregateMetadataMetrics()
    resubPresubmissionService.logFailedSubmissionCount(processFailedSubmissionsConfig)
    logger.info(LockMessage(lockService).message)

    ERSEnvelope.fromFuture(lockService
      .withLock(resubmit()(request, hc).value).map {
      case Some(_) =>
        logger.info(FinishedResubmissionJob.message)
        true
      case None =>
        false
    })
  }
}
