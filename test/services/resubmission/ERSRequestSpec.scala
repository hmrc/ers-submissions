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

package services.resubmission

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Request

class ERSRequestSpec extends AnyWordSpecLike with Matchers {

  "ERSRequest" should {

    "createERSRequest" should {

      "create a valid Request[JsObject] instance" in {
        val request: Request[JsObject] = ERSRequest.createERSRequest()

        request shouldBe a[Request[_]]
      }

      "have correct connection properties" in {
        val request = ERSRequest.createERSRequest()
        val connection = request.connection

        connection.secure shouldBe false
        connection.clientCertificateChain shouldBe None
      }

      "have correct target properties" in {
        val request = ERSRequest.createERSRequest()
        val target = request.target

        target.uriString shouldBe "ers-submissions"
        target.path shouldBe "ers-submissions"
        target.queryMap shouldBe Map.empty[String, Seq[String]]
      }

      "have uri set to ers-submissions" in {
        val request = ERSRequest.createERSRequest()

        request.target.uri.toString shouldBe "ers-submissions"
      }

      "have empty body as JsObject" in {
        val request = ERSRequest.createERSRequest()

        request.body shouldBe Json.obj()
      }

      "have POST method" in {
        val request = ERSRequest.createERSRequest()

        request.method shouldBe "POST"
      }

      "have empty headers" in {
        val request = ERSRequest.createERSRequest()

        request.headers.headers shouldBe empty
      }

      "have valid attrs TypedMap" in {
        val request = ERSRequest.createERSRequest()

        request.attrs should not be null
      }

      "have empty version string" in {
        val request = ERSRequest.createERSRequest()

        request.version shouldBe ""
      }

      "create multiple independent request instances" in {
        val request1 = ERSRequest.createERSRequest()
        val request2 = ERSRequest.createERSRequest()

        request1 should not be theSameInstanceAs(request2)
        request1.method shouldBe request2.method
        request1.target.path shouldBe request2.target.path
      }

      "have non-secure connection" in {
        val request = ERSRequest.createERSRequest()

        request.connection.secure shouldBe false
      }

      "not have client certificate chain" in {
        val request = ERSRequest.createERSRequest()

        request.connection.clientCertificateChain shouldBe None
      }

      "have valid target URI structure" in {
        val request = ERSRequest.createERSRequest()

        request.target.uri should not be null
        request.target.uri.toString should not be empty
      }

      "have consistent path and uriString" in {
        val request = ERSRequest.createERSRequest()

        request.target.path shouldBe request.target.uriString
      }

      "have empty query parameters" in {
        val request = ERSRequest.createERSRequest()

        request.target.queryMap shouldBe Map.empty
      }

      "be usable as a Play Request" in {
        val request = ERSRequest.createERSRequest()

        // Should be able to call standard Request methods
        request.method shouldBe a[String]
        request.body shouldBe a[JsObject]
        request.headers should not be null
        request.attrs should not be null
      }

      "have correct content type expectations for JsObject body" in {
        val request = ERSRequest.createERSRequest()

        request.body shouldBe a[JsObject]
        Json.toJson(request.body) should not be null
      }

      "create request suitable for resubmission scenarios" in {
        val request = ERSRequest.createERSRequest()

        // Verify it's suitable for resubmission use case
        request.method shouldBe "POST"
        request.body shouldBe Json.obj()
        request.target.path should include("ers-submissions")
      }
    }

    "RemoteConnection" should {

      "return false for secure connection" in {
        val request = ERSRequest.createERSRequest()

        request.connection.secure shouldBe false
      }

      "return None for client certificate chain" in {
        val request = ERSRequest.createERSRequest()

        request.connection.clientCertificateChain shouldBe None
      }
    }

    "RequestTarget" should {

      "provide consistent URI representations" in {
        val request = ERSRequest.createERSRequest()
        val target = request.target

        target.uri.toString shouldBe target.uriString
        target.path shouldBe target.uriString
      }

      "provide empty query map" in {
        val request = ERSRequest.createERSRequest()

        request.target.queryMap shouldBe empty
      }
    }

    "Headers" should {

      "be empty by default" in {
        val request = ERSRequest.createERSRequest()

        request.headers.headers shouldBe empty
      }

      "be a valid Headers instance" in {
        val request = ERSRequest.createERSRequest()

        request.headers should not be null
      }
    }

    "TypedMap attrs" should {

      "contain valid entries" in {
        val request = ERSRequest.createERSRequest()

        request.attrs should not be null
      }

      "be a valid TypedMap instance" in {
        val request = ERSRequest.createERSRequest()

        request.attrs shouldBe a[play.api.libs.typedmap.TypedMap]
      }
    }
  }
}

