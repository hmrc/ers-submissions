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

package utils

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import fixtures.Common
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsObject, Json}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class SubmissionCommonSpec extends UnitSpec with MockitoSugar with GuiceOneAppPerSuite {

  val mockConfigUtils: ConfigUtils = app.injector.instanceOf[ConfigUtils]
  val testSubmissionCommon: SubmissionCommon = new SubmissionCommon(mockConfigUtils)

  "getBSONObjectID" should {

    "return BSONObjectID if valid string is given" in {
      val result = testSubmissionCommon.getBSONObjectID("""BSONObjectID("575164805500007f007d5406")""")
      result.isInstanceOf[BSONObjectID] shouldBe true
    }

    "throws exception if invalid string is given" in {
      intercept[Exception] {
        testSubmissionCommon.getBSONObjectID("""Invalid ID""")
      }
    }

  }

  "getCorrelationID" should {

    "return CorrelationId from header" in {
      val result = testSubmissionCommon.getCorrelationID(HttpResponse(202, None, Map("CorrelationId" -> Seq("CorrelationId -> Buffer(1A2B-3C-4D5F-6G-7Q)"))))
      result shouldBe "1A2B-3C-4D5F-6G-7Q"
    }

    "return empty string if CorrelationId is not in header" in {
      val result = testSubmissionCommon.getCorrelationID(HttpResponse(202))
      result shouldBe ""
    }
  }

  "castToDouble" should {
    "return double from string" in {
      testSubmissionCommon.castToDouble("10.5") shouldBe Some(10.5)
    }

    "return None from invalid double string" in {
      testSubmissionCommon.castToDouble("test") shouldBe None
    }
  }

  "castToInt" should {
    "return int from string" in {
      testSubmissionCommon.castToInt("10") shouldBe Some(10)
    }

    "return None from invalid double string" in {
      testSubmissionCommon.castToInt("test") shouldBe None
    }
  }

  "customFormat" should {

    val dateTime: DateTime = new DateTime().withYear(2015).withMonthOfYear(5).withDayOfMonth(21).withHourOfDay(11).withMinuteOfHour(12).withSecondOfMinute(0).withMillisOfSecond(0).withZone(DateTimeZone.UTC)

    "convert datetime to string using correct format" in {
      val testConfig = ConfigFactory.empty().withValue("type", ConfigValueFactory.fromAnyRef("datetime"))
        .withValue("json_format", ConfigValueFactory.fromAnyRef("yyyy-MM-dd'T'HH:mm:ss"))
      val result = testSubmissionCommon.customFormat(dateTime, testConfig)

      result shouldBe "2015-05-21T11:12:00"
    }

    "convert datetime to string without using formatting" in {
      val testConfig = ConfigFactory.empty().withValue("type", ConfigValueFactory.fromAnyRef(""))
      val result = testSubmissionCommon.customFormat(dateTime, testConfig)

      result shouldBe dateTime.toString()
    }
  }

  "getNewField" should {
    "get the expected field" in{
      val testConfig = ConfigFactory.empty().withValue("name", ConfigValueFactory.fromAnyRef("fieldName"))
      val result = testSubmissionCommon.getNewField(testConfig, "value")

     result shouldBe Json.obj("fieldName" -> "value")
    }
  }

  "getObjectFromJson" should {
    val value2 = Json.obj("field3" -> "value3")
    val json = Json.obj(
      "field1" -> "value1",
      "field2" -> value2
    )

    "return correct value if field is found" in {
      val result = testSubmissionCommon.getObjectFromJson("field2", json)
      result shouldBe value2
    }

    "return empty object if field is not found" in {
      val result = testSubmissionCommon.getObjectFromJson("field4", json)
      result shouldBe Json.obj()
    }
  }

  "getArrayFromJson" should {
    val value2 = Json.arr(1, 2, 3)

    val json = Json.obj(
      "field1" -> "value1",
      "field2" -> value2
    )

    "return correct value if field is found" in {
      val result = testSubmissionCommon.getArrayFromJson("field2", json)
      result shouldBe value2
    }

    "return empty array if field is not found" in {
      val result = testSubmissionCommon.getArrayFromJson("field4", json)
      result shouldBe Json.arr()
    }
  }

  "mergeSheetData" should {

    val configData: Config = Common.loadConfiguration("SIP", "SIP_Awards_V3", mockConfigUtils)

    "return json that contains merged sheet data" in {
      val oldJson: JsObject = Json.obj(
        "sharesAcquiredOrAwardedInYear" -> "true",
        "award" -> Json.obj(
          "awards" -> Json.arr(1, 2, 3, 4)
        )
      )
      val newJson: JsObject = Json.obj(
        "sharesAcquiredOrAwardedInYear" -> "true",
        "award" -> Json.obj(
          "awards" -> Json.arr(4, 5, 6)
        )
      )

      val result = testSubmissionCommon.mergeSheetData(configData.getConfig("data_location"), oldJson, newJson)
      result shouldBe Json.obj(
        "sharesAcquiredOrAwardedInYear" -> "true",
        "award" -> Json.obj(
          "awards" -> Json.arr(1, 2, 3, 4, 4, 5, 6)
        )
      )
    }

    "return new json if old one is empty" in {

      val oldJson: JsObject = Json.obj()

      val newJson: JsObject = Json.obj(
        "sharesAcquiredOrAwardedInYear" -> "true",
        "award" -> Json.obj(
          "awards" -> Json.arr(4, 5, 6)
        )
      )

      val result = testSubmissionCommon.mergeSheetData(configData.getConfig("data_location"), oldJson, newJson)
      result shouldBe newJson
    }

    "return empty json if new one is empty" in {
      val oldJson: JsObject = Json.obj(
        "sharesAcquiredOrAwardedInYear" -> "true",
        "award" -> Json.obj(
          "awards" -> Json.arr(1, 2, 3, 4)
        )
      )

      val newJson: JsObject = Json.obj()
      val result = testSubmissionCommon.mergeSheetData(configData.getConfig("data_location"), oldJson, newJson)
      result shouldBe Json.obj()
    }
  }

}
