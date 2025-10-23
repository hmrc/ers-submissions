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

import com.mongodb.client.result.DeleteResult
import common.ERSEnvelope
import fixtures.Fixtures
import helpers.ERSTestHelper
import models._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.EitherValues
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import repositories.{PresubmissionMongoRepository, Repositories}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

class PresubmissionServiceSpec extends ERSTestHelper with EitherValues {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[JsObject] = FakeRequest().withBody(Fixtures.metadataJson)
  val mockRepositories: Repositories = mock[Repositories]
  val mockPresubmissionRepository: PresubmissionMongoRepository = mock[PresubmissionMongoRepository]
  val validGetJsonResult: Seq[JsObject] = Seq(Json.toJsObject(Fixtures.schemeData))
  val invalidGetJsonResult: Seq[JsObject] = Seq(Json.toJsObject(Fixtures.schemeData) - "sheetName")
  val deleteResultAcknowledged0: DeleteResult = DeleteResult.acknowledged(0)
  val deleteResultAcknowledged1: DeleteResult = DeleteResult.acknowledged(1)
  val deleteResultUnacknowledged: DeleteResult = DeleteResult.unacknowledged()

  def buildPresubmissionService(storeJsonResult: Option[Boolean] = Some(true),
                                getJsonResult: Seq[JsObject] = validGetJsonResult,
                                removeJsonResult: Option[DeleteResult] = Some(deleteResultAcknowledged1))
                               (implicit ec: ExecutionContext): PresubmissionService =
    new PresubmissionService(mockRepositories) {

      override lazy val presubmissionRepository: PresubmissionMongoRepository = mockPresubmissionRepository
      when(mockPresubmissionRepository.storeJson(any[SchemeData], any())).thenReturn(
        ERSEnvelope(storeJsonResult.toRight(MongoGenericError("Mongo operation failed"))))
      when(mockPresubmissionRepository.getJson(any[SchemeInfo], any()))
        .thenReturn(ERSEnvelope(getJsonResult))
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

    "return error if error occurred in repository" in {
      val presubmissionService = buildPresubmissionService(None)
      val result = await(presubmissionService.storeJson(Fixtures.schemeData).value)
      result.swap.value shouldBe MongoGenericError("Mongo operation failed")
    }
  }

  "calling getJson" should {
    "return List[SchemeData] if finding succeeds" in {
      val presubmissionService = buildPresubmissionService(Some(true))
      val result = await(presubmissionService.getJson(Fixtures.EMISchemeInfo).value)
      result.value.isEmpty shouldBe false
    }

    "return NoData() if finding fails" in {
      val presubmissionService = buildPresubmissionService(Some(true), getJsonResult = Seq())
      val result = await(presubmissionService.getJson(Fixtures.EMISchemeInfo).value)
      result.swap.value shouldBe NoData()
    }

    "return SchemeDataMappingError if mapping to SchemeData fails" in {
      val presubmissionService = buildPresubmissionService(Some(true), getJsonResult = invalidGetJsonResult)
      val result  = await(presubmissionService.getJson(Fixtures.EMISchemeInfo).value)

      result.swap.value shouldBe a[SchemeDataMappingError]
    }
  }

  "calling removeJson" should {
    "return true if removing is sussessful" in {
      val presubmissionService = buildPresubmissionService(Some(true), removeJsonResult = Some(deleteResultAcknowledged1))
      val result = await(presubmissionService.removeJson(Fixtures.EMISchemeInfo).value)
      result.value shouldBe true
    }

    "return false if remove returns false" in {
      val presubmissionService = buildPresubmissionService(Some(true), removeJsonResult = Some(deleteResultUnacknowledged))
      val result = await(presubmissionService.removeJson(Fixtures.EMISchemeInfo).value)
      result.value shouldBe false
    }

    "return NoData if remove returns true and deleted count 0" in {
      val presubmissionService = buildPresubmissionService(Some(true), removeJsonResult = Some(deleteResultAcknowledged0))
      val result = await(presubmissionService.removeJson(Fixtures.EMISchemeInfo).value)
      result.swap.value shouldBe NoData()
    }

    "return error if error returned from repository" in {
      val presubmissionService = buildPresubmissionService(Some(true), removeJsonResult = None)
      val result = await(presubmissionService.removeJson(Fixtures.EMISchemeInfo).value)
      result.swap.value shouldBe MongoGenericError("Mongo operation failed")
    }
  }

  "calling compareSheetsNumber" should {
    def buildPresubmissionService(foundSheets: Option[Int]): PresubmissionService = new PresubmissionService(mockRepositories) {
      override lazy val presubmissionRepository: PresubmissionMongoRepository = mockPresubmissionRepository
      when(mockPresubmissionRepository.count(any[SchemeInfo](), any()))
        .thenReturn(ERSEnvelope(foundSheets.map(_.toLong).toRight(MongoGenericError("Mongo operation failed"))))
    }

    "return true and number of found records if expected number of sheets is equal to found ones" in {
      val presubmissionService = buildPresubmissionService(Some(1))
      val result = await(presubmissionService.compareSheetsNumber(expectedSheets = 1, schemeInfo = Fixtures.EMISchemeInfo).value)
      result.value shouldBe (true, 1)
    }

    "return false and number of found records  if expected number of sheets is not equal to found ones" in {
      val presubmissionService = buildPresubmissionService(Some(1))
      val result = await(presubmissionService.compareSheetsNumber(expectedSheets = 2, schemeInfo = Fixtures.EMISchemeInfo).value)
      result.value shouldBe (false, 1)
    }

    "return false if error is returned from repository" in {
      val presubmissionService = buildPresubmissionService(None)
      val result = await(presubmissionService.compareSheetsNumber(expectedSheets = 1, schemeInfo = Fixtures.EMISchemeInfo).value)
      result.swap.value shouldBe MongoGenericError("Mongo operation failed")
    }
  }
}
