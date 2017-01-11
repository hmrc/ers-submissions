/*
 * Copyright 2017 HM Revenue & Customs
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
import models.{ADRTransferException, SchemeInfo, ErsSummary}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.{Json, JsObject}
import play.api.mvc.Request
import play.api.test.FakeRequest
import services.PresubmissionService
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class ADRSubmissionSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  implicit val hc: HeaderCarrier = new HeaderCarrier()
  implicit val request = FakeRequest().withBody(Fixtures.metadataJson)

  val mockConfig: Config = mock[Config]
  when(
    mockConfig.getConfig(anyString())
  ).thenReturn(
    mock[Config]
  )

  val mockConfigUtils: ConfigUtils = mock[ConfigUtils]
  when(
    mockConfigUtils.getConfigData(anyString(), anyString())(any[Request[_]](), any[HeaderCarrier](), any[ErsSummary]())
  ).thenReturn(
    mockConfig
  )

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
    
    val adrSubmission: ADRSubmission = new ADRSubmission {

      override val presubmissionService: PresubmissionService = mock[PresubmissionService]

      override val submissionCommon: SubmissionCommon = mock[SubmissionCommon]

      override val configUtils: ConfigUtils = mockConfigUtils

      override def createSubmissionJson()(implicit request: Request[_], hc: HeaderCarrier, ersSummary: ErsSummary, schemeType: String): Future[JsObject] = Future.successful(notNilReturnJson)

      override def createRootJson(sheetsJson: JsObject)(implicit request: Request[_], hc: HeaderCarrier, ersSummary: ErsSummary, schemeType: String): JsObject = nilReturnJson

    }

    "return createSubmissionJson result for not NilReturn submission" in {
      val result = await(adrSubmission.generateSubmission()(request, hc, Fixtures.metadata))
      result shouldBe notNilReturnJson
    }

    "return createRootJson result for NilReturn submission" in {
      val result = await(adrSubmission.generateSubmission()(request, hc, Fixtures.metadataNilReturn))
      result shouldBe nilReturnJson
    }

  }

  "calling createSubmissionJson" should {

    val adrSubmission: ADRSubmission = new ADRSubmission {

      override val presubmissionService: PresubmissionService = mock[PresubmissionService]

      override val submissionCommon: SubmissionCommon = mock[SubmissionCommon]

      override val configUtils: ConfigUtils = mockConfigUtils

      override def createSheetsJson(sheetsJson: JsObject)(implicit request: Request[_], hc: HeaderCarrier, ersSummary: ErsSummary, schemeType: String): Future[JsObject] = Future.successful(sheetsJson)

      override def createRootJson(sheetsJson: JsObject)(implicit request: Request[_], hc: HeaderCarrier, ersSummary: ErsSummary, schemeType: String): JsObject = notNilReturnJson

    }

    "return the result of createRootJson" in {

      val result = await(adrSubmission.createSubmissionJson()(request, hc, Fixtures.metadata, Fixtures.schemeType))
      result shouldBe notNilReturnJson

    }
    
  }

  "calling createSheetsJson" should {

    val mockPresubmissionService: PresubmissionService = mock[PresubmissionService]

    val adrSubmission: ADRSubmission = new ADRSubmission {

      override val presubmissionService: PresubmissionService = mockPresubmissionService

      val mockSubmissionCommon: SubmissionCommon = mock[SubmissionCommon]

      when(
        mockSubmissionCommon.mergeSheetData(any[Config](), any[JsObject], any[JsObject])
      ).thenReturn(
        sheetsJson
      )

      override val submissionCommon: SubmissionCommon = mockSubmissionCommon

      override val configUtils: ConfigUtils = mockConfigUtils

      override def buildJson(configData: Config, fileData: ListBuffer[Seq[String]], row: Option[Int] = None)(implicit request: Request[_], hc: HeaderCarrier): JsObject = sheetsJson

    }

    "return given json as parameter if there is no data in the database" in {

      reset(mockPresubmissionService)
      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(List())
      )

      val result = await(adrSubmission.createSheetsJson(sheetsJson)(request, hc, Fixtures.metadata, Fixtures.schemeType))
      result shouldBe sheetsJson

    }

    "return merged data json if there is data in the database" in {

      reset(mockPresubmissionService)
      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(List(Fixtures.schemeData))
      )

      val result = await(adrSubmission.createSheetsJson(Json.obj())(request, hc, Fixtures.metadata, Fixtures.schemeType))
      result shouldBe sheetsJson

    }

    "throws adrException if retrieving data throws exception" in {

      reset(mockPresubmissionService)
      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.failed(new Exception("errorMessage"))
      )

      intercept[ADRTransferException] {
        await(adrSubmission.createSheetsJson(Json.obj())(request, hc, Fixtures.metadata, Fixtures.schemeType))
      }
    }
  }

  "calling createRootJson" should {

    val adrSubmission: ADRSubmission = new ADRSubmission {

      override val presubmissionService: PresubmissionService = mock[PresubmissionService]

      override val submissionCommon: SubmissionCommon = mock[SubmissionCommon]

      override val configUtils: ConfigUtils = mockConfigUtils

      override def buildRoot(configData: Config, metadata: Object, sheetsJson: JsObject)(implicit request: Request[_], hc: HeaderCarrier, ersSummary: ErsSummary, schemeType: String): JsObject = nilReturnJson

    }

    "return the result of buildRoot" in {

      val result = await(adrSubmission.createRootJson(Json.obj())(request, hc, Fixtures.metadataNilReturn, Fixtures.schemeType))
      result shouldBe nilReturnJson
    }

  }

}
