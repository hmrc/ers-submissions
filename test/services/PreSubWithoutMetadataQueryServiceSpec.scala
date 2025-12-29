/*
 * Copyright 2025 HM Revenue & Customs
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

import config.ApplicationConfig
import models.PreSubWithoutMetadata
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import repositories.PreSubWithoutMetadataQuery
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class PreSubWithoutMetadataQueryServiceSpec
  extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with BeforeAndAfterEach {

  val mockApplicationConfig: ApplicationConfig = mock[ApplicationConfig]
  val mockPreSubQuery: PreSubWithoutMetadataQuery = mock[PreSubWithoutMetadataQuery]

  private val service = spy(new PresSubWithoutMetadataQueryService(
    mockApplicationConfig,
    mockPreSubQuery
  ))

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(service)
    reset(mockApplicationConfig)
    reset(mockPreSubQuery)
  }

  private val className = "PresSubWithoutMetadataQueryService"

  "PresSubWithoutMetadataQueryService" should {

    val maxRecords = 100
    val testRecords = List(
      PreSubWithoutMetadata("scheme1", "2023/24", 1672531200000L),
      PreSubWithoutMetadata("scheme2", "2023/24", 1672617600000L)
    )

    "log individual records when validQueryRecords is less than maxNumberOfRecordsToReturn" in {

      when(mockApplicationConfig.maxNumberOfRecordsToReturn).thenReturn(maxRecords)
      when(mockPreSubQuery.runQuery).thenReturn(
        Future.successful((List.empty[String], testRecords))
      )

      Await.result(service.invoke.value, Duration.Inf)

      val expectedInfoLog =
        s"""[$className][logPresubmissionRecordsWithoutMetadata] Presubmission data without metadata:
           |schemeRef: scheme1, taxYear: 2023/24, timestamp: 2023-01-01 00:00:00
           |schemeRef: scheme2, taxYear: 2023/24, timestamp: 2023-01-02 00:00:00
           |""".stripMargin

      verify(service).logInfo(expectedInfoLog)
    }

    "log only record count when validQueryRecords is greater than maxNumberOfRecordsToReturn" in {
      val maxRecords = 1

      when(mockApplicationConfig.maxNumberOfRecordsToReturn).thenReturn(maxRecords)
      when(mockPreSubQuery.runQuery).thenReturn(
        Future.successful((List.empty[String], testRecords))
      )

      Await.result(service.invoke.value, Duration.Inf)

      val expectedInfoLog = s"[$className][invoke] Number of records > 1, 2 records returned from query"

      verify(service).logInfo(expectedInfoLog)
    }

    "log both validation errors and individual record details when validation errors are present" in {
      val someValidationError = "bonk"

      when(mockApplicationConfig.maxNumberOfRecordsToReturn).thenReturn(maxRecords)
      when(mockPreSubQuery.runQuery).thenReturn(
        Future.successful((List(someValidationError), testRecords))
      )

      Await.result(service.invoke.value, Duration.Inf)

      val expectedInfoLog = s"[$className][invoke] 1 validation errors, showing first 10: $someValidationError"

      val expectedInfoLogRecords =
        s"""[$className][logPresubmissionRecordsWithoutMetadata] Presubmission data without metadata:
           |schemeRef: scheme1, taxYear: 2023/24, timestamp: 2023-01-01 00:00:00
           |schemeRef: scheme2, taxYear: 2023/24, timestamp: 2023-01-02 00:00:00
           |""".stripMargin

      verify(service).logInfo(expectedInfoLog)
      verify(service).logInfo(expectedInfoLogRecords)
    }

    "log error message when runQuery fails with an exception" in {
      val someValidationError = "kablam"

      when(mockApplicationConfig.maxNumberOfRecordsToReturn).thenReturn(maxRecords)
      when(mockPreSubQuery.runQuery).thenReturn(Future.failed(new Exception(someValidationError)))

      Await.result(service.invoke.value, Duration.Inf)

      val expectedErrorLog = s"[$className][invoke] Failed to fetch or log records: $someValidationError"

      verify(service).logError(expectedErrorLog)
    }

  }
}
