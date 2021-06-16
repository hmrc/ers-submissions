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

package utils.LoggingAndExceptions

import helpers.ERSTestHelper
import models.ResubmissionException
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import services.audit.AuditEvents
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggingAndRexceptions.ResubmissionExceptionEmitter

class ResubmissionExceptionEmitterSpec extends ERSTestHelper {

  implicit val request: FakeRequest[AnyContent] = FakeRequest()
  implicit val hc: HeaderCarrier = new HeaderCarrier()
  val mockAuditEvents: AuditEvents = mock[AuditEvents]
  val mockResubmissionExceptionEmiter: ResubmissionExceptionEmitter = new ResubmissionExceptionEmitter(mockAuditEvents)

  "calling emitFrom" should {

    "throw ResubmissionException exeption with stack trace if an exception is given" in {
      val result = intercept[ResubmissionException] {
        mockResubmissionExceptionEmiter.emitFrom(Map("message" -> "ex message", "context" -> "ex context"), Some(new Exception("original message")), None)
      }
      result.getCause.getMessage shouldBe "original message"
    }

    "throw ResubmissionException exeption without stack trace if an exception isn't given" in {
      val result = intercept[ResubmissionException] {
        mockResubmissionExceptionEmiter.emitFrom(Map("message" -> "ex message", "context" -> "ex context"), None, None)
      }
      result.getCause shouldBe null
    }

  }

}
