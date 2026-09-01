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

import cats.data.EitherT
import cats.syntax.all._
import com.typesafe.config.ConfigFactory
import common.ERSEnvelope.ERSEnvelope
import config.ApplicationConfig
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import utils.{CorrelationIdHelper, ErrorHandlerHelper}
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.MimeTypes.JSON

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ADRConnector @Inject() (applicationConfig: ApplicationConfig, http: HttpClientV2)
    extends ErrorHandlerHelper with CorrelationIdHelper {

  override val className: String  = getClass.getSimpleName
  private val headerCarrierConfig = HeaderCarrier.Config.fromConfig(ConfigFactory.load())

  private def buildEtmpPath(path: String): String = s"${applicationConfig.adrBaseURI}/$path"

  private def explicitHeaders()(implicit hc: HeaderCarrier): scala.Seq[(String, String)] = scala.Seq(
    "Environment"   -> applicationConfig.UrlHeaderEnvironment,
    "Authorization" -> applicationConfig.UrlHeaderAuthorization
  ) ++ hc.headers(scala.Seq(HEADER_X_CORRELATION_ID))

  def sendData(adrData: JsObject, schemeType: String)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): ERSEnvelope[HttpResponse] = EitherT {
    val url: String       = buildEtmpPath(s"${applicationConfig.adrFullSubmissionURI}/${schemeType.toLowerCase()}")
    val headersForRequest = hc
      .withExtraHeaders(explicitHeaders(): _*)
      .headersForUrl(headerCarrierConfig)(url)

    http
      .post(url"$url")
      .withBody(adrData)
      .setHeader(headersForRequest: _*)
      .execute[HttpResponse]
      .map(_.asRight)
      .recover { case ex =>
        Left(handleError(ex, "sendData"))
      }
  }

  def sendDataStreamed(adrData: JsObject, schemeType: String)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): ERSEnvelope[HttpResponse] = EitherT {
    val url: String                       = buildEtmpPath(s"${applicationConfig.adrFullSubmissionURI}/${schemeType.toLowerCase()}")
    val payload: ByteString               = ByteString(Json.stringify(adrData))
    logInfo(
      s"[ADRConnector][sendDataStreamed] Streaming ${payload.length} bytes as a single chunk for scheme type [$schemeType]"
    )
    val streamData: Source[ByteString, _] = Source.single(payload)
    val headersForRequest                 = hc
      .withExtraHeaders(explicitHeaders(): _*)
      .headersForUrl(headerCarrierConfig)(url)

    http
      .post(url"$url")
      .setHeader(headersForRequest: _*)
      .setHeader(CONTENT_TYPE -> JSON)
      .withBody(streamData)
      .execute[HttpResponse]
      .map(_.asRight)
      .recover { case ex =>
        Left(handleError(ex, "sendDataStreamed"))
      }
  }

}
