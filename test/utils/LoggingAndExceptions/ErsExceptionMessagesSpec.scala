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

package utils.LoggingAndExceptions

import fixtures.Fixtures
import helpers.ERSTestHelper
import models.ADRTransferException
import utils.LoggingAndRexceptions.ErsExceptionMessages

class ErsExceptionMessagesSpec extends ERSTestHelper {

  object TestExceptionMessages extends ErsExceptionMessages

  "calling buildExceptionMesssage" should {
    "return correct message if ADRTransferException is given" in {
      val result = TestExceptionMessages.buildExceptionMesssage(ADRTransferException(Fixtures.EMIMetaData, "message", "context"))
      result shouldBe "ADRTransferException: message,\ncontext: context"
    }

    "return correct message if Exception is given" in {
      val result = TestExceptionMessages.buildExceptionMesssage(new RuntimeException("message"))
      result shouldBe "Exception: message"
    }
  }
}
