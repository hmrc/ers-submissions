/*
 * Copyright 2020 HM Revenue & Customs
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

package services

import java.util.concurrent.TimeUnit

import connectors.ADRConnector
import controllers.Assets.ACCEPTED
import javax.inject.Inject
import metrics.Metrics
import models.{ADRTransferException, ErsSummary}
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.JsObject
import play.api.mvc.Request
import repositories.{MetadataMongoRepository, Repositories}
import services.audit.AuditEvents
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggingAndRexceptions.{ADRExceptionEmitter, ErsLoggingAndAuditing}
import utils.{ADRSubmission, SubmissionCommon}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubmissionService @Inject()(repositories: Repositories,
                                  adrConnector: ADRConnector,
                                  adrSubmission: ADRSubmission,
                                  submissionCommon: SubmissionCommon,
                                  ersLoggingAndAuditing: ErsLoggingAndAuditing,
                                  adrExceptionEmitter: ADRExceptionEmitter,
                                  auditEvents: AuditEvents,
                                  metrics: Metrics) {

  lazy val metadataRepository: MetadataMongoRepository = repositories.metadataRepository

  def callProcessData(ersSummary: ErsSummary, failedStatus: String, successStatus: String)(implicit request: Request[_], hc: HeaderCarrier): Future[Boolean] = {
    processData(ersSummary, failedStatus, successStatus).map {
      res => res
    }.recover {
      case aex: ADRTransferException =>
        ersLoggingAndAuditing.logWarn(s"Submission journey 4. start creating json exception ${aex.message}: ${DateTime.now}", Some(ersSummary))
        metadataRepository.updateStatus(ersSummary.metaData.schemeInfo, failedStatus).map[Boolean] { res => res }
        throw aex
      case ex: Exception =>
        ersLoggingAndAuditing.logWarn(s"Submission journey 4. start creating json exception ${ex.getMessage}: ${DateTime.now}", Some(ersSummary))
        metadataRepository.updateStatus(ersSummary.metaData.schemeInfo, failedStatus).map[Boolean] { res => res }
        adrExceptionEmitter.emitFrom(
          ersSummary.metaData,
          Map(
            "message" -> "Exception processing submission",
            "context" -> "PostsubmissionService.callProcessData"
          ),
          Some(ex)
        )
    }
  }

  def processData(ersSummary: ErsSummary, failedStatus: String, successStatus: String)(implicit request: Request[_], hc: HeaderCarrier): Future[Boolean] = {
    ersLoggingAndAuditing.logWarn(s"Submission journey 3. start processing data: ${DateTime.now}", Some(ersSummary))
    Logger.info(s"Start processing data for ${ersLoggingAndAuditing.buildDataMessage(ersSummary)}")
    transformData(ersSummary).flatMap { adrData =>
      sendToADRUpdatePostData(ersSummary, adrData, failedStatus, successStatus) map {res => res}
    }
  }

  def transformData(ersSummary: ErsSummary)(implicit request: Request[_], hc: HeaderCarrier): Future[JsObject] = {
    ersLoggingAndAuditing.logWarn(s"Submission journey 4. start creating json: ${DateTime.now}", Some(ersSummary))
    val startTime = System.currentTimeMillis()
    adrSubmission.generateSubmission()(request, hc, ersSummary).map { json =>
      ersLoggingAndAuditing.logWarn(s"Submission journey 5. json is created: ${DateTime.now}", Some(ersSummary))
      Logger.debug("LFP -> 8. Json created () In PostsubmissionService.transformData method " + json.fields.size)
      metrics.generateJson(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
      ersLoggingAndAuditing.handleSuccess(ersSummary, "Json is successfully created")
      json
    }.recover {
      case adrEx: ADRTransferException =>
        ersLoggingAndAuditing.logWarn(s"Submission journey 5. json is created exception ${adrEx.message}: ${DateTime.now}", Some(ersSummary))
        throw adrEx
      case ex: Exception =>
        ersLoggingAndAuditing.logWarn(s"Submission journey 5. json is created exception ${ex.getMessage}: ${DateTime.now}", Some(ersSummary))
        adrExceptionEmitter.emitFrom(
          ersSummary.metaData,
          Map(
            "message" -> "Exception during transformData",
            "context" -> "PostsubmissionService.transformData"
          ),
          Some(ex)
        )
    }
  }

  def sendToADRUpdatePostData(ersSummary: ErsSummary, adrData: JsObject, failedStatus: String, successStatus: String)
                             (implicit request: Request[_], hc: HeaderCarrier): Future[Boolean] = {
    ersLoggingAndAuditing.logWarn(s"Submission journey 6. start sending data: ${DateTime.now}", Some(ersSummary))
    Logger.info(s"Start sending data ${ersLoggingAndAuditing.buildDataMessage(ersSummary)}")

    val startTime = System.currentTimeMillis()

    adrConnector.sendData(adrData, ersSummary.metaData.schemeInfo.schemeType).flatMap { response =>
      ersLoggingAndAuditing.logWarn(s"Submission journey 7. data is sent with response ${response.status}: ${DateTime.now}", Some(ersSummary))
      val transferStatus: String = response.status match {
        case ACCEPTED =>
          Logger.debug("LFP -> 14. Data sent ")
          metrics.sendToADR(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
          metrics.successfulSendToADR()
          val correlationID: String = submissionCommon.getCorrelationID(response)
          ersLoggingAndAuditing.handleSuccess(ersSummary, s"Data is sent successfully to ADR. CorrelationId : ${correlationID}" + "size of Fields in Json: " + adrData.fields.size)
          auditEvents.sendToAdrEvent("ErsTransferToAdrResponseReceived", ersSummary, Some(correlationID))
          successStatus
        case _ =>
          metrics.failedSendToADR()
          ersLoggingAndAuditing.handleFailure(ersSummary.metaData.schemeInfo, s"Sending to ADR failed. BundleRef: ${ersSummary.bundleRef}")
          auditEvents.sendToAdrEvent("ErsTransferToAdrFailed", ersSummary)
          failedStatus
      }
      updatePostsubmission(response.status, transferStatus, ersSummary).map(res => res)
    }.recover {
      case adr: ADRTransferException =>
        ersLoggingAndAuditing.logWarn(s"Submission journey 7. data is sent exception ${adr.message}: ${DateTime.now}", Some(ersSummary))
        throw adr
      case e: Exception =>
        ersLoggingAndAuditing.logWarn(s"Submission journey 7. data is sent exception ${e.getMessage}: ${DateTime.now}", Some(ersSummary))
        adrExceptionEmitter.emitFrom(
          ersSummary.metaData,
          Map(
            "message" -> e.getMessage,
            "context" -> "SubmissionCommon.sendToADRUpdatePostData"
          ),
          Some(e)
        )
    }
  }

  def updatePostsubmission(adrSubmissionStatus: Int, transferStatus: String, ersSummary: ErsSummary)
                          (implicit request: Request[_], hc: HeaderCarrier): Future[Boolean] = {
    ersLoggingAndAuditing.logWarn(s"Submission journey 8. start updating status ${transferStatus}: ${DateTime.now}", Some(ersSummary))
    Logger.info(s"Start updating status for ${ersSummary.metaData.schemeInfo.toString}")
    val startUpdateTime = System.currentTimeMillis()

    metadataRepository.updateStatus(ersSummary.metaData.schemeInfo, transferStatus).map[Boolean] {
      case true if adrSubmissionStatus == ACCEPTED =>
        ersLoggingAndAuditing.logWarn(s"Submission journey 9. status was updated: ${DateTime.now}", Some(ersSummary))
        metrics.updatePostsubmissionStatus(System.currentTimeMillis() - startUpdateTime, TimeUnit.MILLISECONDS)
        ersLoggingAndAuditing.handleSuccess(ersSummary, "Status is updated successfully")
        true
      case false =>
        ersLoggingAndAuditing.logWarn(s"Submission journey 9. status was updated: ${DateTime.now}", Some(ersSummary))
        adrExceptionEmitter.emitFrom(
          ersSummary.metaData,
          Map(
            "message" -> "Updating status failed",
            "context" -> "SubmissionCommon.updatePostsubmission"
          )
        )
      case _ =>
        ersLoggingAndAuditing.logWarn(s"Submission journey 9. status was updated: ${DateTime.now}", Some(ersSummary))
        metrics.updatePostsubmissionStatus(System.currentTimeMillis() - startUpdateTime, TimeUnit.MILLISECONDS)
        adrExceptionEmitter.emitFrom(
          ersSummary.metaData,
          Map(
            "message" -> "Sending data to ADR failed",
            "context" -> "SubmissionCommon.sendToADRUpdatePostData"
          )
        )
    }.recover {
      case ex: Exception =>
        ersLoggingAndAuditing.logWarn(s"Submission journey 9. status was updated exception ${ex.getMessage}: ${DateTime.now}", Some(ersSummary))
        adrExceptionEmitter.emitFrom(
          ersSummary.metaData,
          Map(
            "message" -> "Updating status exception",
            "context" -> "SubmissionCommon.updatePostsubmission"
          ),
          Some(ex)
        )
    }
  }

}
