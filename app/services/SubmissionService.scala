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

package services

import common.ERSEnvelope
import common.ERSEnvelope.ERSEnvelope
import connectors.ADRConnector
import metrics.Metrics
import models.{ErsSummary, SchemeInfo, SubmissionStatusUpdateError}
import play.api.Logging
import play.api.http.Status.ACCEPTED
import play.api.libs.json.{JsError, JsObject, JsPath, JsString, JsSuccess, __}
import play.api.mvc.Request
import repositories.{MetadataMongoRepository, Repositories}
import services.audit.AuditEvents
import uk.gov.hmrc.http.HeaderCarrier
import utils.{ADRSubmission, Session, SubmissionCommon}

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SubmissionService @Inject()(repositories: Repositories,
                                  adrConnector: ADRConnector,
                                  adrSubmission: ADRSubmission,
                                  submissionCommon: SubmissionCommon,
                                  auditEvents: AuditEvents,
                                  metrics: Metrics)(implicit ec: ExecutionContext) extends Logging {

  lazy val metadataRepository: MetadataMongoRepository = repositories.metadataRepository

  def callProcessData(ersSummary: ErsSummary, failedStatus: String, successStatus: String)
                     (implicit request: Request[_], hc: HeaderCarrier): ERSEnvelope[Boolean] =
    processData(ersSummary, failedStatus, successStatus).recover {
      case error =>
        metadataRepository.updateStatus(ersSummary.metaData.schemeInfo, failedStatus, Session.id(hc))
        logger.error(s"Processing data failed with error: [$error]. Updating transfer status to: [$failedStatus] for ${ersSummary.metaData.schemeInfo.basicLogMessage}")
        false
    }

  def processData(ersSummary: ErsSummary, failedStatus: String, successStatus: String)
                 (implicit request: Request[_], hc: HeaderCarrier): ERSEnvelope[Boolean] = {
      for {
        adrData <- transformData(ersSummary)
        postSubmissionUpdated <- sendToADRUpdatePostData(ersSummary, adrData, failedStatus, successStatus)
      } yield postSubmissionUpdated
  }

  def transformData(ersSummary: ErsSummary)(implicit request: Request[_], hc: HeaderCarrier): ERSEnvelope[JsObject] = {
    val startTime = System.currentTimeMillis()
    val (maxFirstNameLen, maxCountryLen) = (35, 18)

    def trimDataIfSizeExceeded(json: JsObject, fieldName: String, jsPath: JsPath, maxLen: Int) = json.transform({
     jsPath.json.update(__.read[JsString].map { field =>
        if (field.as[String].length > maxLen) {
          logger.info(s"[SubmissionService][transformData] $fieldName was greater than $maxLen characters for " +
            s"SchemeRef: ${ersSummary.metaData.schemeInfo.schemeRef}, trimming to allow submission")
          JsString(field.as[String].take(maxLen))
        } else {
          field
        }
      })
    }) match {
      case JsSuccess(value, _) => value
      case JsError(_) =>
        logger.error(s"[SubmissionService][transformData] Failed to transform Json Path $jsPath data, attempting to proceed untransformed")
        json
    }
    
    adrSubmission.generateSubmission(ersSummary)(request, hc).map { json =>
      metrics.generateJson(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
      val transformedFirstNameJson = trimDataIfSizeExceeded(json, "firstName", __ \ "submitter" \ "firstName", maxFirstNameLen)
      val transformedDataJson = trimDataIfSizeExceeded(transformedFirstNameJson, "country", __ \ "submitter" \ "address" \ "country", maxCountryLen)
      transformedDataJson
    }
  }

  def sendToADRUpdatePostData(ersSummary: ErsSummary, adrData: JsObject, failedStatus: String, successStatus: String)
                             (implicit hc: HeaderCarrier): ERSEnvelope[Boolean] = {
    val startTime = System.currentTimeMillis()

    val result: ERSEnvelope[Boolean] = adrConnector.sendData(adrData, ersSummary.metaData.schemeInfo.schemeType).flatMap { response =>
      val correlationID: String = submissionCommon.getCorrelationID(response)
      val transferStatus: String = response.status match {
        case ACCEPTED =>
          metrics.sendToADR(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
          metrics.successfulSendToADR()
          auditEvents.sendToAdrEvent("ErsTransferToAdrResponseReceived", ersSummary, Some(correlationID))
          logger.info(s"Data transfer to ADR was successful for ${ersSummary.metaData.schemeInfo.basicLogMessage}, correlationId: $correlationID")
          successStatus
        case _ =>
          metrics.failedSendToADR()
          auditEvents.sendToAdrEvent("ErsTransferToAdrFailed", ersSummary)
          logger.error(s"Data transfer to ADR failed for ${ersSummary.metaData.schemeInfo.basicLogMessage}, correlationId: $correlationID")
          failedStatus
      }
      updatePostsubmission(response.status, transferStatus, ersSummary.metaData.schemeInfo)
    }
    result
  }

  def updatePostsubmission(adrSubmissionStatus: Int, transferStatus: String, schemeInfo: SchemeInfo)
                          (implicit hc: HeaderCarrier): ERSEnvelope[Boolean] = {
    val startUpdateTime = System.currentTimeMillis()
    metadataRepository.updateStatus(schemeInfo, transferStatus, Session.id(hc)).flatMap {
        case true if adrSubmissionStatus == ACCEPTED =>
          metrics.updatePostsubmissionStatus(System.currentTimeMillis() - startUpdateTime, TimeUnit.MILLISECONDS)
          logger.info(s"Updated submission transfer status to: [$transferStatus] for ${schemeInfo.basicLogMessage}")
          ERSEnvelope(true)
        case true =>
          metrics.updatePostsubmissionStatus(System.currentTimeMillis() - startUpdateTime, TimeUnit.MILLISECONDS)
          logger.info(s"Updated submission transfer status to: [$transferStatus] for ${schemeInfo.basicLogMessage}")
          ERSEnvelope(SubmissionStatusUpdateError(Some(adrSubmissionStatus), Some(transferStatus)))
        case _ =>
          logger.info(s"Submission transfer status update to: [$transferStatus] failed for ${schemeInfo.basicLogMessage}")
          ERSEnvelope(SubmissionStatusUpdateError(Some(adrSubmissionStatus), Some(transferStatus)))
    }
  }
}
