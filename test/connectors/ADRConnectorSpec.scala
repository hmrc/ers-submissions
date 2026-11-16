/*
 * Copyright 2026 HM Revenue & Customs
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

package connectors

import config.ApplicationConfig
import fixtures.Fixtures
import org.mockito.Mockito.when
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.http.Fault
import models.{ADRTransferError, ERSError}
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext.Implicits.global

class ADRConnectorSpec
  extends AnyWordSpecLike
    with MockitoSugar
    with Matchers
    with WireMockSupport
    with HttpClientV2Support
    with ScalaFutures {

  val mockApplicationConfig: ApplicationConfig    = mock[ApplicationConfig]
  when(mockApplicationConfig.adrFullSubmissionURI).thenReturn("etmpFullSubmit")
  when(mockApplicationConfig.adrBaseURI).thenReturn(s"http://$wireMockHost:$wireMockPort/ers-stub")

  val connector = new ADRConnector(mockApplicationConfig, httpClientV2)
  given hc: HeaderCarrier = new HeaderCarrier()

  "sendData handle error correctly" in {
    stubFor(post(urlEqualTo("/ers-stub/etmpFullSubmit/emi")).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)))

    connector.sendData(Fixtures.metadataJson, "emi").value.futureValue match {
      case Left(error: ERSError) => error shouldBe ADRTransferError()
      case Right(resp: HttpResponse) => fail(s"Expected Left but got Right with status ${resp.status}")
    }
  }

  "sendDataStreamed handle error correctly" in {
    stubFor(post(urlEqualTo("/ers-stub/etmpFullSubmit/emi")).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)))

    connector.sendDataStreamed(Fixtures.metadataJson, "emi").value.futureValue match {
      case Left(error: ERSError) => error shouldBe ADRTransferError()
      case Right(resp: HttpResponse) => fail(s"Expected Left but got Right with status ${resp.status}")
    }
  }
}
