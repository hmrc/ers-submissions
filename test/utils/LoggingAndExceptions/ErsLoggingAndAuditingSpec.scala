/*
 * Copyright 2018 HM Revenue & Customs
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
import models.ADRTransferException
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing
import uk.gov.hmrc.http.HeaderCarrier

class ErsLoggingAndAuditingSpec extends UnitSpec with WithFakeApplication {

  implicit val request = FakeRequest()
  implicit val hc = new HeaderCarrier()

  "calling handleException" should {
    "log error if exception is given" in {
      val result = ErsLoggingAndAuditing.handleException(Fixtures.schemeInfo, new Exception("error message"), "")
      result shouldBe (())
    }

    "log error and audit if ADRTransferException is given" in {
      val result = ErsLoggingAndAuditing.handleException(Fixtures.schemeInfo, ADRTransferException(Fixtures.EMIMetaData, "error message", "error context"), "")
      result shouldBe (())
    }
  }

  "calling handleFailure" should {
    "log error and audit if exception is given" in {
      val result = ErsLoggingAndAuditing.handleFailure(Fixtures.schemeInfo, "")
      result shouldBe (())
    }
  }

  "calling handleSuccess" should {
    "log warning" in {
      val result = ErsLoggingAndAuditing.handleFailure(Fixtures.schemeInfo, "")
      result shouldBe (())
    }
  }

  "calling handleResult" should {
    "log warnrning with success message if result is true" in {
      val result = ErsLoggingAndAuditing.handleResult(Some(true), Some("Success message"), None, None)
      result shouldBe (())
    }
    "log error with error message if result is false" in {
      val result = ErsLoggingAndAuditing.handleResult(Some(false), None, Some("Error message"), None)
      result shouldBe (())
    }
  }

}
