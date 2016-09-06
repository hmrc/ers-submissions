/*
 * Copyright 2016 HM Revenue & Customs
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
import models.{ErsJsonStoreInfo, SchemeInfo}
import org.joda.time.{DateTimeZone, DateTime}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import repositories.{MetadataRepository, PresubmissionRepository, JsonStoreInfoRepository}
import uk.gov.hmrc.play.test.UnitSpec
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class MonitoringServiceSpec extends UnitSpec with MockitoSugar {

  "jsonStoreInfoState" should {
    val mockJsonStoreInfoRepository: JsonStoreInfoRepository = mock[JsonStoreInfoRepository]

    val monitoringService: MonitoringService = new MonitoringService {
      override val jsonStoreInfoRepository: JsonStoreInfoRepository = mockJsonStoreInfoRepository
      override val presubmissionRepository: PresubmissionRepository = mock[PresubmissionRepository]
      override val metadataRepository: MetadataRepository = mock[MetadataRepository]
    }

    "log sliced a list of schemeRefs of given status" in {
      reset(mockJsonStoreInfoRepository)
      when(
        mockJsonStoreInfoRepository.findJsonStoreInfoByStatus(any[List[String]](), anyInt())
      ).thenReturn(
        Future.successful(List(Fixtures.ersJsonStoreInfo))
      )
      val result = await(monitoringService.jsonStoreInfoState("failed"))
      result shouldBe (())
    }

    "log exception if data can't be retrieved" in {
      reset(mockJsonStoreInfoRepository)
      when(
        mockJsonStoreInfoRepository.findJsonStoreInfoByStatus(any[List[String]](), anyInt())
      ).thenReturn(
        Future.failed(new RuntimeException)
      )
      val result = await(monitoringService.jsonStoreInfoState("failed"))
      result shouldBe (())
    }
  }

  "missingPresubmissionData" should {
    val mockPresubmissionRepository: PresubmissionRepository = mock[PresubmissionRepository]
    val mockMetadataRepository: MetadataRepository = mock[MetadataRepository]

    val monitoringService: MonitoringService = new MonitoringService {
      override val jsonStoreInfoRepository: JsonStoreInfoRepository = mock[JsonStoreInfoRepository]
      override val presubmissionRepository: PresubmissionRepository = mockPresubmissionRepository
      override val metadataRepository: MetadataRepository = mockMetadataRepository
    }

    "log sliced a list of schemeRefs that doesn't exists in presubmission collection but present in metadata one" in {
      reset(mockPresubmissionRepository)
      reset(mockMetadataRepository)
      when(
        mockPresubmissionRepository.getSchemeRefs(any[DateTime](), any[DateTime]())
      ).thenReturn(
        Future.successful(List("schemeRef"))
      )
      when(
        mockMetadataRepository.getSchemeRefs(any[DateTime](), any[DateTime](), any[List[String]]())
      ).thenReturn(
        Future.successful(List("schemeRef"))
      )
      val result = await(monitoringService.missingPresubmissionData(DateTime.now, DateTime.now))
      result shouldBe (())
    }

    "log exception if metadata throws one" in {
      reset(mockPresubmissionRepository)
      reset(mockMetadataRepository)
      when(
        mockPresubmissionRepository.getSchemeRefs(any[DateTime](), any[DateTime]())
      ).thenReturn(
        Future.successful(List("schemeRef"))
      )
      when(
        mockMetadataRepository.getSchemeRefs(any[DateTime](), any[DateTime](), any[List[String]]())
      ).thenReturn(
        Future.failed(new RuntimeException)
      )
      val result = await(monitoringService.missingPresubmissionData(DateTime.now, DateTime.now))
      result shouldBe (())
    }

    "log exception if presubmission throws one" in {
      reset(mockPresubmissionRepository)
      reset(mockMetadataRepository)
      when(
        mockPresubmissionRepository.getSchemeRefs(any[DateTime](), any[DateTime]())
      ).thenReturn(
        Future.failed(new RuntimeException)
      )
      when(
        mockMetadataRepository.getSchemeRefs(any[DateTime](), any[DateTime](), any[List[String]]())
      ).thenReturn(
        Future.successful(List("schemeRef"))
      )
      val result = await(monitoringService.missingPresubmissionData(DateTime.now, DateTime.now))
      result shouldBe (())
    }
  }

  "syncMetadataAndJsonStoreInfo" should {
    val mockJsonStoreInfoRepository: JsonStoreInfoRepository = mock[JsonStoreInfoRepository]
    val mockMetadataRepository: MetadataRepository = mock[MetadataRepository]

    val monitoringService: MonitoringService = new MonitoringService {
      override val jsonStoreInfoRepository: JsonStoreInfoRepository = mockJsonStoreInfoRepository
      override val presubmissionRepository: PresubmissionRepository = mock[PresubmissionRepository]
      override val metadataRepository: MetadataRepository = mockMetadataRepository
    }

    "log successfully missing JsonStoreInfo records" in {
      reset(mockJsonStoreInfoRepository)
      reset(mockMetadataRepository)
      when(
        mockJsonStoreInfoRepository.getSchemeInfoForPeriod(any[DateTime](), any[DateTime]())
      ).thenReturn(
        Future.successful(List(Fixtures.schemeInfo))
      )
      when(
        mockMetadataRepository.getSchemeInfo(any[DateTime](), any[DateTime](), any[List[SchemeInfo]]())
      ).thenReturn(
        Future.successful(List(Fixtures.schemeInfo))
      )

      val result = await(monitoringService.syncMetadataAndJsonStoreInfo(DateTime.now, DateTime.now))
      result shouldBe (())
    }

    "log exception if metadata repository throws one" in {
      reset(mockJsonStoreInfoRepository)
      reset(mockMetadataRepository)
      when(
        mockJsonStoreInfoRepository.getSchemeInfoForPeriod(any[DateTime](), any[DateTime]())
      ).thenReturn(
        Future.successful(List(Fixtures.schemeInfo))
      )
      when(
        mockMetadataRepository.getSchemeInfo(any[DateTime](), any[DateTime](), any[List[SchemeInfo]]())
      ).thenReturn(
        Future.failed(new RuntimeException)
      )

      val result = await(monitoringService.syncMetadataAndJsonStoreInfo(DateTime.now, DateTime.now))
      result shouldBe (())
    }

    "log exception if jsonStoreInfo repository throws one" in {
      reset(mockJsonStoreInfoRepository)
      reset(mockMetadataRepository)
      when(
        mockJsonStoreInfoRepository.getSchemeInfoForPeriod(any[DateTime](), any[DateTime]())
      ).thenReturn(
        Future.failed(new RuntimeException)
      )
      when(
        mockMetadataRepository.getSchemeInfo(any[DateTime](), any[DateTime](), any[List[SchemeInfo]]())
      ).thenReturn(
        Future.successful(List(Fixtures.schemeInfo))
      )

      val result = await(monitoringService.syncMetadataAndJsonStoreInfo(DateTime.now, DateTime.now))
      result shouldBe (())
    }
  }

  "syncMetadataAndJsonStoreInfoBySchemeRef" should {
    val mockJsonStoreInfoRepository: JsonStoreInfoRepository = mock[JsonStoreInfoRepository]
    val mockMetadataRepository: MetadataRepository = mock[MetadataRepository]

    val monitoringService: MonitoringService = new MonitoringService {
      override val jsonStoreInfoRepository: JsonStoreInfoRepository = mockJsonStoreInfoRepository
      override val presubmissionRepository: PresubmissionRepository = mock[PresubmissionRepository]
      override val metadataRepository: MetadataRepository = mockMetadataRepository
    }
    val newSchemeInfo = SchemeInfo (
      schemeRef = "XA1100000000005",
      timestamp = new DateTime().withDate(2016,12,5).withTime(12,50,55,0).withZone(DateTimeZone.UTC),
      schemeId = "123PA12345678",
      taxYear = "2014/15",
      schemeName = "My scheme",
      schemeType = "EMI"
    )

    "add successfully missing records in JsonStoreInfoRepository" in {
      reset(mockJsonStoreInfoRepository)
      reset(mockMetadataRepository)
      when(
        mockJsonStoreInfoRepository.getSchemeInfoBySchemeRefs(any[List[String]]())
      ).thenReturn(
        Future.successful(List(Fixtures.schemeInfo))
      )
      when(
        mockMetadataRepository.getSchemeInfoBySchemeRefs(any[List[String]]())
      ).thenReturn(
        Future.successful(List(Fixtures.schemeInfo, newSchemeInfo))
      )
      when(
        mockJsonStoreInfoRepository.createErsJsonStoreInfo(any[ErsJsonStoreInfo]())
      ).thenReturn(
        Future.successful(true)
      )

      val result = await(monitoringService.syncMetadataAndJsonStoreInfoBySchemeRef(List(Fixtures.schemeInfo.schemeRef, "XA1100000000005")))
      result shouldBe List(())
    }

    "log exception if can't add missing record" in {
      reset(mockJsonStoreInfoRepository)
      reset(mockMetadataRepository)
      when(
        mockJsonStoreInfoRepository.getSchemeInfoBySchemeRefs(any[List[String]]())
      ).thenReturn(
        Future.successful(List(Fixtures.schemeInfo))
      )
      when(
        mockMetadataRepository.getSchemeInfoBySchemeRefs(any[List[String]]())
      ).thenReturn(
        Future.successful(List(Fixtures.schemeInfo, newSchemeInfo))
      )
      when(
        mockJsonStoreInfoRepository.createErsJsonStoreInfo(any[ErsJsonStoreInfo]())
      ).thenReturn(
        Future.failed(new RuntimeException)
      )

      val result = await(monitoringService.syncMetadataAndJsonStoreInfoBySchemeRef(List(Fixtures.schemeInfo.schemeRef, "XA1100000000005")))
      result shouldBe List(())
    }

    "log exception if can't access records in MetadataRepository" in {
      reset(mockJsonStoreInfoRepository)
      reset(mockMetadataRepository)
      when(
        mockJsonStoreInfoRepository.getSchemeInfoBySchemeRefs(any[List[String]]())
      ).thenReturn(
        Future.successful(List(Fixtures.schemeInfo))
      )
      when(
        mockMetadataRepository.getSchemeInfoBySchemeRefs(any[List[String]]())
      ).thenReturn(
        Future.failed(new RuntimeException)
      )
      when(
        mockJsonStoreInfoRepository.createErsJsonStoreInfo(any[ErsJsonStoreInfo]())
      ).thenReturn(
        Future.successful(true)
      )

      val result = await(monitoringService.syncMetadataAndJsonStoreInfoBySchemeRef(List(Fixtures.schemeInfo.schemeRef, "XA1100000000005")))
      result shouldBe List(())
    }

    "log exception if can't access records in JsonStoreInfoRepository" in {
      reset(mockJsonStoreInfoRepository)
      reset(mockMetadataRepository)
      when(
        mockJsonStoreInfoRepository.getSchemeInfoBySchemeRefs(any[List[String]]())
      ).thenReturn(
        Future.failed(new RuntimeException)
      )
      when(
        mockMetadataRepository.getSchemeInfoBySchemeRefs(any[List[String]]())
      ).thenReturn(
        Future.successful(List(Fixtures.schemeInfo, newSchemeInfo))
      )
      when(
        mockJsonStoreInfoRepository.createErsJsonStoreInfo(any[ErsJsonStoreInfo]())
      ).thenReturn(
        Future.successful(true)
      )

      val result = await(monitoringService.syncMetadataAndJsonStoreInfoBySchemeRef(List(Fixtures.schemeInfo.schemeRef, "XA1100000000005")))
      result shouldBe List(())
    }
  }

}
