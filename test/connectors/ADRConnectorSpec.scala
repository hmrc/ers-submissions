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

package connectors

import config.ApplicationConfig
import fixtures.Fixtures
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.JsObject
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class ADRConnectorSpec extends UnitSpec with MockitoSugar with GuiceOneAppPerSuite {

  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  val mockHttpClient: HttpClient = mock[HttpClient]

  val mockConnector = new ADRConnector(mockAppConfig, mockHttpClient)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "calling sendData" should {

    "send data successfully" in {
      when(mockHttpClient.POST[JsObject, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK)))

      val result = await(mockConnector.sendData(Fixtures.schemeDataJson, Fixtures.schemeType))
      result.status shouldBe OK
    }

    "fail sending data" in {

      when(mockHttpClient.POST[JsObject, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))

      val result = await(mockConnector.sendData(Fixtures.schemeDataJson, Fixtures.schemeType))
      result.status shouldBe INTERNAL_SERVER_ERROR
    }
  }

}
