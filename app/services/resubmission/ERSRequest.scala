/*
 * Copyright 2016 HM Revenue & Customs
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

import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Headers, Request}

object ERSRequest {

  def createERSRequest(): Request[JsObject] = {

    new Request[JsObject] {
      override def body: JsObject = Json.obj()

      override def secure: Boolean = false

      override def uri: String = "ers-submissions"

      override def queryString: Map[String, Seq[String]] = Map()

      override def remoteAddress: String = ""

      override def method: String = "POST"

      override def headers: Headers = new Headers {
        override protected val data: Seq[(String, Seq[String])] = Seq()
      }

      override def path: String = "ers-submissions"

      override def version: String = ""

      override def tags: Map[String, String] = Map()

      override def id: Long = 1
    }

  }

}
