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

package helpers

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.util.ByteString
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.ApplicationLifecycle
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.Helpers.stubControllerComponents
import play.api.{Application, Configuration, Play, inject}
import scheduler.SchedulingActor.UpdateDocumentsClass
import scheduler.{SchedulingActor, UpdateCreatedAtFieldsJob}
import services.DocumentUpdateService

import java.nio.charset.Charset
import javax.inject.Inject
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future}

trait ERSTestHelper extends AnyWordSpecLike with Matchers with OptionValues with MockitoSugar with GuiceOneAppPerSuite {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        inject.bind[DocumentUpdateService].to[FakeDocumentUpdateService],
        inject.bind[UpdateCreatedAtFieldsJob].to[FakeUpdateCreatedAtFieldsJob]
      )
      .build()

  val mockCc: ControllerComponents = stubControllerComponents()
  implicit def materializer: Materializer = Play.materializer(fakeApplication())
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

class FakeDocumentUpdateService extends DocumentUpdateService {
  override val jobName: String = "update-created-at-field-job"

  override def invoke(implicit ec: ExecutionContext): Future[Long] = Future.successful(2)
}

class FakeUpdateCreatedAtFieldsJob @Inject()(
                                              val config: Configuration,
                                              val service: FakeDocumentUpdateService,
                                              val applicationLifecycle: ApplicationLifecycle
) extends UpdateCreatedAtFieldsJob {

  override def jobName: String = "update-created-at-field-job"
  override val scheduledMessage: SchedulingActor.ScheduledMessage[_] = UpdateDocumentsClass(service)
  override val actorSystem: ActorSystem = ActorSystem(jobName)
}
