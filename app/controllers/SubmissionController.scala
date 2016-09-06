/*
 * Copyright 2016 HM Revenue & Customs
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
import metrics.Metrics
import models._
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.mvc._
import services.audit.AuditEvents
import services._
import uk.gov.hmrc.play.microservice.controller.BaseController
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object SubmissionController extends SubmissionController {

  override val postsubmissionService: PostsubmissionService = PostsubmissionService
  override val metadataService: MetadataService = MetadataService
  override val metrics: Metrics = Metrics
  override val ersLoggingAndAuditing: ErsLoggingAndAuditing = ErsLoggingAndAuditing
  override val validationService: ValidationService = ValidationService

}

trait SubmissionController extends BaseController {

  val postsubmissionService: PostsubmissionService
  val metadataService: MetadataService
  val metrics: Metrics
  val ersLoggingAndAuditing: ErsLoggingAndAuditing
  val validationService: ValidationService

  def receiveMetadataJson = Action.async(parse.json[JsObject]) { implicit request =>
    ersLoggingAndAuditing.logWarn(s"Submission journey 1. received request: ${DateTime.now}")

    metadataService.validateErsSummaryFromJson(request.body) match {
      case Some(ersSummary: ErsSummary) => {
        ersLoggingAndAuditing.logWarn(s"Submission journey 2. validated request: ${DateTime.now}", Some(ersSummary))

        try {
          postsubmissionService.processDataForADR(ersSummary).map{ _ =>
            ersLoggingAndAuditing.handleSuccess(ersSummary.metaData.schemeInfo, "Submission is successfully completed")
            Ok
          }.recover {
            case ex: Exception => {
              AuditEvents.sendToAdrEvent("ErsTransferToAdrFailed", ersSummary)
              ersLoggingAndAuditing.handleException(ersSummary, ex, "Processing data for ADR exception")
              InternalServerError(s"Exception: ${ex.getMessage}.")
            }
          }
        }
        catch {
          case ex: Exception => {
            Future {
              AuditEvents.sendToAdrEvent("ErsTransferToAdrFailed", ersSummary)
              ersLoggingAndAuditing.handleException(ersSummary, ex, "Processing data for ADR exception")
              InternalServerError(s"Exception: ${ex.getMessage}.")
            }
          }
        }
      }
      case _ => Future {
        BadRequest("Invalid json.")
      }
    }
  }

  def saveMetadata = Action.async(parse.json[JsObject]) { implicit request =>
    metadataService.validateErsSummaryFromJson(request.body) match {
      case Some(ersSummary: ErsSummary) => {
        val startTime = System.currentTimeMillis()
        metadataService.storeErsSummary(ersSummary).map { result =>
          result match {
            case true => {
              metrics.saveMetadata(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
              ersLoggingAndAuditing.handleSuccess(ersSummary.metaData.schemeInfo, s"ErsSummary is successfully saved, bundleRef: ${ersSummary.bundleRef}")
              Ok("Metadata is successfully stored.")
            }
            case false => {
              ersLoggingAndAuditing.handleFailure(ersSummary.metaData.schemeInfo, s"Saving ErsSummary failed, bundleRef: ${ersSummary.bundleRef}")
              InternalServerError("Storing metadata failed.")
            }
          }
        }
      }
      case _ => Future {
        BadRequest("Invalid json.")
      }
    }
  }
}
