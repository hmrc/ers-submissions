/*
 * Copyright 2022 HM Revenue & Customs
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

package helpers

import java.nio.charset.Charset

import akka.stream.Materializer
import akka.util.ByteString
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.Helpers.stubControllerComponents

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future}

trait ERSTestHelper extends AnyWordSpecLike with Matchers with OptionValues with MockitoSugar with GuiceOneAppPerSuite {

  val mockCc: ControllerComponents = stubControllerComponents()
  implicit def materializer: Materializer = Play.materializer(fakeApplication)
  implicit val ec: ExecutionContext = mockCc.executionContext

  def status(result: Future[Result]): Int = Await.result(result, 10.seconds).header.status

  def bodyOf(result: Result): String = {
    val bodyBytes: ByteString = Await.result(result.body.consumeData, 10.seconds)
    bodyBytes.decodeString(Charset.defaultCharset().name)
  }

  def bodyOf(resultF: Future[Result]): Future[String] = {
    resultF.map(bodyOf)
  }

  def await[T](future: Future[T], timeout: FiniteDuration = 10.seconds): T = Await.result(future, timeout)

}
