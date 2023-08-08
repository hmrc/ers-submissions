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

package fixtures

import com.typesafe.config.Config
import helpers.ERSTestHelper
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import utils.ConfigUtils

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDateTime, ZoneId}

object Common extends ERSTestHelper {
  val testConfirmationDateTime: Instant = LocalDateTime.parse("2015-05-21T11:12:00").atZone(ZoneId.of("UTC")).toInstant.truncatedTo(ChronoUnit.MILLIS)

  def loadConfiguration(schemeType: String, sheetName: String, configUtils: ConfigUtils): Config = {
    running(app) {
      configUtils.getConfigData(schemeType + "/" + sheetName, sheetName, Fixtures.EMISummaryDate)(new HeaderCarrier())
    }
  }
}
