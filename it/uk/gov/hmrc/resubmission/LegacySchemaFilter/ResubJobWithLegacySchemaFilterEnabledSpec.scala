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

package uk.gov.hmrc.resubmission.LegacySchemaFilter

import _root_.play.api.Application
import _root_.play.api.inject.guice.GuiceApplicationBuilder
import _root_.play.api.test.Helpers._
import models.{ErsSummary, SchemeData}
import org.mongodb.scala.model.Filters
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.resubmission.ResubmissionJobSetUp
import uk.gov.hmrc.{FakeAuthService, FakeErsStubService, Fixtures}

class ResubJobWithLegacySchemaFilterEnabledSpec extends AnyWordSpecLike
  with Matchers
  with GuiceOneServerPerSuite
  with FakeErsStubService
  with FakeAuthService {

  val legacySchemaOne = "schema1"
  val legacySchemaTwo = "schema2"

  val applicationConfig: Map[String, Any] = Map(
    "microservice.services.ers-stub.port" -> "19339",
    "auditing.enabled" -> false,
    "schedules.resubmission-service.dateTimeFilter.enabled" -> false,
    "schedules.resubmission-service.schemaRefsFilter.enabled" -> false,
    "schedules.resubmission-service.legacySchemaFilter.enabled" -> true,
    "schedules.resubmission-service.legacySchemaFilter.filter" -> Seq(legacySchemaOne, legacySchemaTwo).mkString(","),
    "schedules.resubmission-service.schemaFilter.enabled" -> false,
    "schedules.resubmission-service.schemaFilter.filter" -> "CSOP",
    "schedules.resubmission-service.resubmissionLimit" -> 10,
    "schedules.resubmission-service.resubmit-list-statuses" -> "failed",
    "schedules.resubmission-service.resubmit-successful-status" -> "successResubmit"
  )

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      conf = applicationConfig
    )
    .build()

  "With legacySchemaFilter enabled ResubPresubmissionServiceJob" should {
    "resubmit failed jobs with the correct transfer status, schema type and submitted after dateTimeFilter" in new ResubmissionJobSetUp(app = app) {

      val legacyErsSubmission: ErsSummary = Fixtures.buildErsSummary(transferStatus = Some("failed"), schemaType = "EMI", schemeRef = legacySchemaOne)
      val notLegacyErsSubmission: ErsSummary = Fixtures.buildErsSummary(transferStatus = Some("failed"), schemaType = "EMI", schemeRef = "not_leggacy")

      val ersSummaries: Seq[JsObject] = Seq(
        legacyErsSubmission,
        notLegacyErsSubmission
      ).map(Json.toJsObject(_))

      val schemeData: Seq[JsObject] = Seq(
        Fixtures.schemeData(legacyErsSubmission.metaData.schemeInfo, "EMI40_Adjustments_V3"),
        Fixtures.schemeData(notLegacyErsSubmission.metaData.schemeInfo, "EMI40_Adjustments_V4")
      ).map(Json.toJsObject(_))

      val storeErsSubDocs: Boolean = await(storeMultipleErsSummary(ersSummaries))
      storeErsSubDocs shouldBe true
      val storeSchemeDocs: Boolean = await(storeMultipleSchemeData(schemeData))
      storeSchemeDocs shouldBe true

      countMetadataRecordsWithSelector(Filters.empty()) shouldBe 2
      countMetadataRecordsWithSelector(successResubmitTransferStatusSelector) shouldBe 0
      countMetadataRecordsWithSelector(failedJobSelector) shouldBe 2

      val updateCompleted: Boolean = await(getJob.scheduledMessage.service.invoke.map(_.asInstanceOf[Boolean]))
      updateCompleted shouldBe true

      countMetadataRecordsWithSelector(Filters.empty()) shouldBe 2
      countMetadataRecordsWithSelector(successResubmitTransferStatusSelector) shouldBe 2
      countMetadataRecordsWithSelector(failedJobSelector) shouldBe 0
    }
  }

}


