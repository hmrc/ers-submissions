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

package utils.LoggingAndRexceptions

import models.{ADRTransferException, ResubmissionException, SchemeInfo}
import play.api.mvc.Request
import services.audit.AuditEvents
import uk.gov.hmrc.play.http.HeaderCarrier

object ResubmissionExceptionEmiter extends ErsLogger {

  def emitFrom(data: Map[String, String], ex: Option[Exception] = None, schemeInfo: Option[SchemeInfo])(implicit request: Request[_], hc: HeaderCarrier) = {
    val errorMessage = buildEmiterMessage(data)
    if(ex.isDefined) {
      val context = if(schemeInfo.isDefined) {
        Some(buildDataMessage(schemeInfo.get))
      }
      else {
        None
      }
      logException(errorMessage, ex.get, context)
      auditAndThrowWithStackTrace(data, ex.get, schemeInfo)
    }
    else {
      logError(errorMessage)
      auditAndThrow(data, schemeInfo)
    }
  }

  def auditAndThrowWithStackTrace(data: Map[String, String], ex: Exception, schemeInfo: Option[SchemeInfo])(implicit request: Request[_], hc: HeaderCarrier) = {
    if(!ex.isInstanceOf[ADRTransferException]) {
      AuditEvents.auditRunTimeError(ex, data("context"))
    }
    throw createResubmissionExceptionWithStackTrace(data, ex, schemeInfo)
  }

  def auditAndThrow(data: Map[String, String], schemeInfo: Option[SchemeInfo])(implicit request: Request[_], hc: HeaderCarrier) = {
    throw createResubmissionException(data, schemeInfo)
  }

  def createResubmissionExceptionWithStackTrace(data: Map[String, String], ex: Exception, schemeInfo: Option[SchemeInfo]) = {
    createResubmissionException(data, schemeInfo).initCause(ex)
  }

  def createResubmissionException(data: Map[String, String], schemeInfo: Option[SchemeInfo]) = {
    ResubmissionException (
      data.get("message").getOrElse("Undefined message"),
      data.get("context").getOrElse("Undefined context"),
      schemeInfo
    )
  }

}
