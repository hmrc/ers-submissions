/*
 * Copyright 2020 HM Revenue & Customs
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

import akka.actor.ActorSystem
import com.typesafe.config.Config
import play.api.Play.current
import play.api.libs.json.{Json, Writes}
import play.api.{Configuration, Play}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.auth.microservice.connectors.AuthConnector
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.http.ws._
import uk.gov.hmrc.play.microservice.config.LoadAuditingConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._


trait WSHttp extends WSGet with HttpGet with HttpPatch with HttpPut with HttpDelete with HttpPost with WSPut with WSPost with WSDelete with WSPatch with AppName with HttpAuditing {
  override val hooks = Seq(AuditingHook)
  override val auditConnector = MicroserviceAuditConnector

  protected def mode: play.api.Mode.Mode = Play.current.mode
  protected def runModeConfiguration: play.api.Configuration = Play.current.configuration
  override protected def configuration: Option[Config] = Some(Play.current.configuration.underlying)
  override protected def appNameConfiguration: Configuration = Play.current.configuration
  override protected def actorSystem : ActorSystem =  akka.actor.ActorSystem()
}
object WSHttp extends WSHttp

object WSHttpWithCustomTimeOut extends WSHttp with AppName with HttpAuditing {
  override val hooks = Seq(AuditingHook)
  override val auditConnector = MicroserviceAuditConnector

  override def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
    val jsonbody = Json.toJson(body)
    buildRequest(url).withHeaders(headers: _*).post(jsonbody).map(new WSHttpResponse(_))
  }

  override def buildRequest[A](url: String)(implicit hc: HeaderCarrier) = {
    val ersTimeOut = Play.configuration.getInt("ers-submissions-timeout-seconds").getOrElse(20).seconds
    super.buildRequest[A](url).withRequestTimeout(ersTimeOut)
  }
}

object MicroserviceAuditConnector extends AuditConnector {
  override lazy val auditingConfig = LoadAuditingConfig("auditing")
}

object MicroserviceAuthConnector extends AuthConnector with ServicesConfig with WSHttp {
  override val authBaseUrl = baseUrl("auth")
}
