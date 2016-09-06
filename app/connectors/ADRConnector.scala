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

package connectors

import config.{ApplicationConfig, WSHttpWithCustomTimeOut}
import models.ADRTransferException
import play.Logger
import play.api.libs.json.JsObject
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.Authorization
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait ADRConnector extends ServicesConfig {
  def http: HttpPost

  def buildEtmpPath(path: String): String = s"${ApplicationConfig.adrBaseURI}/${path}"

  private def createHeaderCarrier = HeaderCarrier(
    extraHeaders = Seq(("Environment" -> ApplicationConfig.UrlHeaderEnvironment)),
    authorization = Some(Authorization(ApplicationConfig.UrlHeaderAuthorization))
  )

  def sendData(adrData: JsObject, schemeType: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    implicit val hc = createHeaderCarrier
    val url: String = buildEtmpPath(s"${ApplicationConfig.adrFullSubmissionURI}/${schemeType.toLowerCase()}")

    Logger.debug("Sending data to ADR.\n" +
      s"hc - headers: ${hc.extraHeaders.toString()}, authorization: ${hc.authorization.toString}\n" +
      s"url: ${url}")

    try {
      http.POST(url, adrData).map{ res =>
        Logger.warn(s"ADR response: ${res.status}")
        res
      }.recover {
        case ex: Exception => {
          Logger.error("Exception in ADRConnector sending data to ADR" + ex.getMessage)
          throw ex
        }
        case tex: Throwable => {
          Logger.error("Throwable recovery in ADRConnector sending data to ADR" + tex.getMessage)
          throw tex
        }
      }
    }
    catch {
      case ex: Throwable => {
        Logger.error("Throwable try/catch in ADRConnector sending data to ADR" + ex.getMessage)
        throw ex
      }
    }
  }
}

object ADRConnector extends ADRConnector {
  override def http: HttpPost = WSHttpWithCustomTimeOut
}
