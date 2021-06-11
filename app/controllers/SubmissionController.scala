/*
 * Copyright 2021 HM Revenue & Customs
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

import java.util.concurrent.TimeUnit

import javax.inject.Inject
import metrics.Metrics
import models._
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.mvc._
import services._
import services.audit.AuditEvents
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing

import scala.concurrent.{ExecutionContext, Future}

class SubmissionController @Inject()(submissionCommonService: SubmissionService,
                                     metadataService: MetadataService,
                                     metrics: Metrics,
                                     ersLoggingAndAuditing: ErsLoggingAndAuditing,
                                     auditEvents: AuditEvents,
                                     cc: ControllerComponents)
                                    (implicit val ec: ExecutionContext) extends BackendController(cc) {

  def receiveMetadataJson(): Action[JsObject] = Action.async(parse.json[JsObject]) { implicit request =>
    ersLoggingAndAuditing.logWarn(s"Submission journey 1. received request: ${DateTime.now}")

    metadataService.validateErsSummaryFromJson(request.body) match {
      case Some(ersSummary: ErsSummary) =>
        ersLoggingAndAuditing.logWarn(s"Submission journey 2. validated request: ${DateTime.now}", Some(ersSummary))

        try {
          submissionCommonService.callProcessData(ersSummary, Statuses.Failed.toString, Statuses.Sent.toString).map{ _ =>
            ersLoggingAndAuditing.handleSuccess(ersSummary.metaData.schemeInfo, "Submission is successfully completed")
            Ok
          }.recover {
            case ex: Exception =>
              auditEvents.sendToAdrEvent("ErsTransferToAdrFailed", ersSummary)
              ersLoggingAndAuditing.handleException(ersSummary, ex, "Processing data for ADR exception")
              InternalServerError(s"Exception: ${ex.getMessage}.")
          }
        } catch {
          case ex: Exception =>
            Future {
              auditEvents.sendToAdrEvent("ErsTransferToAdrFailed", ersSummary)
              ersLoggingAndAuditing.handleException(ersSummary, ex, "Processing data for ADR exception")
              InternalServerError(s"Exception: ${ex.getMessage}.")
            }
        }
      case _ => Future.successful(BadRequest("Invalid json."))
    }
  }

  def saveMetadata(): Action[JsObject] = Action.async(parse.json[JsObject]) { implicit request =>
    metadataService.validateErsSummaryFromJson(request.body) match {
      case Some(ersSummary: ErsSummary) =>
        val startTime = System.currentTimeMillis()
        metadataService.storeErsSummary(ersSummary).map {
          case true =>
            metrics.saveMetadata(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
            ersLoggingAndAuditing.handleSuccess(ersSummary.metaData.schemeInfo, s"ErsSummary is successfully saved, bundleRef: ${ersSummary.bundleRef}")
            Ok("Metadata is successfully stored.")
          case false =>
            ersLoggingAndAuditing.handleFailure(ersSummary.metaData.schemeInfo, s"Saving ErsSummary failed, bundleRef: ${ersSummary.bundleRef}")
            InternalServerError("Storing metadata failed.")
        }
      case _ => Future.successful(BadRequest("Invalid json."))
    }
  }
}
