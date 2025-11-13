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

package services

import config.ApplicationConfig
import models.SubmissionsSchemeData
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.{HttpRequest, HttpResponse}
import org.apache.pekko.stream.connectors.csv.scaladsl.CsvParsing
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.http.Status
import uk.gov.hmrc.http.UpstreamErrorResponse
import utils.LoggingAndExceptions.ErsLogger

import javax.inject.Inject
import scala.concurrent.Future

class FileDownloadService @Inject()(appConfig: ApplicationConfig)(implicit actorSystem: ActorSystem) extends ErsLogger {

  def extractEntityData(response: HttpResponse): Source[ByteString, _] = {
    val uploadFileSizeLimit = appConfig.uploadFileSizeLimit
    response match {
      case HttpResponse(org.apache.pekko.http.scaladsl.model.StatusCodes.OK, _, entity, _) => entity.withSizeLimit(uploadFileSizeLimit).dataBytes
      case notOkResponse =>
        logError(
          s"[ProcessCsvService][extractEntityData] Illegal response from Upscan: ${notOkResponse.status.intValue}, " +
            s"body: ${notOkResponse.entity.dataBytes}")
        Source.failed(UpstreamErrorResponse("Could not download file from upscan", Status.INTERNAL_SERVER_ERROR))
    }
  }

  def extractBodyOfRequest: Source[HttpResponse, _] => Source[List[ByteString], _] =
    _.flatMapConcat(extractEntityData)
      .via(CsvParsing.lineScanner())

  def schemeDataToChunksWithIndex(schemeData: SubmissionsSchemeData, maxGroupSize: Int = appConfig.maxGroupSize): Source[(Seq[Seq[ByteString]], Long), _] = {
    extractBodyOfRequest(streamFile(schemeData.data.downloadUrl))
      .grouped(maxGroupSize)
      .zipWithIndex
  }

  private[services] def streamFile(downloadUrl: String): Source[HttpResponse, _] = {
    Source
      .single(HttpRequest(uri = downloadUrl))
      .mapAsync(parallelism = 1)(makeRequest)
  }

  private[services] def makeRequest(request: HttpRequest): Future[HttpResponse] = Http()(actorSystem).singleRequest(request)
}
