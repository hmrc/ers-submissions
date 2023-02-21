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

package uk.gov.hmrc

import _root_.play.api.Configuration
import _root_.play.api.inject.ApplicationLifecycle
import akka.actor.ActorSystem
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Suite}
import scheduler.SchedulingActor.UpdateDocumentsClass
import scheduler.{SchedulingActor, UpdateCreatedAtFieldsJob}
import services.DocumentUpdateService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait FakeAuthService extends BeforeAndAfterAll with ScalaFutures {
  this: Suite =>

  lazy val authServiceHost = "localhost"
  lazy val authServicePort = 18500

  lazy val authServer = new WireMockServer(wireMockConfig().port(authServicePort))

  final lazy val authServiceBaseUrl = s"http://$authServiceHost:$authServicePort"

  lazy val downloadServer = new WireMockServer(wireMockConfig().port(19000))

  override def beforeAll(): Unit = {
    super.beforeAll()
    authServer.start()
    downloadServer.start()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    authServer.stop()
    downloadServer.stop()
  }

  authServer.stubFor(WireMock.post(urlMatching("/auth/authorise")).willReturn(WireMock.aResponse().withStatus(200).withBody("""{}""")))
  downloadServer.stubFor(WireMock.get(urlMatching("/fakeDownload")).willReturn(WireMock.aResponse().withStatus(200)
    .withBody(""""no", "no", "yes", "3", "2015-12-09", "John", "", "Doe", "AA123456A", "123/XZ55555555", "10.1234", "100.12", "10.1234", "10.1234"""")))
}

trait FakeErsStubService extends BeforeAndAfterAll with ScalaFutures {
  this: Suite =>

  lazy val stubServiceHost = "localhost"
  lazy val stubServicePort = 19339

  lazy val stubServer = new WireMockServer(wireMockConfig().port(stubServicePort))

  final lazy val stubServiceBaseUrl = s"http://$stubServiceHost:$stubServicePort"

  override def beforeAll(): Unit = {
    super.beforeAll()
    stubServer.start()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    stubServer.stop()
  }

  stubServer.stubFor(WireMock.post(urlMatching("/.*")).willReturn(WireMock.aResponse().withStatus(202)))
}

class FakeDocumentUpdateService extends DocumentUpdateService {
  override val jobName: String = "UpdateCreatedAtFieldJob"

  override def invoke(implicit ec: ExecutionContext): Future[Long] = Future.successful(2)
}

class FakeUpdateCreatedAtFieldsJob @Inject()(
                                              val config: Configuration,
                                              val service: FakeDocumentUpdateService,
                                              val applicationLifecycle: ApplicationLifecycle
                                            ) extends UpdateCreatedAtFieldsJob {
  override def jobName: String = "UpdateCreatedAtFieldJob"
  override val scheduledMessage: SchedulingActor.ScheduledMessage[_] = UpdateDocumentsClass(service)
  override val actorSystem: ActorSystem = ActorSystem(jobName)
}
