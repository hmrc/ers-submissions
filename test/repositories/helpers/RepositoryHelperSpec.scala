/*
 * Copyright 2025 HM Revenue & Customs
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

package repositories.helpers

import models.{MongoGenericError, MongoUnavailableError}
import org.mongodb.scala.{MongoSocketWriteException, ServerAddress}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class RepositoryHelperSpec extends AnyWordSpec with Matchers {

  class TestRepositoryHelper extends RepositoryHelper

  val helper = new TestRepositoryHelper()

  "mongoRecover" should {

    val repository = "TestRepository"
    val method = "testMethod"
    val message = "Test operation failed"
    val sessionId = "session-123"

    "return true for isDefinedAt  given an Exception" in {
      val partialFunction = helper.mongoRecover[String](repository, method, message, sessionId)
      partialFunction.isDefinedAt(new Exception("test")) shouldBe true
    }

    "return MongoUnavailableError given a MongoSocketWriteException" in {
      val mongoSocketWriteException = new MongoSocketWriteException(
        "Connection refused",
        new ServerAddress(),
        new RuntimeException("Socket write error")
      )

      val result = helper.mongoRecover[String](repository, method, message, sessionId)(mongoSocketWriteException)

      result.left.toOption.get shouldBe MongoUnavailableError("Connection refused")
    }

    "return MongoGenericError given a RuntimeException" in {
      val genericException = new RuntimeException("Generic error")

      val result = helper.mongoRecover[String](repository, method, message, sessionId)(genericException)

      result.left.toOption.get shouldBe MongoGenericError("Generic error")
    }

  }
}