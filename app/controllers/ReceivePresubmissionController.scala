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

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import config.ApplicationConfig
import controllers.auth.{AuthAction, AuthorisedAction}
import metrics.Metrics
import models.{SchemeData, SubmissionsSchemeData}
import play.api.Logging
import play.api.libs.json.{JsObject, JsValue}
import play.api.mvc._
import services.audit.AuditEvents
import services.{FileDownloadService, PresubmissionService, ValidationService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

class ReceivePresubmissionController @Inject()(presubmissionService: PresubmissionService,
                                               validationService: ValidationService,
                                               fileDownloadService: FileDownloadService,
                                               ersLoggingAndAuditing: ErsLoggingAndAuditing,
                                               authConnector: AuthConnector,
                                               auditEvents: AuditEvents,
                                               metrics: Metrics,
                                               cc: ControllerComponents,
                                               bodyParser: PlayBodyParsers,
                                               appConfig: ApplicationConfig)
                                              (implicit actorSystem: ActorSystem, ec: ExecutionContext) extends BackendController(cc) with Logging {

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
        case submissionsSchemeData: Some[SubmissionsSchemeData] =>
          storePresubmission(submissionsSchemeData.get)
        case _ =>
          Future.successful(BadRequest("Invalid json format."))
      }
    }

  def submitJson(fileSource: Source[(Seq[Seq[ByteString]], Long), _], submissionsSchemeData: SubmissionsSchemeData)(
    implicit hc: HeaderCarrier): Future[(Boolean, Long)] = {

    fileSource.mapAsyncUnordered(appConfig.submissionParallelism)(chunkedRowsWithIndex => {
      val (chunkedRows, index) = chunkedRowsWithIndex
      val checkedData: Option[ListBuffer[Seq[String]]] = Option(chunkedRows.map(_.map(_.utf8String)).to[ListBuffer]).filter(_.nonEmpty)

      presubmissionService.storeJsonV2(submissionsSchemeData,
        SchemeData(submissionsSchemeData.schemeInfo,
          submissionsSchemeData.sheetName,
          numberOfParts = None,
          data = checkedData))
        .map(wasStoredSuccessfully => (wasStoredSuccessfully, index))
    })
      .takeWhile(booleanAndIndex => {
        val wasStoredSuccessfully: Boolean = booleanAndIndex._1
        wasStoredSuccessfully
      }, inclusive = true)
      .runWith(Sink.last)
  }

  def storePresubmission(submissionsSchemeData: SubmissionsSchemeData)(implicit hc: HeaderCarrier): Future[Result] = {

    val startTime = System.currentTimeMillis()
    val fileSource: Source[(Seq[Seq[ByteString]], Long), _] =
      fileDownloadService.schemeDataToChunksWithIndex(submissionsSchemeData)

    submitJson(fileSource, submissionsSchemeData).map {
      case (true, _) =>
        metrics.storePresubmission(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
        logger.debug("total running time was " + (System.currentTimeMillis() - startTime))
        auditEvents.publicToProtectedEvent(submissionsSchemeData.schemeInfo, submissionsSchemeData.sheetName, submissionsSchemeData.numberOfRows.toString)
        ersLoggingAndAuditing.handleSuccess(
          submissionsSchemeData.schemeInfo, s"Presubmission data for sheet ${submissionsSchemeData.sheetName} was stored successfully"
        )
        Ok("Presubmission data is stored successfully.")
      case (_, index) =>
        presubmissionService.removeJson(submissionsSchemeData.schemeInfo).map { wasSuccess =>
          if (!wasSuccess && index > 0) {
            logger.error(
              "[ReceivePresubmissionController][storePresubmission] INTERVENTION NEEDED: Removing partial presubmission data failed after storing failure")
          }
        }
        metrics.failedStorePresubmission()
        ersLoggingAndAuditing.handleFailure(submissionsSchemeData.schemeInfo, s"Storing presubmission data for sheet ${submissionsSchemeData.sheetName} failed")
        InternalServerError("Storing presubmission data failed.")
    }.recover {
      case ex: UpstreamErrorResponse =>
        InternalServerError(ex.getMessage())
      case ex =>
        logger.error(s"[ReceivePresubmissionController][storePresubmission] Unknown exception encountered while submitting file: ${ex.getMessage}")
        InternalServerError(ex.getMessage)
    }
  }

  def storePresubmission(schemeData: SchemeData)(implicit hc: HeaderCarrier): Future[Result] = {
    logger.info(s"Starting storing presubmission data. SchemeInfo: ${schemeData.schemeInfo.toString}, SheetName: ${schemeData.sheetName}")

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
