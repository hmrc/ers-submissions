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
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse, StatusCodes}
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import akka.util.ByteString
import config.ApplicationConfig
import fixtures.SIP
import models.{SchemeInfo, SubmissionsSchemeData, UpscanCallback}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class FileDownloadServiceSpec extends TestKit(ActorSystem("FileDownloadServiceSpec"))
  with UnitSpec with MockitoSugar with WithFakeApplication {

  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  when(mockAppConfig.uploadCsvSizeLimit).thenReturn(10000)

  "fileDownloadService" should {
    "extract entity data" when {
      "given a status 200 response" in {
        val testService = new FileDownloadService(mockAppConfig)
        val response: HttpResponse = HttpResponse(StatusCodes.OK, entity = HttpEntity.apply("gotABody"))

        val result = Await.result(
          testService.extractEntityData(response).runWith(Sink.seq),
          Duration.Inf
        )

        result.length shouldBe 1
        result.head.utf8String shouldBe "gotABody"

      }

      "given an unhappy response" in {
        val testService = new FileDownloadService(mockAppConfig)
        val response: HttpResponse = HttpResponse(StatusCodes.NotFound, entity = HttpEntity.apply("gotABody"))

        val result = testService.extractEntityData(response).runWith(Sink.seq)

        ScalaFutures.whenReady(result.failed) { e =>
          e shouldBe a[Exception] // TODO elaborate on this?
        }

      }
    }

    "extract body of request" in {
      val testService = new FileDownloadService(mockAppConfig) {
        override def extractEntityData(response: HttpResponse): Source[ByteString, _] =
          Source.fromIterator(() => Seq(ByteString("aSingleRow,withTwoEntries\n"), ByteString("anotherRow")).toIterator)
      }
      val response: HttpResponse = HttpResponse(StatusCodes.OK, entity = HttpEntity.apply("gotABody"))

      val result: Seq[List[ByteString]] = Await.result(
        testService.extractBodyOfRequest(Source.single(response)).runWith(Sink.seq),
        Duration.Inf
      )

      result.length shouldBe 2
      result.head.map(_.utf8String) shouldBe List("aSingleRow", "withTwoEntries")
      result.last.map(_.utf8String) shouldBe List("anotherRow")
    }

    "convert file to sequence of eithers" when {
      val submissionsSchemeData: SubmissionsSchemeData = SubmissionsSchemeData(SIP.schemeInfo, "sip sheet name",
        UpscanCallback("name", "/download/url"))

      val testService: FileDownloadService = new FileDownloadService(mockAppConfig) {
        override def extractBodyOfRequest: Source[HttpResponse, _] => Source[List[ByteString], _] = {
          _ =>
            Source.fromIterator(() => List(
              List(
                ByteString("one"),
                ByteString("two")),
              List(
                ByteString("three"),
                ByteString("four"),
                ByteString("five"))
            ).toIterator)
        }

        override def streamFile(downloadUrl: String): Source[HttpResponse, _] =
          Source.single(HttpResponse(StatusCodes.OK, entity = HttpEntity.apply("gotABody")))
      }

      "file has less than grouping-size rows" in {
        val result = Await.result(testService
          .fileToSequenceOfEithers(submissionsSchemeData, 10)
          .runWith(Sink.seq),
          Duration.Inf
        )


        result.length shouldBe 1
        result.head._2 shouldBe 0
        result.head._1.length shouldBe 2
        result.head._1.head.map(_.utf8String) shouldBe Seq("one", "two")
        result.head._1.last.map(_.utf8String) shouldBe Seq("three", "four", "five")
      }

      "file has more than grouping-size rows" in {
        val result = Await.result(testService
          .fileToSequenceOfEithers(submissionsSchemeData, 1)
          .runWith(Sink.seq),
          Duration.Inf
        )


        result.length shouldBe 2
        result.head._2 shouldBe 0
        result.head._1.length shouldBe 1
        result.head._1.head.map(_.utf8String) shouldBe Seq("one", "two")

        result.last._2 shouldBe 1
        result.last._1.length shouldBe 1
        result.last._1.last.map(_.utf8String) shouldBe Seq("three", "four", "five")
      }
    }

    "streamFile" should {
      "process file" in {
        val testService = new FileDownloadService(mockAppConfig) {
          override def makeRequest(request: HttpRequest): Future[HttpResponse] = Future.successful(HttpResponse(StatusCodes.OK))
        }

        val result = Await.result(testService.streamFile("http://thisIsNot.aRealPage").runWith(Sink.seq),
          Duration.Inf)

        result.length shouldBe 1
        result.head shouldBe HttpResponse(StatusCodes.OK)
      }
    }

  }

}
