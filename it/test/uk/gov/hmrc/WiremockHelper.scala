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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Suite}

trait WiremockHelper {
  val wiremockPort = 11111
  val wiremockHost = "localhost"
  val url = s"http://$wiremockHost:$wiremockPort"

  lazy val wmConfig: WireMockConfiguration = wireMockConfig().port(wiremockPort)
  lazy val wireMockServer = new WireMockServer(wmConfig)

  def startWiremock(): Unit = {
    wireMockServer.start()
    WireMock.configureFor(wiremockHost, wiremockPort)
  }

  def stopWiremock(): Unit = wireMockServer.stop()

  def resetWiremock(): Unit = WireMock.reset()
}

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

  def verifyPostedTo(path: String): Unit = {
    stubServer.verify(postRequestedFor(urlPathEqualTo(path)))
  }
}
