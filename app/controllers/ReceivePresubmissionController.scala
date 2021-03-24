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

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{Sink, Source}
import javax.inject.Inject
import controllers.auth.{AuthAction, AuthorisedAction}
import metrics.Metrics
import models.{SchemeData, SubmissionsSchemeData}
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}

import play.api.mvc.{Action, ControllerComponents, PlayBodyParsers, Request, Result}
import services.{FileDownloadService, PresubmissionService, ValidationService}
import services.audit.AuditEvents
import uk.gov.hmrc.auth.core.AuthConnector
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

class ReceivePresubmissionController @Inject()(presubmissionService: PresubmissionService,
                                               validationService: ValidationService,
                                               fileDownloadService: FileDownloadService,
                                               ersLoggingAndAuditing: ErsLoggingAndAuditing,
                                               authConnector: AuthConnector,
                                               auditEvents: AuditEvents,
                                               metrics: Metrics,
                                               cc: ControllerComponents,
                                               bodyParser: PlayBodyParsers)
                                              (implicit  actorSystem: ActorSystem) extends BackendController(cc) {

  def authorisedAction(empRef: String): AuthAction = AuthorisedAction(empRef, authConnector, bodyParser)

  def receivePresubmissionJson(empRef: String): Action[JsValue] =
    authorisedAction(empRef).async(parse.json(maxLength = 1024 * 10000)) { implicit request =>

    validationService.validateSchemeData(request.body.as[JsObject]) match {
      case schemeData: Some[SchemeData] => storePresubmission(schemeData.get)
      case _ =>
        Future.successful(BadRequest("Invalid json format."))
    }
  }

  def receivePresubmissionJsonV2(empRef: String): Action[JsValue] =
    authorisedAction(empRef).async(parse.json) { implicit request =>

    validationService.validateSubmissionsSchemeData(request.body.as[JsObject]) match {
      case schemeData: Some[SubmissionsSchemeData] =>
        storePresubmission(schemeData.get)
      case somethingElse =>
        Logger.error("requestBody was actually" + request.body)
        Future.successful(BadRequest("Invalid json format."))
    }
  }

  def storePresubmission(schemeData: SubmissionsSchemeData)(
    implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {

    val startTime = System.currentTimeMillis()
    val validatedFile: Future[Either[Throwable, Seq[Seq[String]]]] =
      fileDownloadService.fileToSequenceOfEithers(schemeData).map{ sequence => sequence.collectFirst{
      case Left(x) => x
    } match {
      case Some(issue) => Left(issue)
      case _ => Right(sequence.map(_.right.get.map(_.utf8String)))
    }}

    val schemeData2: JsObject = Json.toJson(SchemeData(schemeData.schemeInfo, schemeData.sheetName, None, None)).as[JsObject]

    validatedFile.flatMap {
      case Right(rows) =>
        presubmissionService.storeJson(schemeData, schemeData2 + ("data" -> Json.toJson(rows))).map {
          case true =>
            metrics.storePresubmission(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
//            auditEvents.publicToProtectedEvent(schemeData.schemeInfo, schemeData.sheetName, schemeData.data.getOrElse(Seq()).length.toString)
            ersLoggingAndAuditing.handleSuccess(schemeData.schemeInfo, s"Presubmission data for sheet ${schemeData.sheetName} is stored successfully")
            Ok("Presubmission data is stored successfully.")
          case _ =>
            Logger.error("we got a false from storing presubmission")
            metrics.failedStorePresubmission()
            ersLoggingAndAuditing.handleFailure(schemeData.schemeInfo, s"Storing presubmission data for sheet ${schemeData.sheetName} failed")
            InternalServerError("Storing presubmission data failed.")
        }
      case Left(_) =>
        Logger.error("it didn't extract file correctly")
        metrics.failedStorePresubmission()
        ersLoggingAndAuditing.handleFailure(schemeData.schemeInfo, s"Storing presubmission data for sheet ${schemeData.sheetName} failed")
        Future.successful(InternalServerError("Storing presubmission data failed."))
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
