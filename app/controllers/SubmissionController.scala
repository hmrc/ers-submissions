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

package controllers

import metrics.Metrics
import models._
import play.api.libs.json._
import play.api.mvc._
import services._
import services.audit.AuditEvents
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.LoggingAndExceptions.ErsLogger
import utils.{CorrelationIdHelper, ErrorHandlerHelper}

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SubmissionController @Inject()(submissionCommonService: SubmissionService,
                                     metadataService: MetadataService,
                                     metrics: Metrics,
                                     auditEvents: AuditEvents,
                                     cc: ControllerComponents)
                                    (implicit val ec: ExecutionContext) extends BackendController(cc) with CorrelationIdHelper with ErsLogger with ErrorHandlerHelper {

  override val className: String = getClass.getSimpleName

  def receiveMetadataJson(): Action[JsObject] = Action.async(parse.json[JsObject]) {
    implicit request =>
      implicit val hc: HeaderCarrier = getOrCreateCorrelationID(request)

    metadataService.validateErsSummaryFromJson(request.body) match {
      case JsSuccess(ersSummary, _) =>
        (for {
          result <- submissionCommonService.callProcessData(ersSummary, Statuses.Failed.toString, Statuses.Sent.toString)(request, hc)
        } yield result).value.map {
          case Right(true) =>
            logInfo(s"[SubmissionController][receiveMetadataJson] Submission is successfully completed for: ${ersSummary.metaData.schemeInfo.basicLogMessage}")
            Ok
          case Right(false) =>
            auditEvents.sendToAdrEvent("ErsTransferToAdrFailed", ersSummary)
            logError(s"[SubmissionController][receiveMetadataJson] Processing data failed for: ${ersSummary.metaData.schemeInfo.basicLogMessage}")
            InternalServerError("Processing data failed.")
          case Left(error) =>
            auditEvents.sendToAdrEvent("ErsTransferToAdrFailed", ersSummary)
            logError(s"[SubmissionController][receiveMetadataJson] Processing data failed for: ${ersSummary.metaData.schemeInfo.basicLogMessage} with error: [$error]")
            InternalServerError("Processing data failed.")
        }
      case JsError(jsonErrors) => handleBadRequest(jsonErrors)
    }
  }

  def saveMetadata(): Action[JsObject] = Action.async(parse.json[JsObject]) { implicit request =>
    metadataService.validateErsSummaryFromJson(request.body) match {
      case JsSuccess(ersSummary, _) =>
        val startTime = System.currentTimeMillis()
        metadataService.storeErsSummary(ersSummary).value.map {
          case Right(true) =>
            metrics.saveMetadata(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
            logInfo(s"[SubmissionController][saveMetadata] ErsSummary is successfully saved, bundleRef: ${ersSummary.bundleRef}")
            Ok("Metadata is successfully stored.")
          case Right(false) =>
            logError(s"[SubmissionController][saveMetadata] Saving ErsSummary failed, bundleRef: ${ersSummary.bundleRef}, ${ersSummary.metaData.schemeInfo.basicLogMessage}")
            auditEvents.auditADRTransferFailure(ersSummary.metaData.schemeInfo, Map.empty)
            InternalServerError("Storing metadata failed.")
          case Left(error) =>
            logError(s"[SubmissionController][saveMetadata] Saving ErsSummary failed, bundleRef: ${ersSummary.bundleRef}, " +
              s"${ersSummary.metaData.schemeInfo.basicLogMessage} with error: [$error]")
            auditEvents.auditADRTransferFailure(ersSummary.metaData.schemeInfo, Map.empty)
            InternalServerError("Storing metadata failed.")
        }
      case JsError(jsonErrors) => handleBadRequest(jsonErrors)
    }
  }
}
