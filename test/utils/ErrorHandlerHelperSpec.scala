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

package utils

import helpers.ERSTestHelper
import models.ADRTransferError
import play.api.http.Status.BAD_REQUEST
import play.api.libs.json._

class ErrorHandlerHelperSpec extends ERSTestHelper with ErrorHandlerHelper {
  override val className: String = "someClassName"

  "handleError" should {
    "return ADRTransferError()" in {
      handleError(new Exception("message"), "someMethodName") shouldBe ADRTransferError()
    }
  }

  "handleBadRequest" should {
    "return BadRequest" in {
      val somePath = __ \ "somePath"
      val errors = scala.collection.Seq((somePath, scala.collection.Seq(JsonValidationError("someMessage"))))

      status(handleBadRequest(errors)) shouldBe BAD_REQUEST
    }
  }
}
