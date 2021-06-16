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

package services.resubmission

import java.net.{InetAddress, URI}
import java.security.cert.X509Certificate

import play.api.libs.json.{JsObject, Json}
import play.api.libs.typedmap.{TypedEntry, TypedKey, TypedMap}
import play.api.mvc.request.{RemoteConnection, RequestTarget}
import play.api.mvc.{Headers, Request}

object ERSRequest {

  def createERSRequest(): Request[JsObject] = {

    new Request[JsObject] {

       override def connection: RemoteConnection = new RemoteConnection {
         override def secure: Boolean = false
         override def clientCertificateChain: Option[Seq[X509Certificate]]  = None
         override def remoteAddress: InetAddress = ???
       }

      override def target: RequestTarget = new RequestTarget {
        override def uri: URI = new URI("ers-submissions")

        override def uriString: String = "ers-submissions"

        override def path: String = "ers-submissions"

        override def queryMap: Map[String, Seq[String]] = Map()
      }

      override def body: JsObject = Json.obj()

      override def method: String = "POST"

      override def headers: Headers = new Headers(Seq()) {
         protected val data: Seq[(String, Seq[String])] = Seq()
      }

      override def attrs: TypedMap = {
        TypedMap(
          TypedEntry(TypedKey("Id"), 1),
          TypedEntry(TypedKey("Tags"), Map())
        )
      }

      override def version: String = ""
    }

  }

}
