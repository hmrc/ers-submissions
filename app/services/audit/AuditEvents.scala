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

package services.audit

import models.{ErsSummary, SchemeInfo}
import org.apache.commons.lang3.exception.ExceptionUtils
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Disabled, Failure, Success}
import utils.LoggingAndExceptions.ErsLogger

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuditEvents @Inject()(auditService: AuditService)(implicit ec: ExecutionContext) extends ErsLogger {

  def auditRunTimeError(exception: Throwable, contextInfo: String)(implicit hc: HeaderCarrier): Future[AuditResult] = {
    val transactionName = "ERSRunTimeError"
    auditService.sendEvent(
      transactionName,
      Map(
        "ErrorMessage" -> exception.getMessage,
        "Context" -> contextInfo,
        "StackTrace" -> ExceptionUtils.getStackTrace(exception)
      )
    ).map(handleResponse(_, transactionName))
  }

  def auditError(contextInfo: String, message: String)(implicit hc: HeaderCarrier): Future[AuditResult] = {
    val transactionName = "ERSRunTimeError"
    auditService.sendEvent(
      transactionName,
      Map(
        "ErrorMessage" -> message,
        "Context" -> contextInfo
      )
    ).map(handleResponse(_, transactionName))
  }

  def auditADRTransferFailure(schemeInfo: SchemeInfo, data: Map[String, String])(implicit hc: HeaderCarrier): Future[AuditResult] = {
    val transactionName = "ErsADRTransferFailure"
    auditService.sendEvent(
      transactionName,
      eventMap(schemeInfo, data)
    ).map(handleResponse(_, transactionName))
  }

  def publicToProtectedEvent(schemeInfo: SchemeInfo, sheetName: String, numRows: String)(implicit hc: HeaderCarrier): Future[AuditResult] = {
    val transactionName = "ErsFileTransfer"
    val additionalData: Map[String, String] = Map(
      "sheetName" -> sheetName,
      "numberOfRows" -> numRows
    )
    auditService.sendEvent(
      "ErsFileTransfer",
      eventMap(schemeInfo, additionalData)
    ).map(handleResponse(_, transactionName))
  }

  def sendToAdrEvent(transactionName: String, ersSummaryData: ErsSummary, correlationId: Option[String] = None, source: Option[String] = None)
                    (implicit hc: HeaderCarrier): Future[AuditResult] = {
    val additionalData: Map[String, String] = Map(
      "sapNumber" -> ersSummaryData.metaData.sapNumber.getOrElse(""),
      "ipRef" -> ersSummaryData.metaData.ipRef,
      "aoRef" -> ersSummaryData.metaData.aoRef.getOrElse(""),
      "empRef" -> ersSummaryData.metaData.empRef,
      "agentRef" -> ersSummaryData.metaData.agentRef.getOrElse(""),
      "sapNumber" -> ersSummaryData.metaData.sapNumber.getOrElse(""),
      "correlationId" -> correlationId.getOrElse(""),
      "nilReturn" -> ersSummaryData.isNilReturn,
      "fileType" -> ersSummaryData.fileType.getOrElse(""),
      "numberOfRows" -> ersSummaryData.nofOfRows.getOrElse(-1).toString,
      "source" -> source.getOrElse("")
    )
    auditService.sendEvent(
      transactionName,
      eventMap(ersSummaryData.metaData.schemeInfo, additionalData)
    ).map(handleResponse(_, transactionName))
  }

  def resubmissionResult(schemeInfo: SchemeInfo, res: Boolean)(implicit hc: HeaderCarrier): Future[AuditResult] = {
    val transactionName = "resubmissionResult"
    auditService.sendEvent(
      "resubmissionResult",
      eventMap(schemeInfo, Map("result" -> res.toString))
    ).map(handleResponse(_, transactionName))
  }

  def eventMap(schemeInfo: SchemeInfo, additionalMap: Map[String, String] = Map.empty): Map[String, String] = {
    Map(
      "schemeRef" -> schemeInfo.schemeRef,
      "schemeId" -> schemeInfo.schemeId,
      "schemeType" -> schemeInfo.schemeType,
      "schemeName" -> schemeInfo.schemeName,
      "timestamp" -> schemeInfo.timestamp.toString,
      "taxYear" -> schemeInfo.taxYear
    ) ++ additionalMap
  }

  private def handleResponse(result: AuditResult, transactionName: String): AuditResult = result match {
    case Success =>
      logger.debug(s"ers-submissions $transactionName audit successful")
      Success
    case Failure(err, _) =>
      logWarn(s"ers-submissions $transactionName audit error, message: $err")
      Failure(err)
    case Disabled =>
      logWarn(s"Auditing disabled")
      Disabled
  }
}
