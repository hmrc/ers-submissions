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

package utils

import com.typesafe.config.Config
import common.ERSEnvelope
import common.ERSEnvelope.ERSEnvelope
import fixtures.Fixtures
import helpers.ERSTestHelper
import models.{ErsSummary, MongoGenericError, SchemeInfo}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import services.PresubmissionService
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggingAndRexceptions.ADRExceptionEmitter

import scala.collection.mutable.ListBuffer

class ADRSubmissionSpec extends ERSTestHelper with BeforeAndAfterEach with EitherValues {

  implicit val hc: HeaderCarrier = new HeaderCarrier()
  implicit val request = FakeRequest().withBody(Fixtures.metadataJson)
  val mockSubmissionCommon: SubmissionCommon = mock[SubmissionCommon]
  val mockPresubmissionService: PresubmissionService = mock[PresubmissionService]
  val mockAdrExceptionEmitter: ADRExceptionEmitter = app.injector.instanceOf[ADRExceptionEmitter]
  val mockConfig: Config = mock[Config]
  when(mockConfig.getConfig(anyString())).thenReturn(mock[Config])

  val mockConfigUtils: ConfigUtils = mock[ConfigUtils]
  when(mockConfigUtils.getConfigData(anyString(), anyString(), any[ErsSummary]())(any[HeaderCarrier]())).thenReturn(mockConfig)

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
      mockConfigUtils
    ) {

      override def createSubmissionJson(ersSummary: ErsSummary, schemeType: String)
                                       (implicit request: Request[_], hc: HeaderCarrier): ERSEnvelope[JsObject] = ERSEnvelope(notNilReturnJson)

      override def createRootJson(sheetsJson: JsObject, ersSummary: ErsSummary, schemeType: String)
                                 (implicit request: Request[_], hc: HeaderCarrier): JsObject = nilReturnJson
    }

    "return createSubmissionJson result for not NilReturn submission" in {
      val result = await(mockAdrSubmission.generateSubmission(Fixtures.metadata)(request, hc).value).value
      result shouldBe notNilReturnJson
    }

    "return createRootJson result for NilReturn submission" in {
      val result = await(mockAdrSubmission.generateSubmission(Fixtures.metadataNilReturn)(request, hc).value).value
      result shouldBe nilReturnJson
    }
  }

  "calling createSubmissionJson" should {
    val mockAdrSubmission: ADRSubmission = new ADRSubmission(
      mockSubmissionCommon,
      mockPresubmissionService,
      mockConfigUtils
    ) {
      override def createSheetsJson(sheetsJson: JsObject, ersSummary: ErsSummary, schemeType: String)
                                   (implicit request: Request[_], hc: HeaderCarrier): ERSEnvelope[JsObject] =
        ERSEnvelope(sheetsJson)

      override def createRootJson(sheetsJson: JsObject, ersSummary: ErsSummary, schemeType: String)
                                 (implicit request: Request[_], hc: HeaderCarrier): JsObject =
        notNilReturnJson
    }

    "return the result of createRootJson" in {
      val result = await(mockAdrSubmission.createSubmissionJson(Fixtures.metadata, Fixtures.schemeType)(request, hc).value).value
      result shouldBe notNilReturnJson
    }
  }

  "calling createSheetsJson" should {
    val mockAdrSubmission: ADRSubmission = new ADRSubmission(
      mockSubmissionCommon,
      mockPresubmissionService,
      mockConfigUtils
    ) {
      when(mockSubmissionCommon.mergeSheetData(any[Config](), any[JsObject], any[JsObject])).thenReturn(sheetsJson)

      override def buildJson(configData: Config, fileData: ListBuffer[Seq[String]], row: Option[Int] = None, sheetName: Option[String], schemeInfo: Option[SchemeInfo])
                            (implicit request: Request[_], hc: HeaderCarrier): JsObject =
        sheetsJson
    }

    "return given json as parameter if there is no data in the database" in {
      when(mockPresubmissionService.getJson(any[SchemeInfo]())(any())).thenReturn(ERSEnvelope(List()))

      val result = await(mockAdrSubmission.createSheetsJson(sheetsJson, Fixtures.metadata, Fixtures.schemeType)(request, hc).value).value
      result shouldBe sheetsJson
    }

    "return merged data json if there is data in the database" in {
      when(mockPresubmissionService.getJson(any[SchemeInfo]())(any()))
        .thenReturn(ERSEnvelope(scala.Seq(Fixtures.schemeData)))

      val result = await(mockAdrSubmission.createSheetsJson(Json.obj(), Fixtures.metadata, Fixtures.schemeType)(request, hc).value).value
      result shouldBe sheetsJson
    }

    "return MongoGenericError if retrieving data returns error" in {
      when(mockPresubmissionService.getJson(any[SchemeInfo]())(any()))
        .thenReturn(ERSEnvelope(MongoGenericError("There was a problem")))

      val result = await(mockAdrSubmission.createSheetsJson(Json.obj(), Fixtures.metadata, Fixtures.schemeType)(request, hc).value)
      result.swap.value shouldBe MongoGenericError("There was a problem")
    }

    "calling createRootJson" should {
      val mockAdrSubmission: ADRSubmission = new ADRSubmission(
        mockSubmissionCommon,
        mockPresubmissionService,
        mockConfigUtils
      ) {
        override def buildRoot(configData: Config, metadata: Object, sheetsJson: JsObject, ersSummary: ErsSummary, schemeType: String)
                              (implicit request: Request[_], hc: HeaderCarrier): JsObject = nilReturnJson
      }
      "return the result of buildRoot" in {
        val result = mockAdrSubmission.createRootJson(Json.obj(), Fixtures.metadataNilReturn, Fixtures.schemeType)(request, hc)
        result shouldBe nilReturnJson
      }
    }
  }
}
