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

package utils.LoggingAndRexceptions

import models.{ADRTransferException, ErsMetaData}
import services.audit.AuditEvents
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject

class ADRExceptionEmitter @Inject()(auditEvents: AuditEvents) {
  def auditAndThrowWithStackTrace(ersMetaData: ErsMetaData, data: Map[String, String], ex: Exception)
                                 (implicit hc: HeaderCarrier): Nothing = {
    auditEvents.auditRunTimeError(ex, data("context"))
    throw createADRException(ersMetaData, data).initCause(ex)
  }

  def createADRException(ersMetaData: ErsMetaData, data: Map[String, String]): ADRTransferException = {
    ADRTransferException(
      ersMetaData,
      data.getOrElse("message","Undefined message"),
      data.getOrElse("context", "Undefined context")
    )
  }
}
