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

import uk.gov.hmrc.play.test.UnitSpec
import utils.LoggingAndRexceptions.ErsLogger

class ErsLoggerSpec extends UnitSpec {

  object TestErsLogger extends ErsLogger {
    override def buildExceptionMesssage(ex: Exception): String = "exception message"

    override val buildDataMessage: PartialFunction[Object, String] = {
      case _ => "data message"
    }
  }

  "calling buildMessage" should {
    "merge message and data message if additional data is given" in {
      val result = TestErsLogger.buildMessage("message", Some("data"))
      result shouldBe "message for data message"
    }

    "return message if additional data is not given" in {
      val result = TestErsLogger.buildMessage("message", None)
      result shouldBe "message"
    }
  }

  "calling logException" should {

    "log error without context if it's not given" in {
      val result = TestErsLogger.logException("data", new Exception("exception message"))
      result shouldBe (())
    }

    "log error with context if it's given" in {
      val result = TestErsLogger.logException("data", new Exception("exception message"), Some("context"))
      result shouldBe (())
    }

  }

  "calling logError" should {
    "log error" in {
      val result = TestErsLogger.logError("message")
      result shouldBe (())
    }
  }

  "calling logWarn" should {
    "log warn" in {
      val result = TestErsLogger.logWarn("message")
      result shouldBe (())
    }
  }

  "calling logSliced" should {
    "log list data in chuncks" in {
      val testData = List(
        List.fill(20)("test"),
        List.fill(21)("test")
      )
      testData.map { testList =>
        val result = TestErsLogger.logSliced(testList, "length message", "data message")
        result shouldBe (())
      }
    }
  }
}
