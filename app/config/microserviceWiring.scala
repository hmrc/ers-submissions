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

package config

import play.api.Play
import play.api.Play.current
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.auth.microservice.connectors.AuthConnector
import uk.gov.hmrc.play.config.{AppName, RunMode, ServicesConfig}
import uk.gov.hmrc.play.http.ws._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration

object WSHttp extends WSGet with WSPut with WSPost with WSDelete with WSPatch with AppName with RunMode with HttpAuditing {
  override val hooks = Seq(AuditingHook)
  override val auditConnector = MicroserviceAuditConnector
}

object WSHttpWithCustomTimeOut extends WSHttp with AppName with RunMode with HttpAuditing {
  override val hooks = Seq(AuditingHook)
  override val auditConnector = MicroserviceAuditConnector

  override def doPost[A](url: String, body: A, headers: Seq[(String,String)])(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
    val jsonbody = Json.toJson(body)
    buildRequest(url).withHeaders(headers: _*).post(jsonbody).map(new WSHttpResponse(_))
  }

  override def buildRequest[A](url: String)(implicit hc: HeaderCarrier) = {
    val ersTimeOut = (Play.configuration.getInt("ers-submissions-timeout-seconds").getOrElse(20)) * 1000
    super.buildRequest[A](url).withRequestTimeout(Duration(ersTimeOut.toString))
  }
}

object MicroserviceAuditConnector extends AuditConnector with RunMode {
  override lazy val auditingConfig = LoadAuditingConfig(s"$env.auditing")
}

object MicroserviceAuthConnector extends AuthConnector with ServicesConfig {
  override val authBaseUrl = baseUrl("auth")
}
