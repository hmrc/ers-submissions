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

package services.resubmission

import common.ERSEnvelope
import common.ERSEnvelope.ERSEnvelope
import config.ApplicationConfig
import messages._
import play.api.libs.json.JsObject
import play.api.mvc.Request
import repositories.LockRepositoryProvider
import scheduler.ScheduledService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.LockService
import utils.CorrelationIdHelper
import utils.LoggingAndExceptions.ErsLogger

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class ReSubmissionSchedulerService @Inject() (
  val applicationConfig: ApplicationConfig,
  lockRepositoryProvider: LockRepositoryProvider,
  resubPresubmissionService: ResubPresubmissionService
)(using ec: ExecutionContext)
    extends ScheduledService[Boolean] with ErsLogger with SchedulerConfig with CorrelationIdHelper {

  override val jobName: String  = "resubmission-service"
  private val resubmissionLimit = getResubmissionLimit(jobName)
  private val lockoutTimeout    = getLockoutTimeout(jobName)

  private val lockService: LockService = LockService(
    lockRepositoryProvider.repo,
    lockId = "resubmission-service-job-lock",
    ttl = Duration.create(lockoutTimeout, SECONDS)
  )

  given processFailedSubmissionsConfig: ProcessFailedSubmissionsConfig = getProcessFailedSubmissionsConfig(
    resubmissionLimit
  )

  def resubmit()(using request: Request[?], hc: HeaderCarrier): ERSEnvelope[Boolean] = {
    logInfo(ResubmissionLimitMessage(resubmissionLimit).message)
    resubPresubmissionService.processFailedSubmissions(processFailedSubmissionsConfig).map { result =>
      if (result) {
        logInfo(ResubmissionSuccessMessage.message)
      } else {
        logError(ResubmissionFailedMessage.message)
      }
      result
    }
  }

  override def invoke(using ec: ExecutionContext): ERSEnvelope[Boolean] = {
    val request: Request[JsObject] = ERSRequest.createERSRequest()
    given hc: HeaderCarrier = getOrCreateCorrelationID(request)

    logIfEnabled(applicationConfig.schedulerEnableAdditionalLogs(jobName)) {
      resubPresubmissionService
        .logAggregateMetadataMetrics()
        .map(message => logInfo(message))
      resubPresubmissionService
        .logFailedSubmissionCount(processFailedSubmissionsConfig)
        .map(message => logInfo(message))
      logInfo(LockMessage(lockService).message)
    }
    logIfEnabled(applicationConfig.schedulerSchemeRefListEnabled(jobName)) {
      resubPresubmissionService
        .getMetadataSelectedSchemeRefDetailsMessage(processFailedSubmissionsConfig)
        .map(message => logInfo(message))
      resubPresubmissionService
        .getPreSubSelectedSchemeRefDetailsMessage(processFailedSubmissionsConfig)
        .map(message => logInfo(message))
    }

    ERSEnvelope(lockService.withLock(resubmit()(using request, hc).value).map {
      case Some(_) =>
        logInfo(FinishedResubmissionJob.message)
        true
      case None    =>
        false
    })
  }

}
