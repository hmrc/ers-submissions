/*
 * Copyright 2020 HM Revenue & Customs
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

package services

import fixtures.Fixtures
import models.{SchemeInfo, SchemeData}
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.play.test.UnitSpec

class ValidationServiceSpec extends UnitSpec {

  "calling validateSchemeData" should {

    "return None if invalid json is given" in {
      val result = ValidationService.validateSchemeData(Fixtures.invalidJson)
      result shouldBe None
    }

    "return valid SchemeData if correct json is given" in {
      val result = ValidationService.validateSchemeData(Fixtures.schemeDataJson)
      result.get.isInstanceOf[SchemeData] shouldBe true
     // result.get shouldBe Fixtures.schemeData
    }

  }

  "calling validateSchemeInfo" should {

    "return None if json is not of type SchemeInfo" in {
      val result = ValidationService.validateSchemeInfo(Fixtures.invalidJson)
      result shouldBe None
    }

    "return Some(ErsDataRef) if json is valid SchemeInfo" in {
      val result = ValidationService.validateSchemeInfo(Json.toJson(Fixtures.EMISchemeInfo).as[JsObject])
      result.get.isInstanceOf[SchemeInfo] shouldBe true
      // result.get shouldBe Fixtures.ersDataRef
    }

  }

}
