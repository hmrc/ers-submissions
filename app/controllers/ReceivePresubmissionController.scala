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

import cats.data.EitherT
import common.ERSEnvelope.ERSEnvelope
import config.ApplicationConfig
import controllers.auth.{AuthAction, AuthorisedAction}
import metrics.Metrics
import models.{ERSError, SchemeData, SubmissionsSchemeData}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.apache.pekko.util.ByteString
import play.api.libs.json._
import play.api.mvc._
import services.audit.AuditEvents
import services.{FileDownloadService, PresubmissionService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ErrorHandlerHelper
import utils.LoggingAndExceptions.ErsLogger

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

class ReceivePresubmissionController @Inject()(presubmissionService: PresubmissionService,
                                               fileDownloadService: FileDownloadService,
                                               authConnector: AuthConnector,
                                               auditEvents: AuditEvents,
                                               metrics: Metrics,
                                               cc: ControllerComponents,
                                               bodyParser: PlayBodyParsers,
                                               appConfig: ApplicationConfig)
                                              (implicit actorSystem: ActorSystem, ec: ExecutionContext) extends BackendController(cc) with ErsLogger with ErrorHandlerHelper {

  override val className: String = getClass.getSimpleName

  def authorisedAction(empRef: String): AuthAction = AuthorisedAction(empRef, authConnector, bodyParser)

  def receivePresubmissionJson(empRef: String): Action[JsValue] =
    authorisedAction(empRef).async(parse.json(maxLength = 1024 * 10000)) {
      implicit request =>
        request.body.validate[SchemeData] match {
          case JsSuccess(schemeData, _) => storePresubmission(schemeData)
          case JsError(jsonErrors) => handleBadRequest(jsonErrors)
        }
    }

  def receivePresubmissionJsonV2(empRef: String): Action[JsValue] =
    authorisedAction(empRef).async(parse.json) {
      implicit request =>
        request.body.validate[SubmissionsSchemeData] match {
          case JsSuccess(submissionsSchemeData, _) => storePresubmission(submissionsSchemeData)
          case JsError(jsonErrors) => handleBadRequest(jsonErrors)
        }
    }

  private[controllers] def storePresubmission(submissionsSchemeData: SubmissionsSchemeData)(implicit hc: HeaderCarrier): Future[Result] = {
    val startTime = System.currentTimeMillis()
    val fileSource: Source[(Seq[Seq[ByteString]], Long), _] =
      fileDownloadService.schemeDataToChunksWithIndex(submissionsSchemeData)

    submitJson(fileSource, submissionsSchemeData).value.map {
      case Right((true, _)) =>
        metrics.storePresubmission(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
        logInfo(s"Presubmission data for sheet ${submissionsSchemeData.sheetName} was stored successfully for: ${submissionsSchemeData.schemeInfo.basicLogMessage}")
        auditEvents.publicToProtectedEvent(submissionsSchemeData.schemeInfo, submissionsSchemeData.sheetName, submissionsSchemeData.numberOfRows.toString)
        Ok("Presubmission data is stored successfully.")
      case Right((_, index)) =>
        presubmissionService.removeJson(submissionsSchemeData.schemeInfo).map { wasSuccess =>
          if (!wasSuccess && index > 0) {
            logError(
              "[ReceivePresubmissionController][storePresubmission] INTERVENTION NEEDED: Removing partial presubmission data failed after storing failure")
          }
        }
        metrics.failedStorePresubmission()
        logError(s"Storing presubmission data failed for: ${submissionsSchemeData.sheetName}, ${submissionsSchemeData.schemeInfo.basicLogMessage}")
        auditEvents.auditADRTransferFailure(submissionsSchemeData.schemeInfo, Map.empty)
        InternalServerError("Storing presubmission data failed.")
      case Left(error) =>
        metrics.failedStorePresubmission()
        logError(s"Storing presubmission data failed for: ${submissionsSchemeData.sheetName}, ${submissionsSchemeData.schemeInfo.basicLogMessage} with error: [$error]")
        auditEvents.auditADRTransferFailure(submissionsSchemeData.schemeInfo, Map.empty)
        InternalServerError("Storing presubmission data failed.")
    }
  }

  private def storePresubmission(schemeData: SchemeData)(implicit hc: HeaderCarrier): Future[Result] = {
    val startTime = System.currentTimeMillis()
    presubmissionService.storeJson(schemeData).value.map {
      case Right(true) =>
        metrics.storePresubmission(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
        logInfo(s"Presubmission data for sheet ${schemeData.sheetName} is stored successfully")
        auditEvents.publicToProtectedEvent(schemeData.schemeInfo, schemeData.sheetName, schemeData.data.getOrElse(Seq()).length.toString)
        Ok("Presubmission data is stored successfully.")
      case Right(false) =>
        metrics.failedStorePresubmission()
        logError(s"Storing presubmission data failed for: ${schemeData.sheetName}, ${schemeData.schemeInfo.basicLogMessage}")
        auditEvents.auditADRTransferFailure(schemeData.schemeInfo, Map.empty)
        InternalServerError("Storing presubmission data failed.")
      case Left(error) =>
        metrics.failedStorePresubmission()
        logError(s"Storing presubmission data failed for: ${schemeData.sheetName}, ${schemeData.schemeInfo.basicLogMessage} with error: [$error]")
        auditEvents.auditADRTransferFailure(schemeData.schemeInfo, Map.empty)
        InternalServerError("Storing presubmission data failed.")
    }
  }

  private[controllers] def submitJson(fileSource: Source[(Seq[Seq[ByteString]], Long), _], submissionsSchemeData: SubmissionsSchemeData)
                                     (implicit hc: HeaderCarrier): ERSEnvelope[(Boolean, Long)] = EitherT {
    fileSource.mapAsyncUnordered(appConfig.submissionParallelism)(chunkedRowsWithIndex => {
        val (chunkedRows, index) = chunkedRowsWithIndex
        val checkedData: Option[ListBuffer[scala.Seq[String]]] = Option(chunkedRows.map(_.map(_.utf8String)).to(ListBuffer)).filter(_.nonEmpty)

        presubmissionService.storeJson(
            SchemeData(submissionsSchemeData.schemeInfo,
              submissionsSchemeData.sheetName,
              numberOfParts = None,
              data = checkedData))
          .value
          .map(_.map((_, index)))
      })
      .takeWhile((booleanAndIndex: Either[ERSError, (Boolean, Long)]) => {
        val wasStoredSuccessfully: Boolean = booleanAndIndex.map(_._1).getOrElse(false)
        wasStoredSuccessfully
      }, inclusive = true)
      .runWith(Sink.last)
  }
}
