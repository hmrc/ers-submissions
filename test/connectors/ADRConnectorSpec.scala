/*
 * Copyright 2017 HM Revenue & Customs
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

import fixtures.Fixtures
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.JsObject
import uk.gov.hmrc.play.http.{HttpResponse, HttpPost, HeaderCarrier}
import uk.gov.hmrc.play.test.UnitSpec
import play.api.test.FakeApplication
import play.api.test.Helpers._
import scala.concurrent.Future

class ADRConnectorSpec extends UnitSpec with MockitoSugar {

  def buildADRConnector(postResult: Option[Boolean] = None) = new ADRConnector {

    val mockPostHttp = mock[HttpPost]
    when(mockPostHttp.POST[JsObject, HttpResponse](any(), any(), any())(any(), any(), any())).thenReturn(postResult match {
      case Some(true) => Future.successful(HttpResponse(200))
      case Some(false) => Future.successful(HttpResponse(500))
      case _ => Future.failed(new RuntimeException)
    })

    override def http: HttpPost = mockPostHttp
  }

  implicit val hc: HeaderCarrier = new HeaderCarrier()

  "calling sendData" should {

    "send data successfully" in {
      running(FakeApplication()) {
        val adrConnector = buildADRConnector(Some(true))
        val result = await(adrConnector.sendData(Fixtures.schemeDataJson, Fixtures.schemeType))
        result.status shouldBe OK
      }
    }

    "fail sending data" in {
      running(FakeApplication()) {
        val adrConnector = buildADRConnector(Some(false))
        val result = await(adrConnector.sendData(Fixtures.schemeDataJson, Fixtures.schemeType))
        result.status shouldBe INTERNAL_SERVER_ERROR
      }
    }

  }

}
