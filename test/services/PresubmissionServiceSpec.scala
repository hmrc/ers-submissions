/*
 * Copyright 2019 HM Revenue & Customs
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

import models.{SchemeInfo, SchemeData}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, FakeApplication}
import org.scalatest.mockito.MockitoSugar
import repositories.PresubmissionMongoRepository
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}
import fixtures.Fixtures
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier

class PresubmissionServiceSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  implicit val hc: HeaderCarrier = new HeaderCarrier()
  implicit val request = FakeRequest().withBody(Fixtures.metadataJson)

  def buildPresubmissionService(storeJsonResult: Option[Boolean] = Some(true), getJsonResult: Boolean = true, removeJsonResult: Option[Boolean] = Some(true)): PresubmissionService = new PresubmissionService {

    val mockPresubmissionRepository: PresubmissionMongoRepository = mock[PresubmissionMongoRepository]
    when(
      mockPresubmissionRepository.storeJson(any[SchemeData])
    ).thenReturn(
      if(storeJsonResult.isDefined) {
        Future(storeJsonResult.get)
      }
      else {
        Future.failed(new RuntimeException)
      }
    )
    when(mockPresubmissionRepository.getJson(any[SchemeInfo])).thenReturn(Future(getJsonResult match {
      case true => List(Fixtures.schemeData)
      case _ => List()
    }))
    when(
      mockPresubmissionRepository.removeJson(any[SchemeInfo])
    ).thenReturn(
      if(removeJsonResult.isDefined) {
        Future(removeJsonResult.get)
      }
      else {
        Future.failed(new RuntimeException)
      }
    )

    override lazy val presubmissionRepository = mockPresubmissionRepository
    override val ersLoggingAndAuditing: ErsLoggingAndAuditing = mock[ErsLoggingAndAuditing]
  }

  "calling storeJson" should {

    "return true if storage is sussessful" in {
      val presubmissionService = buildPresubmissionService(Some(true), true)
      val result = await(presubmissionService.storeJson(Fixtures.schemeData))
      result shouldBe true
    }

    "return false if storage fails" in {
      val presubmissionService = buildPresubmissionService(Some(false), true)
      val result = await(presubmissionService.storeJson(Fixtures.schemeData))
      result shouldBe false
    }

    "return false if exception" in {
      val presubmissionService = buildPresubmissionService(None, true)
      val result = await(presubmissionService.storeJson(Fixtures.schemeData))
      result shouldBe false
    }

  }

  "calling getJson" should {

    "return List[SchemeData] if finding succeeds" in {
      val presubmissionService = buildPresubmissionService(Some(true), true)
      val result = await(presubmissionService.getJson(Fixtures.EMISchemeInfo))
      result.isEmpty shouldBe false
    }

    "return empty list if finding fails" in {
      val presubmissionService = buildPresubmissionService(Some(true), false)
      val result = await(presubmissionService.getJson(Fixtures.EMISchemeInfo))
      result.isEmpty shouldBe true
    }
  }

  "calling removeJson" should {

    "return true if removing is sussessful" in {
      val presubmissionService = buildPresubmissionService(Some(true), true, Some(true))
      val result = await(presubmissionService.removeJson(Fixtures.EMISchemeInfo))
      result shouldBe true
    }

    "return false if removing fails" in {
      val presubmissionService = buildPresubmissionService(Some(true), true, Some(false))
      val result = await(presubmissionService.removeJson(Fixtures.EMISchemeInfo))
      result shouldBe false
    }

    "return false if exception" in {
      val presubmissionService = buildPresubmissionService(Some(true), true, None)
      val result = await(presubmissionService.removeJson(Fixtures.EMISchemeInfo))
      result shouldBe false
    }

  }

  "calling compareSheetsNumber" should {

    def buildPresubmissionService(foundSheets: Option[Int]): PresubmissionService = new PresubmissionService {

      val mockPresubmissionRepository: PresubmissionMongoRepository = mock[PresubmissionMongoRepository]

      when(
        mockPresubmissionRepository.count(any[SchemeInfo]())
      ).thenReturn(
        if(foundSheets.isDefined) {
          Future.successful(foundSheets.get)
        }
        else {
          Future.failed(new RuntimeException)
        }
      )

      override lazy val presubmissionRepository = mockPresubmissionRepository
      override val ersLoggingAndAuditing: ErsLoggingAndAuditing = mock[ErsLoggingAndAuditing]
    }

    "return true if expected number of sheets is equal to found ones" in {
      val presubmissionService = buildPresubmissionService(Some(1))
      val result = await(presubmissionService.compareSheetsNumber(1, Fixtures.EMISchemeInfo))
      result shouldBe true
    }

    "return false if expected number of sheets is not equal to found ones" in {
      val presubmissionService = buildPresubmissionService(Some(-1))
      val result = await(presubmissionService.compareSheetsNumber(1, Fixtures.EMISchemeInfo))
      result shouldBe false
    }

    "return false if exception is thrown" in {
      val presubmissionService = buildPresubmissionService(None)
      val result = await(presubmissionService.compareSheetsNumber(1, Fixtures.EMISchemeInfo))
      result shouldBe false
    }

  }

  "calling findAndUpdate" should {

    val  presubmissionService: PresubmissionService = new PresubmissionService {

      val mockPresubmissionRepository: PresubmissionMongoRepository = mock[PresubmissionMongoRepository]

      when(
        mockPresubmissionRepository.findAndUpdate(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(Some(Fixtures.schemeData))
      )

      override lazy val presubmissionRepository = mockPresubmissionRepository
      override val ersLoggingAndAuditing: ErsLoggingAndAuditing = mock[ErsLoggingAndAuditing]
    }

    "return the result of repository findAndUpdate" in {
      val result = await(presubmissionService.findAndUpdate(Fixtures.EMISchemeInfo))
      result.get shouldBe Fixtures.schemeData
    }

  }

}
