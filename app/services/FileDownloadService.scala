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
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.Materializer
import akka.util.ByteString
import config.ApplicationConfig
import play.api.Logger
import akka.stream.alpakka.csv.scaladsl.CsvParsing
import javax.inject.Inject
import models.SubmissionsSchemeData

import scala.concurrent.Future

class FileDownloadService @Inject()(
                                   appConfig: ApplicationConfig
                                   )(implicit actorSystem: ActorSystem) {

  def extractEntityData(response: HttpResponse): Source[ByteString, _] = {
    val uploadCsvSizeLimit = appConfig.uploadCsvSizeLimit
    response match {
      case HttpResponse(akka.http.scaladsl.model.StatusCodes.OK, _, entity, _) => entity.withSizeLimit(uploadCsvSizeLimit).dataBytes
      case notOkResponse =>
        Logger.error(
          s"[ProcessCsvService][extractEntityData] Illegal response from Upscan: ${notOkResponse.status.intValue}, " +
            s"body: ${notOkResponse.entity.dataBytes}")
        Source.failed(new Exception("aaaa")) //TODO maybe touch this up
    }
  }

  def extractBodyOfRequest: Source[HttpResponse, _] => Source[Either[Throwable, List[String]], _] =
    _.flatMapConcat(extractEntityData)
      .via(CsvParsing.lineScanner())
      .via(Flow.fromFunction(bytestrings => Right(bytestrings.map(_.utf8String))))
      .recover {
        case e => Left(e)
      }

  def fileToSequenceOfEithers(schemeData: SubmissionsSchemeData): Future[Seq[Either[Throwable, Seq[String]]]] = {
    extractBodyOfRequest(streamFile(schemeData.data.downloadUrl))
      .takeWhile(_.isRight, inclusive = true)
      .runWith(Sink.seq[Either[Throwable, Seq[String]]])
  }

  private[services] def streamFile(downloadUrl: String): Source[HttpResponse, _] = {
    Source
      .single(HttpRequest(uri = downloadUrl))
      .mapAsync(parallelism = 1)(makeRequest)
  }

  private[services] def makeRequest(request: HttpRequest): Future[HttpResponse] = Http()(actorSystem).singleRequest(request)

}
