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
import play.api.libs.json._
import play.api.mvc._
import services.PresubmissionService
import services.audit.AuditEvents
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ErrorHandlerHelper
import utils.LoggingAndExceptions.ErsLogger

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PresubmissionController @Inject()(presubmissionService: PresubmissionService,
                                        auditEvents: AuditEvents,
                                        metrics: Metrics,
                                        cc: ControllerComponents)
                                       (implicit ec: ExecutionContext) extends BackendController(cc) with ErsLogger with ErrorHandlerHelper {

  override val className: String = getClass.getSimpleName

  def removePresubmissionJson(): Action[JsObject] = Action.async(parse.json[JsObject]) {
    implicit request =>
      request.body.validate[SchemeInfo] match {
        case JsSuccess(schemeInfo, _) =>
          val startTime = System.currentTimeMillis()
          presubmissionService.removeJson(schemeInfo).value.map {
            case Right(true) =>
              logInfo(s"[PresubmissionController][removePresubmissionJson] Old presubmission data is successfully deleted for: ${schemeInfo.basicLogMessage}")
              metrics.removePresubmission(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
              Ok("Old presubmission data is successfully deleted.")
            case Left(NoData()) =>
              logWarn(s"[PresubmissionController][removePresubmissionJson] No data to delete found for: ${schemeInfo.basicLogMessage}")
              Ok("No data to delete found.")
            case Right(false) =>
              logError(s"[PresubmissionController][removePresubmissionJson] Deleting old presubmission data failed. for: ${schemeInfo.basicLogMessage}")
              metrics.failedRemovePresubmission()
              auditEvents.auditADRTransferFailure(schemeInfo, Map.empty)
              InternalServerError("Deleting old presubmission data failed.")
            case Left(error) =>
              logError(s"[PresubmissionController][removePresubmissionJson] Deleting old presubmission data failed for: ${schemeInfo.basicLogMessage} with [$error]")
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
          presubmissionService.compareSheetsNumber(validatedSheets, schemeInfo).value.map {
            case Right((true, _)) =>
              metrics.checkForPresubmission(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
              logInfo(s"[PresubmissionController][checkForExistingPresubmission] All presubmission records are found for: ${schemeInfo.basicLogMessage}")
              Ok("All presubmission records are found.")
            case Right((false, sheetsInRepository)) =>
              logError(s"[PresubmissionController][checkForExistingPresubmission] Found $sheetsInRepository presubmission records of expected $validatedSheets records for: ${schemeInfo.basicLogMessage}")
              auditEvents.auditADRTransferFailure(schemeInfo, Map.empty)
              InternalServerError(s"[PresubmissionController][checkForExistingPresubmission] Not all $validatedSheets records are found for ${schemeInfo.toString}.")
            case Left(error) =>
              logError(s"[PresubmissionController][checkForExistingPresubmission] Check existing presubmission failed for: ${schemeInfo.basicLogMessage} with error: [$error]")
              auditEvents.auditADRTransferFailure(schemeInfo, Map.empty)
              InternalServerError(s"[PresubmissionController][checkForExistingPresubmission] Not all $validatedSheets records are found for ${schemeInfo.toString}.")
          }
        case JsError(jsonErrors) => handleBadRequest(jsonErrors)
      }
  }
}
