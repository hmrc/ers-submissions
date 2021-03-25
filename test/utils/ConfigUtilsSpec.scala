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

package utils

import fixtures.Fixtures
import models.{ADRTransferException, ErsSummary}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggingAndRexceptions.ADRExceptionEmitter

class ConfigUtilsSpec extends UnitSpec with MockitoSugar with GuiceOneAppPerSuite {

  val mockADRExceptionEmitter: ADRExceptionEmitter = app.injector.instanceOf[ADRExceptionEmitter]
  val testConfigUtils = new ConfigUtils(mockADRExceptionEmitter)
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val hc = HeaderCarrier()
  implicit val ersSummary: ErsSummary = Fixtures.EMISummaryDate

  "calling getConfigData" should {

    "return config object" in {
      val result = testConfigUtils.getConfigData("common/Root", "Root")
      val firstField = result.getConfigList("fields").get(0)
      firstField.getString("name") shouldBe "regime"
      firstField.getString("type") shouldBe "string"
      firstField.getString("value") shouldBe "ERS"
    }

    "throws ADRException if unexisting file is loaded" in {
      val result = intercept[ADRTransferException] {
        testConfigUtils.getConfigData("unexisting path", "Root")
      }
      result.message shouldBe "Trying to load invalid configuration. Path: unexisting path, value: Root"
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
