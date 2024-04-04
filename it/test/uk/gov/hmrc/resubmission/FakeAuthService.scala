/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.resubmission

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Suite}

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
