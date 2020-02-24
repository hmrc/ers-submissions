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

package utils.LoggingAndRexceptions

import models.{ADRTransferException, ErsMetaData}
import play.api.mvc.Request
import services.audit.AuditEvents
import uk.gov.hmrc.http.HeaderCarrier

object ADRExceptionEmitter extends ErsLogger {
  
  def emitFrom(ersMetaData: ErsMetaData, data: Map[String, String], ex: Option[Exception] = None)(implicit request: Request[_], hc: HeaderCarrier) = {
    if(ex.isDefined) {
      logException(ersMetaData.schemeInfo, ex.get, Some(buildEmiterMessage(data)))
      auditAndThrowWithStackTrace(ersMetaData, data, ex.get)
    }
    else {
      logError(buildEmiterMessage(data), Some(ersMetaData.schemeInfo))
      auditAndThrow(ersMetaData, data)
    }
  }
  
  def auditAndThrowWithStackTrace(ersMetaData: ErsMetaData, data: Map[String, String], ex: Exception)(implicit request: Request[_], hc: HeaderCarrier) = {
    AuditEvents.auditRunTimeError(ex, data("context"))
    throw createADRException(ersMetaData, data).initCause(ex)
  }

  def auditAndThrow(ersMetaData: ErsMetaData, data: Map[String, String])(implicit request: Request[_], hc: HeaderCarrier) = {
    AuditEvents.auditADRTransferFailure(ersMetaData.schemeInfo, data)
    throw createADRException(ersMetaData, data)
  }

  def createADRException(ersMetaData: ErsMetaData, data: Map[String, String]) = {
    ADRTransferException(
      ersMetaData,
      data.get("message").getOrElse("Undefined message"),
      data.get("context").getOrElse("Undefined context")
    )
  }

}
