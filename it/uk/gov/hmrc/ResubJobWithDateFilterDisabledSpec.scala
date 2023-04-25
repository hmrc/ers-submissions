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

package uk.gov.hmrc

import org.mongodb.scala.model.Filters
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import _root_.play.api.Application
import _root_.play.api.inject.guice.GuiceApplicationBuilder
import _root_.play.api.test.Helpers._
import org.mongodb.scala.bson.BsonDocument

class ResubJobWithDateFilterDisabledSpec extends AnyWordSpecLike
  with Matchers
  with GuiceOneServerPerSuite
  with FakeErsStubService {

  val applicationConfig: Map[String, Any] = Map(
    "microservice.services.ers-stub.port" -> "19339",
    "schedules.resubmission-service.dateTimeFilter.enabled" -> false,
    "schedules.resubmission-service.resubmissionLimit" -> 10
  )

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      conf = applicationConfig
    )
    .build()

  "With dateTimeFiltering not enabled ResubPresubmissionServiceJob" should {

    "resubmit failed jobs with the correct transfer status and schema type" in new ResubmissionJobSetUp(app = app) {
      val failedJobSelector: BsonDocument = createFailedJobSelector(None)
      val storeDocs: Boolean = await(storeMultipleErsSummary(Fixtures.ersSummaries))
      storeDocs shouldBe true

      countMetadataRecordsWithSelector(Filters.empty()) shouldBe 6
      countMetadataRecordsWithSelector(successResubmitTransferStatusSelector) shouldBe 1
      countMetadataRecordsWithSelector(failedJobSelector) shouldBe 3

      val updateCompleted: Boolean = await(getJob.scheduledMessage.service.invoke.map(_.asInstanceOf[Boolean]))
      updateCompleted shouldBe true

      countMetadataRecordsWithSelector(Filters.empty()) shouldBe 6
      countMetadataRecordsWithSelector(successResubmitTransferStatusSelector) shouldBe 4
      countMetadataRecordsWithSelector(failedJobSelector) shouldBe 0
    }

    "resubmit failed jobs in batches with the correct transfer status and schema type" in new ResubmissionJobSetUp(app = app) {
      val failedJobSelector: BsonDocument = createFailedJobSelector(None)
      val storeDocs: Boolean = await(storeMultipleErsSummary(Fixtures.generateListOfErsSummaries()))
      storeDocs shouldBe true

      countMetadataRecordsWithSelector(Filters.empty()) shouldBe 40
      countMetadataRecordsWithSelector(successResubmitTransferStatusSelector) shouldBe 0
      countMetadataRecordsWithSelector(failedJobSelector) shouldBe 20
      val firstJobRunOutcome: Boolean = await(getJob.scheduledMessage.service.invoke.map(_.asInstanceOf[Boolean]))
      firstJobRunOutcome shouldBe true

      countMetadataRecordsWithSelector(Filters.empty()) shouldBe 40
      countMetadataRecordsWithSelector(successResubmitTransferStatusSelector) shouldBe 10
      countMetadataRecordsWithSelector(failedJobSelector) shouldBe 10

      val secondJobRunOutcome: Boolean = await(getJob.scheduledMessage.service.invoke.map(_.asInstanceOf[Boolean]))
      secondJobRunOutcome shouldBe true

      countMetadataRecordsWithSelector(Filters.empty()) shouldBe 40
      countMetadataRecordsWithSelector(successResubmitTransferStatusSelector) shouldBe 20
      countMetadataRecordsWithSelector(failedJobSelector) shouldBe 0
      val secondStoreDocs: Boolean = await(storeMultipleErsSummary(Fixtures.failedJobsWithDifferentBundleRef))
      secondStoreDocs shouldBe true

      countMetadataRecordsWithSelector(Filters.empty()) shouldBe 50
      countMetadataRecordsWithSelector(successResubmitTransferStatusSelector) shouldBe 20
      countMetadataRecordsWithSelector(failedJobSelector) shouldBe 10
      val thirdJobRunOutcome: Boolean = await(getJob.scheduledMessage.service.invoke.map(_.asInstanceOf[Boolean]))
      thirdJobRunOutcome shouldBe true

      countMetadataRecordsWithSelector(Filters.empty()) shouldBe 50
      countMetadataRecordsWithSelector(successResubmitTransferStatusSelector) shouldBe 30
      countMetadataRecordsWithSelector(failedJobSelector) shouldBe 0
    }
  }
}
