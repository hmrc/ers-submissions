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

package uk.gov.hmrc.resubmission

import _root_.play.api.Application
import _root_.play.api.inject.guice.GuiceApplicationBuilder
import _root_.play.api.libs.json._
import _root_.play.api.test.Helpers._
import models.{ERSError, ErsSummary, SchemeData}
import org.mongodb.scala.model.Filters
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import uk.gov.hmrc.{FakeErsStubService, Fixtures}

class ResubJobNoFiltersEnabledSpec extends AnyWordSpecLike
  with Matchers
  with GuiceOneServerPerSuite
  with FakeErsStubService {

  val applicationConfig: Map[String, Any] = Map(
    "microservice.services.ers-stub.port" -> "19339",
    "schedules.resubmission-service.enabled" -> true,
    "schedules.resubmission-service.dateTimeFilter.enabled" -> false,
    "schedules.resubmission-service.schemaRefsFilter.enabled" -> false,
    "schedules.resubmission-service.schemaFilter.enabled" -> false,
    "schedules.resubmission-service.resubmissionLimit" -> 2,
    "schedules.resubmission-service.resubmit-list-statuses" -> "failed",
    "schedules.resubmission-service.resubmit-fail-status" -> "failedResubmission",
    "schedules.resubmission-service.resubmit-successful-status" -> "successResubmit",
    "auditing.enabled" -> false,
    "settings.presubmission-collection-index-replace" -> false,
    "schedules.resubmission-service.additional-logs.enabled" -> true
  )

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      conf = applicationConfig
    )
    .build()

  "ResubPresubmissionServiceJob" should {
    "resubmit failed jobs in batches with the correct transfer status and schema type" in new ResubmissionJobSetUp(app = app) {

      val ersSummaries: Seq[ErsSummary] = Fixtures.generateListOfErsSummaries()
      val schemeData: Seq[SchemeData] = Fixtures.generatePresubmissionRecordsForMetadata(ersSummaries)
      val storeDocs: Boolean = await(storeMultipleErsSummary(ersSummaries.map(Json.toJsObject(_))))
      val storePresubmissionDocs: Boolean = await(storeMultiplePresubmissionData(schemeData.map(Json.toJsObject(_))))
      storeDocs shouldBe true
      storePresubmissionDocs shouldBe true

      countMetadataRecordsWithSelector(Filters.empty()) shouldBe 15
      countMetadataRecordsWithSelector(successResubmitTransferStatusSelector) shouldBe 0
      countMetadataRecordsWithSelector(failedJobSelector) shouldBe 10
      val firstJobRunOutcome: Either[ERSError, Boolean] = await(getJob.scheduledMessage.service.invoke.value)
      firstJobRunOutcome shouldBe Right(true)

      countMetadataRecordsWithSelector(Filters.empty()) shouldBe 15
      countMetadataRecordsWithSelector(successResubmitTransferStatusSelector) shouldBe 2
      countMetadataRecordsWithSelector(failedJobSelector) shouldBe 8

      val secondJobRunOutcome: Either[ERSError, Boolean] = await(getJob.scheduledMessage.service.invoke.value)
      secondJobRunOutcome shouldBe Right(true)

      countMetadataRecordsWithSelector(Filters.empty()) shouldBe 15
      countMetadataRecordsWithSelector(successResubmitTransferStatusSelector) shouldBe 4
      countMetadataRecordsWithSelector(failedJobSelector) shouldBe 6

      val ersSummariesSecondStore: Seq[ErsSummary] = Fixtures.failedJobsWithDifferentBundleRef
      val secondStoreDocs: Boolean = await(storeMultipleErsSummary(ersSummariesSecondStore.map(Json.toJsObject(_))))
      secondStoreDocs shouldBe true

      val presubmissionDocsSecondStore: Boolean = await(
        storeMultiplePresubmissionData(Fixtures.generatePresubmissionRecordsForMetadata(ersSummariesSecondStore).map(Json.toJsObject(_))))
      presubmissionDocsSecondStore shouldBe true

      countMetadataRecordsWithSelector(Filters.empty()) shouldBe 25
      countMetadataRecordsWithSelector(successResubmitTransferStatusSelector) shouldBe 4
      countMetadataRecordsWithSelector(failedJobSelector) shouldBe 16
      val thirdJobRunOutcome: Either[ERSError, Boolean] = await(getJob.scheduledMessage.service.invoke.value)
      thirdJobRunOutcome shouldBe Right(true)

      countMetadataRecordsWithSelector(Filters.empty()) shouldBe 25
      countMetadataRecordsWithSelector(successResubmitTransferStatusSelector) shouldBe 6
      countMetadataRecordsWithSelector(failedJobSelector) shouldBe 14
    }
  }
}
