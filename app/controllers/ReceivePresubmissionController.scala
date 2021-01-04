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
import controllers.auth.{AuthAction, AuthorisedAction}
import metrics.Metrics
import models.SchemeData
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue}
import play.api.mvc.{Action, ControllerComponents, PlayBodyParsers, Request, Result}
import services.{PresubmissionService, ValidationService}
import services.audit.AuditEvents
import uk.gov.hmrc.auth.core.AuthConnector
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

class ReceivePresubmissionController @Inject()(presubmissionService: PresubmissionService,
                                               validationService: ValidationService,
                                               ersLoggingAndAuditing: ErsLoggingAndAuditing,
                                               authConnector: AuthConnector,
                                               auditEvents: AuditEvents,
                                               metrics: Metrics,
                                               cc: ControllerComponents,
                                               bodyParser: PlayBodyParsers) extends BackendController(cc) {

  def authorisedAction(empRef: String): AuthAction = AuthorisedAction(empRef, authConnector, bodyParser)

  def receivePresubmissionJson(empRef: String): Action[JsValue] =
    authorisedAction(empRef).async(parse.json(maxLength = 1024 * 10000)) { implicit request =>

    validationService.validateSchemeData(request.body.as[JsObject]) match {
      case schemeData: Some[SchemeData] => storePresubmission(schemeData.get)
      case _ => Future.successful(BadRequest("Invalid json format."))
    }
  }

  def storePresubmission(schemeData: SchemeData)(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
    Logger.info(s"Starting storing presubmission data. SchemeInfo: ${schemeData.schemeInfo.toString}, SheetName: ${schemeData.sheetName}")

    val startTime = System.currentTimeMillis()

    presubmissionService.storeJson(schemeData).map {
      case true =>
        metrics.storePresubmission(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
        auditEvents.publicToProtectedEvent(schemeData.schemeInfo, schemeData.sheetName, schemeData.data.getOrElse(Seq()).length.toString)
        ersLoggingAndAuditing.handleSuccess(schemeData.schemeInfo, s"Presubmission data for sheet ${schemeData.sheetName} is stored successfully")
        Ok("Presubmission data is stored successfully.")
      case _ =>
        metrics.failedStorePresubmission()
        ersLoggingAndAuditing.handleFailure(schemeData.schemeInfo, s"Storing presubmission data for sheet ${schemeData.sheetName} failed")
        InternalServerError("Storing presubmission data failed.")
    }
  }
}
