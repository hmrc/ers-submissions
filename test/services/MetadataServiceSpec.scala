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

import fixtures.Fixtures
import helpers.ERSTestHelper
import models.ErsSummary
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.test.FakeRequest
import repositories.{MetadataMongoRepository, Repositories}
import utils.LoggingAndRexceptions.ErsLoggingAndAuditing

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class MetadataServiceSpec extends ERSTestHelper with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = new HeaderCarrier()

  val mockRepositories: Repositories = mock[Repositories]
  val mockErsLoggingAndAuditing: ErsLoggingAndAuditing = mock[ErsLoggingAndAuditing]
  val mockMetadataRepository: MetadataMongoRepository = mock[MetadataMongoRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMetadataRepository)
  }
  "calling storeErsSummary" should {

    val metadataService: MetadataService = new MetadataService(mockMetadataRepository, mockErsLoggingAndAuditing) {
      override lazy val metadataRepository: MetadataMongoRepository = mockMetadataRepository
    }

    "return the result of storeErsSummary" in {
      when(mockMetadataRepository.storeErsSummary(any[ErsSummary]())).thenReturn(Future.successful(true))

      val result = await(metadataService.storeErsSummary(Fixtures.metadata)(FakeRequest().withBody(Fixtures.metadataJson), hc))
      result shouldBe true
    }

    "return false if storeErsSummary throws exception" in {
      when(mockMetadataRepository.storeErsSummary(any[ErsSummary]()))
        .thenReturn(Future.failed(new RuntimeException))

      val result = await(metadataService.storeErsSummary(Fixtures.metadata)(FakeRequest().withBody(Fixtures.metadataJson), hc))
      result shouldBe false
    }
  }

  "calling validateErsSummaryFromJson" should {

    def metadataService(validationResult: Boolean): MetadataService = new MetadataService(mockMetadataRepository, mockErsLoggingAndAuditing) {

      override def validateErsSummary(ersSummary: ErsSummary): (Boolean, Option[String]) = {
        (validationResult, Some(""))
      }
    }

    "return object of ErsSummary if json is correct and there are no additional validation errors" in {
      val service = metadataService(true)
      val result = service.validateErsSummaryFromJson(Fixtures.metadataJson)
      result.isDefined shouldBe true
    }

    "return Null if json is correct and but there are additional validation errors" in {
      val service = metadataService(false)
      val result = service.validateErsSummaryFromJson(Fixtures.metadataJson)
      result.isDefined shouldBe false
    }

    "return Null if json is not correct" in {
      val service = metadataService(true)
      val result = service.validateErsSummaryFromJson(Fixtures.invalidJson)
      result.isDefined shouldBe false
    }

  }

  "calling validateErsSummary" should {

    val metadataService: MetadataService = new MetadataService(mockMetadataRepository, mockErsLoggingAndAuditing)

    "return true if given ErsSummary has no errors" in {
      val result = metadataService.validateErsSummary(Fixtures.metadata)
      result._1 shouldBe true
      result._2.isDefined shouldBe false
    }

    "return false if given ErsSummary has invalid value for NilReturn" in {
      val result = metadataService.validateErsSummary(Fixtures.invalidMetadataNilReturn)
      result._1 shouldBe false
      result._2.get shouldBe "isNilReturn"
    }

    "return false if given ErsSummary has missing value for schemeRef" in {
      val result = metadataService.validateErsSummary(Fixtures.invalidMetadataMissingSchemeRef)
      result._1 shouldBe false
      result._2.get shouldBe "schemeRef"
    }

    "return false if given ErsSummary has missing value for schemeType" in {
      val result = metadataService.validateErsSummary(Fixtures.invalidMetadataMissingSchemeType)
      result._1 shouldBe false
      result._2.get shouldBe "schemeType"
    }

  }

}
