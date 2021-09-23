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
import javax.inject.Inject
import play.api.Logging
import play.api.libs.json.JsObject
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

import scala.concurrent.{ExecutionContext, Future}

class ADRConnector @Inject()(applicationConfig: ApplicationConfig,
                             http: HttpClient) extends Logging {

  def buildEtmpPath(path: String): String = s"${applicationConfig.adrBaseURI}/${path}"

  private def createHeaderCarrier = HeaderCarrier(
    extraHeaders = Seq(("Environment" -> applicationConfig.UrlHeaderEnvironment)),
    authorization = Some(Authorization(applicationConfig.UrlHeaderAuthorization))
  )

  def sendData(adrData: JsObject, schemeType: String)(implicit ec: ExecutionContext): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier
    val url: String = buildEtmpPath(s"${applicationConfig.adrFullSubmissionURI}/${schemeType.toLowerCase()}")

    logger.debug("Sending data to ADR.\n" +
      s"hc - headers: ${hc.extraHeaders.toString()}, authorization: ${hc.authorization.toString}\n" +
      s"url: $url")

    http.POST(url, adrData, headers = hc.headers(Seq("Authorization"))).map { res =>
      logger.warn(s"ADR response: $res")
      res
    }.recover {
      case ex: Exception =>
        logger.error("Exception in ADRConnector sending data to ADR" + ex.getMessage)
        throw ex
      case tex: Throwable =>
        logger.error("Throwable recovery in ADRConnector sending data to ADR" + tex.getMessage)
        throw tex
    }
  }
}
