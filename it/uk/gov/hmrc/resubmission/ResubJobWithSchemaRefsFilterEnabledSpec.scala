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
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.mongodb.scala.model.Filters
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import uk.gov.hmrc.{CSOP, FakeErsStubService, Fixtures}

import scala.collection.mutable.ListBuffer

class ResubJobWithSchemaRefsFilterEnabledSpec extends AnyWordSpecLike
  with Matchers
  with GuiceOneServerPerSuite
  with FakeErsStubService {

  val applicationConfig: Map[String, Any] = Map(
    "microservice.services.ers-stub.port" -> "19339",
    "schedules.resubmission-service.enabled" -> true,
    "schedules.resubmission-service.dateTimeFilter.enabled" -> false,
    "schedules.resubmission-service.schemaRefsFilter.enabled" -> true,
    "schedules.resubmission-service.schemaFilter.enabled" -> false,
    "schedules.resubmission-service.schemaRefsFilter.filter" -> "123,789,101",
    "schedules.resubmission-service.resubmissionLimit" -> 2,
    "schedules.resubmission-service.resubmit-list-statuses" -> "failed",
    "schedules.resubmission-service.resubmit-fail-status" -> "failedResubmission",
    "schedules.resubmission-service.resubmit-successful-status" -> "successResubmit",
    "auditing.enabled" -> false
  )

  val formatter: DateTimeFormatter = DateTimeFormat.forPattern("dd/MM/yyyy")

  val submissionDatesAndSchemeRefs: Seq[(DateTime, String)] = Seq(
    (DateTime.parse("11/04/2023", formatter), "123"),
    (DateTime.parse("30/04/2023", formatter), "456"),
    (DateTime.parse("10/05/2023", formatter), "789"),
    (DateTime.parse("20/05/2023", formatter), "101"),
    (DateTime.parse("16/05/2023", formatter), "121")
  )

  val ersSummaries: Seq[ErsSummary] = submissionDatesAndSchemeRefs.map {
    case (submissionDate: DateTime, schemeRef: String) =>
      Fixtures.buildErsSummary(transferStatus = Some("failed"), schemaType = "CSOP", timestamp = submissionDate, schemeRef = schemeRef)
  } ++ Seq(
    Fixtures.buildErsSummary(transferStatus = Some("passed"), schemaType = "CSOP"),
    Fixtures.buildErsSummary(transferStatus = Some("successResubmit"), schemaType = "CSOP")
  )

  val presubmissionsData: Seq[SchemeData] = submissionDatesAndSchemeRefs.map {
    case (submissionDate: DateTime, schemeRef: String) =>
      SchemeData(
        CSOP.schemeInfo.copy(timestamp = submissionDate, schemeRef = schemeRef),
        "CSOP_OptionsRCL_V4",
        None,
        Some(ListBuffer(CSOP.buildOptionsRCL(withAllFields = true, "yes")))
      )
  }

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      conf = applicationConfig
    )
    .build()

  "ResubPresubmissionServiceJob" should {
    "resubmit failed jobs with the correct transfer status and in the scheme filter list" +
      ", assigning successful jobs with the correct status" in new ResubmissionJobSetUp(app = app) {

      val storeDocs: Boolean = await(storeMultipleErsSummary(ersSummaries.map(Json.toJsObject(_))))
      val storePresubmissions: Boolean = await(storeMultiplePresubmissionData(presubmissionsData.map(Json.toJsObject(_))))
      storeDocs shouldBe true
      storePresubmissions shouldBe true

      countMetadataRecordsWithSelector(Filters.empty()) shouldBe 7
      countMetadataRecordsWithSelector(successResubmitTransferStatusSelector) shouldBe 1
      countMetadataRecordsWithSelector(failedJobSelector) shouldBe 3

      val firstUpdateCompleted: Either[ERSError, Boolean] = await(getJob.scheduledMessage.service.invoke.value)
      firstUpdateCompleted shouldBe Right(true)

      countMetadataRecordsWithSelector(Filters.empty()) shouldBe 7
      countMetadataRecordsWithSelector(successResubmitTransferStatusSelector) shouldBe 3 // 2 resubmissions
      countMetadataRecordsWithSelector(failedJobSelector) shouldBe 1

      val secondUpdateCompleted: Either[ERSError, Boolean] = await(getJob.scheduledMessage.service.invoke.value)
      secondUpdateCompleted shouldBe Right(true)

      countMetadataRecordsWithSelector(Filters.empty()) shouldBe 7
      countMetadataRecordsWithSelector(successResubmitTransferStatusSelector) shouldBe 4 // 1 resubmission
      countMetadataRecordsWithSelector(failedJobSelector) shouldBe 0
    }
  }
}