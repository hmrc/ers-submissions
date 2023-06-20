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

import fixtures.Fixtures
import helpers.ERSTestHelper
import models.{ADRTransferException, ErsSummary}
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggingAndRexceptions.ADRExceptionEmitter

class ConfigUtilsSpec extends ERSTestHelper {

  val mockADRExceptionEmitter: ADRExceptionEmitter = app.injector.instanceOf[ADRExceptionEmitter]
  val testConfigUtils = new ConfigUtils(mockADRExceptionEmitter)
  implicit val request: FakeRequest[AnyContent] = FakeRequest()
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val ersSummary: ErsSummary = Fixtures.EMISummaryDate

  "calling getConfigData" should {

    "return config object" in {
      val result = testConfigUtils.getConfigData("common/Root", "Root", ersSummary)
      val firstField = result.getConfigList("fields").get(0)
      firstField.getString("name") shouldBe "regime"
      firstField.getString("type") shouldBe "string"
      firstField.getString("value") shouldBe "ERS"
    }

    "throws ADRException if unexisting file is loaded" in {
      val result = intercept[ADRTransferException] {
        testConfigUtils.getConfigData("nonexistent path", "Root", ersSummary)
      }
      result.message shouldBe "Trying to load invalid configuration. Path: nonexistent path, value: Root"
      result.context shouldBe "ConfigUtils.getConfigData"
    }
  }

  "calling getClearData" should {

    "return Object if Some(Object) is given" in {
      val result = testConfigUtils.getClearData(Some("data"))
      result shouldBe "data"
    }

    "return None if None is given" in {
      val result = testConfigUtils.getClearData(None)
      result shouldBe None
    }

    "return Object if Object is given" in {
      val result = testConfigUtils.getClearData("data")
      result shouldBe "data"
    }
  }
}
