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

package utils.LoggingAndRexceptions

import models._
import services.audit.AuditEvents
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject

class ErsLoggingAndAuditing @Inject()(auditEvents: AuditEvents) extends ErsLogger {

  def handleException(data: Object, ex: Exception, contextInfo: String)(implicit hc: HeaderCarrier): Unit = {
    logException(data, ex)
    if(!ex.isInstanceOf[ResubmissionException] && !ex.isInstanceOf[ADRTransferException]) {
      auditEvents.auditRunTimeError(ex, contextInfo)
    }
  }

  def handleFailure(schemeInfo: SchemeInfo, message: String)(implicit hc: HeaderCarrier): Unit = {
    logError(message, Some(schemeInfo))
    auditEvents.auditADRTransferFailure(schemeInfo, Map.empty)
  }

  def handleSuccess(data: Object, message: String): Unit = logWarn(message, Some(data))

  def handleResult(result: Option[Boolean], successMsg: Option[String], errorMsg: Option[String], data: Option[Object] = None): Unit = {
    result match {
      case Some(true) if successMsg.isDefined => logWarn(successMsg.get, data)
      case Some(false) if errorMsg.isDefined => logError(errorMsg.get, data)
      case None => logWarn("Nothing to submit")
    }
  }
}
