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

package services

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.alpakka.csv.scaladsl.CsvParsing
import akka.stream.scaladsl.Source
import akka.util.ByteString
import config.ApplicationConfig
import models.SubmissionsSchemeData
import play.api.Logging
import play.api.http.Status
import uk.gov.hmrc.http.UpstreamErrorResponse

import javax.inject.Inject
import scala.concurrent.Future

class FileDownloadService @Inject()(
                                   appConfig: ApplicationConfig
                                   )(implicit actorSystem: ActorSystem) extends Logging {

  def extractEntityData(response: HttpResponse): Source[ByteString, _] = {
    val uploadCsvSizeLimit = appConfig.uploadCsvSizeLimit
    response match {
      case HttpResponse(akka.http.scaladsl.model.StatusCodes.OK, _, entity, _) => entity.withSizeLimit(uploadCsvSizeLimit).dataBytes
      case notOkResponse =>
        logger.error(
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
