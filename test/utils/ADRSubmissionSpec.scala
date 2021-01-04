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

package utils

import com.typesafe.config.Config
import fixtures.Fixtures
import models.{ADRTransferException, ErsSummary, SchemeInfo}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import services.PresubmissionService
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggingAndRexceptions.ADRExceptionEmitter

class ADRSubmissionSpec extends UnitSpec with MockitoSugar with GuiceOneAppPerSuite with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = new HeaderCarrier()
  implicit val request = FakeRequest().withBody(Fixtures.metadataJson)
  val mockSubmissionCommon: SubmissionCommon = mock[SubmissionCommon]
  val mockPresubmissionService: PresubmissionService = mock[PresubmissionService]
  val mockAdrExceptionEmitter: ADRExceptionEmitter = app.injector.instanceOf[ADRExceptionEmitter]
  val mockConfig: Config = mock[Config]
  when(mockConfig.getConfig(anyString())).thenReturn(mock[Config])

  val mockConfigUtils: ConfigUtils = mock[ConfigUtils]
  when(mockConfigUtils.getConfigData(anyString(), anyString())(any[Request[_]](), any[HeaderCarrier](), any[ErsSummary]()))
    .thenReturn(mockConfig)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockPresubmissionService)
  }

  val nilReturnJson: JsObject = Json.obj(
    "submissionReturn" -> Json.obj(
      "submitANilReturn" -> true
    )
  )

  val notNilReturnJson: JsObject = Json.obj(
    "submissionReturn" -> Json.obj(
      "submitANilReturn" -> false
    )
  )

  val sheetsJson: JsObject = Json.obj(
    "optionsGrantedInYear" -> true,
    "grant" -> Json.obj(
      "grants" -> Json.arr(
        Json.obj(
          "dateOfGrant" -> "2015-12-09",
          "numberOfIndividuals" -> 1
        ),
        Json.obj(
          "dateOfGrant" -> "2015-12-10",
          "numberOfIndividuals" -> 2
        )
      )
    )
  )

  "calling generateSubmission" should {

    val mockAdrSubmission: ADRSubmission = new ADRSubmission(
      mockSubmissionCommon,
      mockPresubmissionService,
      mockAdrExceptionEmitter,
      mockConfigUtils
    ) {

      override def createSubmissionJson()(implicit request: Request[_], hc: HeaderCarrier, ersSummary: ErsSummary, schemeType: String): Future[JsObject] = Future.successful(notNilReturnJson)

      override def createRootJson(sheetsJson: JsObject)(implicit request: Request[_], hc: HeaderCarrier, ersSummary: ErsSummary, schemeType: String): JsObject = nilReturnJson

    }

    "return createSubmissionJson result for not NilReturn submission" in {
      val result = await(mockAdrSubmission.generateSubmission()(request, hc, Fixtures.metadata))
      result shouldBe notNilReturnJson
    }

    "return createRootJson result for NilReturn submission" in {
      val result = await(mockAdrSubmission.generateSubmission()(request, hc, Fixtures.metadataNilReturn))
      result shouldBe nilReturnJson
    }

  }

  "calling createSubmissionJson" should {

    val mockAdrSubmission: ADRSubmission = new ADRSubmission(
      mockSubmissionCommon,
      mockPresubmissionService,
      mockAdrExceptionEmitter,
      mockConfigUtils
    ) {
      override def createSheetsJson(sheetsJson: JsObject)(implicit request: Request[_], hc: HeaderCarrier, ersSummary: ErsSummary, schemeType: String): Future[JsObject] = Future.successful(sheetsJson)

      override def createRootJson(sheetsJson: JsObject)(implicit request: Request[_], hc: HeaderCarrier, ersSummary: ErsSummary, schemeType: String): JsObject = notNilReturnJson
    }

    "return the result of createRootJson" in {

      val result = await(mockAdrSubmission.createSubmissionJson()(request, hc, Fixtures.metadata, Fixtures.schemeType))
      result shouldBe notNilReturnJson

    }

  }

  "calling createSheetsJson" should {

    val mockAdrSubmission: ADRSubmission = new ADRSubmission(
      mockSubmissionCommon,
      mockPresubmissionService,
      mockAdrExceptionEmitter,
      mockConfigUtils
    ) {
      when(mockSubmissionCommon.mergeSheetData(any[Config](), any[JsObject], any[JsObject])).thenReturn(sheetsJson)

      override def buildJson(configData: Config, fileData: ListBuffer[Seq[String]], row: Option[Int] = None)(implicit request: Request[_], hc: HeaderCarrier): JsObject = sheetsJson
    }

    "return given json as parameter if there is no data in the database" in {
      when(mockPresubmissionService.getJson(any[SchemeInfo]())).thenReturn(Future.successful(List()))

      val result = await(mockAdrSubmission.createSheetsJson(sheetsJson)(request, hc, Fixtures.metadata, Fixtures.schemeType))
      result shouldBe sheetsJson
    }

    "return merged data json if there is data in the database" in {
      when(mockPresubmissionService.getJson(any[SchemeInfo]()))
        .thenReturn(Future.successful(List(Fixtures.schemeData)))

      val result = await(mockAdrSubmission.createSheetsJson(Json.obj())(request, hc, Fixtures.metadata, Fixtures.schemeType))
      result shouldBe sheetsJson
    }

    "throws adrException if retrieving data throws exception" in {
      when(mockPresubmissionService.getJson(any[SchemeInfo]()))
        .thenReturn(Future.failed(new Exception("errorMessage")))

      intercept[ADRTransferException] {
        await(mockAdrSubmission.createSheetsJson(Json.obj())(request, hc, Fixtures.metadata, Fixtures.schemeType))
      }
    }
  }

  "calling createRootJson" should {

    val mockAdrSubmission: ADRSubmission = new ADRSubmission(
      mockSubmissionCommon,
      mockPresubmissionService,
      mockAdrExceptionEmitter,
      mockConfigUtils
    ) {
      override def buildRoot(configData: Config, metadata: Object, sheetsJson: JsObject)(implicit request: Request[_], hc: HeaderCarrier, ersSummary: ErsSummary, schemeType: String): JsObject = nilReturnJson
    }

    "return the result of buildRoot" in {

      val result = await(mockAdrSubmission.createRootJson(Json.obj())(request, hc, Fixtures.metadataNilReturn, Fixtures.schemeType))
      result shouldBe nilReturnJson
    }

  }

}
