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
import models.{NoData, SchemeInfo}
import play.api.Logging
import play.api.libs.json._
import play.api.mvc._
import services.PresubmissionService
import services.audit.AuditEvents
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ErrorHandlerHelper

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PresubmissionController @Inject()(presubmissionService: PresubmissionService,
                                        auditEvents: AuditEvents,
                                        metrics: Metrics,
                                        cc: ControllerComponents)
                                       (implicit ec: ExecutionContext) extends BackendController(cc) with Logging with ErrorHandlerHelper {

  override val className: String = getClass.getSimpleName

  def removePresubmissionJson(): Action[JsObject] = Action.async(parse.json[JsObject]) {
    implicit request =>
      request.body.validate[SchemeInfo] match {
        case JsSuccess(schemeInfo, _) =>
          val startTime = System.currentTimeMillis()
          presubmissionService.removeJson(schemeInfo).value.map {
            case Right(true) =>
              metrics.removePresubmission(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
              Ok("Old presubmission data is successfully deleted.")
            case Left(NoData()) =>
              Ok("No data to delete found.")
            case Right(false) =>
              metrics.failedRemovePresubmission()
              auditEvents.auditADRTransferFailure(schemeInfo, Map.empty)
              InternalServerError("Deleting old presubmission data failed.")
            case Left(error) =>
              logger.error(s"Deleting old presubmission data failed for: ${schemeInfo.basicLogMessage} with [$error]")
              metrics.failedRemovePresubmission()
              auditEvents.auditADRTransferFailure(schemeInfo, Map.empty)
              InternalServerError("Deleting old presubmission data failed.")
          }
        case JsError(jsonErrors) => handleBadRequest(jsonErrors)
      }
  }

  def checkForExistingPresubmission(validatedSheets: Int): Action[JsObject] = Action.async(parse.json[JsObject]) {
    implicit request =>
      request.body.validate[SchemeInfo] match {
        case JsSuccess(schemeInfo, _) =>
          val startTime = System.currentTimeMillis()
          presubmissionService.getSheetCount(schemeInfo).value.map {
            case Right(count) if count > 0 =>
              metrics.checkForPresubmission(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
              logger.info(s"Found $count presubmission records for: ${schemeInfo.basicLogMessage}")
              Ok(s"Presubmission records found: $count")
            case Right(0) =>
              logger.warn(s"No presubmission records found for: ${schemeInfo.basicLogMessage}")
              auditEvents.auditADRTransferFailure(schemeInfo, Map.empty)
              InternalServerError(s"No presubmission data found for ${schemeInfo.toString}.")
            case Left(error) =>
              logger.error(s"Check existing presubmission failed for: ${schemeInfo.basicLogMessage} with error: [$error]")
              auditEvents.auditADRTransferFailure(schemeInfo, Map.empty)
              InternalServerError(s"Failed to verify presubmission data for ${schemeInfo.toString}.")
          }
        case JsError(jsonErrors) => handleBadRequest(jsonErrors)
      }
  }
}
