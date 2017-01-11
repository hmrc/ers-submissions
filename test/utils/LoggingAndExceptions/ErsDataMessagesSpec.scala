/*
 * Copyright 2017 HM Revenue & Customs
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
import uk.gov.hmrc.play.test.UnitSpec
import utils.LoggingAndRexceptions.ErsDataMessages

class ErsDataMessagesSpec extends UnitSpec {

  object TestDataMessages extends ErsDataMessages

  "calling buildSchemeInfoMessage" should {
    "display correct message for schemeInfo" in {
      val result = TestDataMessages.buildSchemeInfoMessage(Fixtures.schemeInfo)
      result shouldBe "SchemeInfo: SchemeInfo(XA1100000000000,2015-12-05T12:50:55.000Z,123PA12345678,2014/15,My scheme,EMI)"
    }
  }

  "calling buildErsSummaryMessage" should {
    "display correct message for ErsSummary" in {
      val result = TestDataMessages.buildErsSummaryMessage(Fixtures.EMISummaryDate)
      result shouldBe "ConfirmationDateTime: 2015-12-05T12:50:55.000Z\nBundleRef: 123453222,\nisNilReturn: true,\nfileType: ods,\nSchemeInfo: SchemeInfo(XA1100000000000,2015-12-05T12:50:55.000Z,123PA12345678,2014/15,My scheme,EMI)"
    }
  }

  "calling buildWildcardDataMessage" should {
    "display given object as a string" in {
      val result = TestDataMessages.buildWildcardDataMessage("test")
      result shouldBe "test"
    }
  }

}
