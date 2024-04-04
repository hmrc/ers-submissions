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

package test.uk.gov.hmrc.resubmission

import _root_.play.api.Application
import _root_.play.api.inject.guice.GuiceApplicationBuilder
import _root_.play.api.test.Helpers._
import models.ERSError
import org.mongodb.scala.model.Filters
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import test.uk.gov.hmrc.{FakeErsStubService, Fixtures}

class ResubJobWithDateFilterEnabledSpec extends AnyWordSpecLike
  with Matchers
  with GuiceOneServerPerSuite
  with FakeErsStubService {

  val applicationConfig: Map[String, Any] = Map(
    "microservice.services.ers-stub.port" -> "19339",
    "schedules.resubmission-service.enabled" -> true,
    "schedules.resubmission-service.dateTimeFilter.enabled" -> true,
    "schedules.resubmission-service.dateTimeFilter.filter" -> "01/05/2023",
    "schedules.resubmission-service.schemaRefsFilter.enabled" -> false,
    "schedules.resubmission-service.schemaFilter.enabled" -> false,
    "schedules.resubmission-service.schemaFilter.filter" -> "CSOP",
    "schedules.resubmission-service.resubmissionLimit" -> 10,
    "schedules.resubmission-service.resubmit-list-statuses" -> "failed",
    "schedules.resubmission-service.resubmit-fail-status" -> "failedResubmission",
    "schedules.resubmission-service.resubmit-successful-status" -> "successResubmit",
    "auditing.enabled" -> false,
    "schedules.resubmission-service.additional-logs.enabled" -> true
  )

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      conf = applicationConfig
    )
    .build()

  "With dateTimeFiltering enabled ResubPresubmissionServiceJob" should {
    "resubmit failed jobs with the correct transfer status, schema type and submitted after dateTimeFilter" in new ResubmissionJobSetUp(app = app) {

      val storeDocs: Boolean = await(storeMultipleErsSummary(Fixtures.ersSummaries))
      val storePresubmissions: Boolean = await(storeMultiplePresubmissionData(Fixtures.schemeData))
      storeDocs shouldBe true
      storePresubmissions shouldBe true

      countMetadataRecordsWithSelector(Filters.empty()) shouldBe 6
      countMetadataRecordsWithSelector(successResubmitTransferStatusSelector) shouldBe 1
      countMetadataRecordsWithSelector(failedJobSelector) shouldBe 2

      val updateCompleted: Either[ERSError, Boolean] = await(getJob.scheduledMessage.service.invoke.value)
      updateCompleted shouldBe Right(true)

      countMetadataRecordsWithSelector(Filters.empty()) shouldBe 6

      countMetadataRecordsWithSelector(successResubmitTransferStatusSelector) shouldBe 3
      countMetadataRecordsWithSelector(failedJobSelector) shouldBe 0
    }
  }
}
