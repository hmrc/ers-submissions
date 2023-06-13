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

import common.ERSEnvelope
import fixtures.Fixtures
import helpers.ERSTestHelper
import models.{MongoGenericError, SchemeData, SchemeDataMappingError, SchemeInfo}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.EitherValues
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import repositories.{PresubmissionMongoRepository, Repositories}
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing

import scala.concurrent.ExecutionContext

class PresubmissionServiceSpec extends ERSTestHelper with EitherValues {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[JsObject] = FakeRequest().withBody(Fixtures.metadataJson)
  val mockRepositories: Repositories = mock[Repositories]
  val mockErsLoggingAndAuditing: ErsLoggingAndAuditing = mock[ErsLoggingAndAuditing]
  val mockPresubmissionRepository: PresubmissionMongoRepository = mock[PresubmissionMongoRepository]

  def buildPresubmissionService(storeJsonResult: Option[Boolean] = Some(true),
                                getJsonResult: Boolean = true,
                                removeJsonResult: Option[Boolean] = Some(true),
                                getJsonResultFailedMapping: Boolean = false)
                               (implicit ec: ExecutionContext): PresubmissionService =
    new PresubmissionService(mockRepositories, mockErsLoggingAndAuditing) {

      override lazy val presubmissionRepository: PresubmissionMongoRepository = mockPresubmissionRepository
      when(mockPresubmissionRepository.storeJson(any[SchemeData], any())).thenReturn(
        ERSEnvelope(storeJsonResult.toRight(MongoGenericError("Mongo operation failed"))))
      when(mockPresubmissionRepository.getJson(any[SchemeInfo], any()))
        .thenReturn(ERSEnvelope((getJsonResult, getJsonResultFailedMapping) match {
            case (true, _) => Seq(Json.toJsObject(Fixtures.schemeData))
            case (_, true) => Seq(Json.toJsObject(Fixtures.schemeData) - "sheetName")
            case _ => Seq()
          }))
      when(mockPresubmissionRepository.removeJson(any[SchemeInfo], any()))
        .thenReturn(ERSEnvelope(removeJsonResult.toRight(MongoGenericError("Mongo operation failed"))))
    }

  "calling storeJson" should {
    "return true if storage is successful" in {
      val presubmissionService = buildPresubmissionService(Some(true))
      val result = await(presubmissionService.storeJson(Fixtures.schemeData).value)
      result.value shouldBe true
    }

    "return false if storage fails" in {
      val presubmissionService = buildPresubmissionService(Some(false))
      val result = await(presubmissionService.storeJson(Fixtures.schemeData).value)
      result.value shouldBe false
    }

    "return false if error occured" in {
      val presubmissionService = buildPresubmissionService(None)
      val result = await(presubmissionService.storeJson(Fixtures.schemeData).value)
      result.value shouldBe false
    }
  }

  "calling getJson" should {
    "return List[SchemeData] if finding succeeds" in {
      val presubmissionService = buildPresubmissionService(Some(true))
      val result = await(presubmissionService.getJson(Fixtures.EMISchemeInfo).value)
      result.value.isEmpty shouldBe false
    }

    "return empty list if finding fails" in {
      val presubmissionService = buildPresubmissionService(Some(true), getJsonResult = false)
      val result = await(presubmissionService.getJson(Fixtures.EMISchemeInfo).value)
      result.value.isEmpty shouldBe true
    }

    "return SchemeDataMappingError if mapping to SchemeData fails" in {
      val presubmissionService = buildPresubmissionService(Some(true), getJsonResult = false, getJsonResultFailedMapping = true)
      val result  = await(presubmissionService.getJson(Fixtures.EMISchemeInfo).value)

      result.swap.value shouldBe a[SchemeDataMappingError]
    }
  }

  "calling removeJson" should {
    "return true if removing is sussessful" in {
      val presubmissionService = buildPresubmissionService(Some(true), getJsonResult = true, Some(true))
      val result = await(presubmissionService.removeJson(Fixtures.EMISchemeInfo).value)
      result.value shouldBe true
    }

    "return false if remove returns false" in {
      val presubmissionService = buildPresubmissionService(Some(true), getJsonResult = true, Some(false))
      val result = await(presubmissionService.removeJson(Fixtures.EMISchemeInfo).value)
      result.value shouldBe false
    }

    "return false if error returned" in {
      val presubmissionService = buildPresubmissionService(Some(true), getJsonResult = true, None)
      val result = await(presubmissionService.removeJson(Fixtures.EMISchemeInfo).value)
      result.value shouldBe false
    }
  }

  "calling compareSheetsNumber" should {

    def buildPresubmissionService(foundSheets: Option[Int]): PresubmissionService = new PresubmissionService(mockRepositories, mockErsLoggingAndAuditing) {
      override lazy val presubmissionRepository: PresubmissionMongoRepository = mockPresubmissionRepository
      when(mockPresubmissionRepository.count(any[SchemeInfo](), any()))
        .thenReturn(ERSEnvelope(foundSheets.map(_.toLong).toRight(MongoGenericError("Mongo operation failed"))))
    }

    "return true if expected number of sheets is equal to found ones" in {
      val presubmissionService = buildPresubmissionService(Some(1))
      val result = await(presubmissionService.compareSheetsNumber(expectedSheets = 1, schemeInfo = Fixtures.EMISchemeInfo).value)
      result.value shouldBe true
    }

    "return false if expected number of sheets is not equal to found ones" in {
      val presubmissionService = buildPresubmissionService(Some(-1))
      val result = await(presubmissionService.compareSheetsNumber(expectedSheets = 1, schemeInfo = Fixtures.EMISchemeInfo).value)
      result.value shouldBe false
    }

    "return false if error is returned" in {
      val presubmissionService = buildPresubmissionService(None)
      val result = await(presubmissionService.compareSheetsNumber(expectedSheets = 1, schemeInfo = Fixtures.EMISchemeInfo).value)
      result.value shouldBe false
    }
  }
}
