/*
 * Copyright 2022 HM Revenue & Customs
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

package utils.LoggingAndExceptions

import fixtures.Fixtures
import helpers.ERSTestHelper
import models.ADRTransferException
import services.audit.AuditEvents
import utils.LoggingAndRexceptions.ADRExceptionEmitter
import uk.gov.hmrc.http.HeaderCarrier

class ADRExceptionEmitterSpec extends ERSTestHelper {

  val mockAuditEvents: AuditEvents = mock[AuditEvents]

  val mockADRExceptionEmitter = new ADRExceptionEmitter(mockAuditEvents)

  "emitFrom" should {

    implicit val hc: HeaderCarrier = new HeaderCarrier()

    "throw ADRException that contains original exception" in {
      val thrownException = intercept[ADRTransferException] {
        try {
          throw new RuntimeException("generic runtime exception")
        }
        catch {
          case ex: Exception => mockADRExceptionEmitter.emitFrom(
            Fixtures.EMIMetaData,
            Map(
              "message" -> "Error message",
              "context" -> "Error context"
            ),
            Some(ex)
          )
        }
      }
      thrownException.getMessage shouldBe s"Error message"
      thrownException.getCause.getMessage shouldBe "generic runtime exception"
    }

    "throw ADRException" in {
      val thrownException = intercept[ADRTransferException] {
        try {
          throw new RuntimeException
        }
        catch {
          case _: Exception => mockADRExceptionEmitter.emitFrom(
            Fixtures.EMIMetaData,
            Map(
              "message" -> "Error message",
              "context" -> "Error context"
            ),
            None
          )
        }
      }
      thrownException.getMessage shouldBe "Error message"
      thrownException.getCause shouldBe null
    }

  }

}
