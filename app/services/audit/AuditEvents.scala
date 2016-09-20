/*
 * Copyright 2016 HM Revenue & Customs
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
import play.api.mvc.Request
import uk.gov.hmrc.play.http.HeaderCarrier

object AuditEvents extends AuditEvents {
  override def auditService : AuditService = AuditService
}

trait AuditEvents {
  def auditService: AuditService

  def auditRunTimeError(exception: Throwable, contextInfo: String) (implicit request: Request[_], hc: HeaderCarrier): Unit = {
    auditService.sendEvent(
      "ERSRunTimeError",
      Map(
        "ErrorMessage" -> exception.getMessage,
        "Context" -> contextInfo,
        "StackTrace" -> ExceptionUtils.getStackTrace(exception)
      )
    )
  }

  def auditADRTransferFailure(schemeInfo: SchemeInfo, data: Map[String, String])(implicit request: Request[_], hc: HeaderCarrier): Unit = {
    auditService.sendEvent("ErsADRTransferFailure", eventMap(schemeInfo, data))
  }

  def publicToProtectedEvent(schemeInfo: SchemeInfo, sheetName: String, numRows: String)(implicit request: Request[_], hc: HeaderCarrier): Boolean = {
    val additionalData: Map[String, String] = Map(
      "sheetName" -> sheetName,
      "numberOfRows" -> numRows
    )
    auditService.sendEvent("ErsFileTransfer", eventMap(schemeInfo, additionalData))
    true
  }

  def sendToAdrEvent(context : String, ersSummaryData: ErsSummary, correlationId: Option[String] = None, source: Option[String] = None)(implicit request: Request[_], hc: HeaderCarrier): Boolean = {
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
    auditService.sendEvent(context, eventMap(ersSummaryData.metaData.schemeInfo, additionalData))
    true
  }

  def eventMap(schemeInfo: SchemeInfo, additionalMap: Map[String, String] = Map.empty): Map[String,String] = {
    Map(
      "schemeRef" -> schemeInfo.schemeRef,
      "schemeId" -> schemeInfo.schemeId,
      "schemeType" -> schemeInfo.schemeType,
      "schemeName" -> schemeInfo.schemeName,
      "timestamp" -> schemeInfo.timestamp.toString,
      "taxYear" -> schemeInfo.taxYear
    ) ++ additionalMap
  }
}
