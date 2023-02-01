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

import java.util.concurrent.TimeUnit

import javax.inject.Inject
import metrics.Metrics
import models.SchemeInfo
import play.api.Logging
import play.api.libs.json.JsObject
import play.api.mvc._
import services.{PresubmissionService, ValidationService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PresubmissionController @Inject()(presubmissionService: PresubmissionService,
                                        validationService: ValidationService,
                                        ersLoggingAndAuditing: ErsLoggingAndAuditing,
                                        metrics: Metrics,
                                        cc: ControllerComponents) extends BackendController(cc) with Logging{


  def removePresubmissionJson(): Action[JsObject] = Action.async(parse.json[JsObject]) { implicit request =>
    validationService.validateSchemeInfo(request.body) match {
      case Some(schemeInfo) =>
        val startTime = System.currentTimeMillis()
        logger.info(s"Start deleting presubmission data from external url for ${schemeInfo.toString}")

        presubmissionService.removeJson(schemeInfo).map {
          case true =>
            metrics.removePresubmission(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
            ersLoggingAndAuditing.handleSuccess(schemeInfo, "Old presubmission data is successfully deleted")
            Ok("Old presubmission data is successfully deleted.")
          case false =>
            metrics.failedRemovePresubmission()
            ersLoggingAndAuditing.handleFailure(schemeInfo, "Deleting old presubmission data failed")
            InternalServerError("Deleting old presubmission data failed.")
        }
      case _ => Future.successful(BadRequest("Invalid json format."))
    }
  }

  def checkForExistingPresubmission(validatedSheets: Int): Action[JsObject] = Action.async(parse.json[JsObject]) { implicit request =>
    validationService.validateSchemeInfo(request.body) match {
      case Some(schemeInfo: SchemeInfo) =>
        val startTime = System.currentTimeMillis()
        presubmissionService.compareSheetsNumber(validatedSheets, schemeInfo).map {
          case true =>
            metrics.checkForPresubmission(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
            ersLoggingAndAuditing.handleSuccess(schemeInfo, "All presubmission records are found")
            Ok("All presubmission records are found")
          case false =>
            ersLoggingAndAuditing.handleFailure(schemeInfo, s"Not all $validatedSheets presubmission records are found")
            InternalServerError(s"Not all $validatedSheets records are found for ${schemeInfo.toString}.")
        }
      case _ => Future.successful(BadRequest("Invalid json."))
    }
  }
}
