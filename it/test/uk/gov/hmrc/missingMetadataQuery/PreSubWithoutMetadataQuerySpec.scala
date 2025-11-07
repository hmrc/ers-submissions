/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.missingMetadataQuery

import _root_.play.api.libs.json.{JsNull, JsObject, Json}
import config.ApplicationConfig
import models.{PreSubWithoutMetadata, SchemeData}
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import repositories.Repositories

import java.time.{Instant, LocalDateTime, ZoneOffset}
import scala.concurrent.ExecutionContext

class PreSubWithoutMetadataQuerySpec
  extends AnyWordSpec
    with Matchers
    with MockitoSugar {

  val mockApplicationConfig: ApplicationConfig = mock[ApplicationConfig]
  val mockRepositories: Repositories = mock[Repositories]

  when(mockApplicationConfig.dateTimeFilterForQuery).thenReturn("02/05/2023")
  when(mockApplicationConfig.metadataCollection).thenReturn("ers-metadata")
  when(mockApplicationConfig.presubmissionCollection).thenReturn("ers-presubmission")

  val localDateTime: LocalDateTime = LocalDateTime.of(2023, 12, 2, 10, 15, 30)
  val defaultInstant: Instant = localDateTime.toInstant(ZoneOffset.UTC)

  implicit val ec: ExecutionContext = ExecutionContext.global

  "PreSubWithoutMetadataQueryService" should {

    "only return presubmission records which were created after the date filter" in new MissingMetadataQuerySetup(mockApplicationConfig) {

      val metaData: Seq[JsObject] = Seq(
        createMetadataRecord(taxYear = "2017/18", schemeRef = "CSOP00000000001", defaultInstant),
      ).map(Json.toJsObject(_))

      val localDateTimeBeforeDateFilter: LocalDateTime = LocalDateTime.of(2023, 5, 1, 10, 15, 30)
      val instantBeforeDateFilter: Instant = localDateTimeBeforeDateFilter.toInstant(ZoneOffset.UTC)

      val presubmissionData: Seq[JsObject] = Seq(
        SchemeData(schemeInfo(taxYear = "2017/18", schemeRef = "CSOP00000000001", instantBeforeDateFilter), "CSOP_OptionsRCL_V4", None, None),
        SchemeData(schemeInfo(taxYear = "2018/19", schemeRef = "CSOP00000000001", instantBeforeDateFilter), "CSOP_OptionsRCL_V4", None, None),
      ).map(Json.toJsObject(_))

      whenReady(
        future = getQueryResult(metaData, presubmissionData),
        timeout = timeout(Span(30, Seconds))
      ) {
        testQueryResults: TestQueryResults => {
          testQueryResults.numMetadataRecords shouldBe 1
          testQueryResults.numPreSubRecords shouldBe 2
          testQueryResults.queryResults shouldBe Seq.empty[PreSubWithoutMetadata]
          testQueryResults.queryErrors.length shouldBe 0
        }
      }
    }

    "return no presubmission records if all have a matching metadata record" in new MissingMetadataQuerySetup(mockApplicationConfig) {

      val metaData: Seq[JsObject] = Seq(
        createMetadataRecord(taxYear = "2017/18", schemeRef = "CSOP00000000001", defaultInstant),
        createMetadataRecord(taxYear = "2018/19", schemeRef = "CSOP00000000001", defaultInstant)
      ).map(Json.toJsObject(_))

      val presubmissionData: Seq[JsObject] = Seq(
        SchemeData(schemeInfo(taxYear = "2017/18", schemeRef = "CSOP00000000001", defaultInstant), "CSOP_OptionsRCL_V4", None, None),
        SchemeData(schemeInfo(taxYear = "2018/19", schemeRef = "CSOP00000000001", defaultInstant), "CSOP_OptionsRCL_V4", None, None),
      ).map(Json.toJsObject(_))

      whenReady(
        future = getQueryResult(metaData, presubmissionData),
        timeout = timeout(Span(30, Seconds))
      ) {
        testQueryResults: TestQueryResults => {
          testQueryResults.numMetadataRecords shouldBe 2
          testQueryResults.numPreSubRecords shouldBe 2
          testQueryResults.queryResults shouldBe Seq.empty[PreSubWithoutMetadata]
          testQueryResults.queryErrors.length shouldBe 0
        }
      }
    }

    "return presubmission records with no linked metadata records" in new MissingMetadataQuerySetup(mockApplicationConfig) {

      val metaData: Seq[JsObject] = Seq(
        createMetadataRecord(taxYear = "2017/18", schemeRef = "CSOP00000000001", defaultInstant),
        createMetadataRecord(taxYear = "2018/19", schemeRef = "CSOP00000000001", defaultInstant)
      ).map(Json.toJsObject(_))

      val presubmissionData: Seq[JsObject] = Seq(
        SchemeData(schemeInfo(taxYear = "2017/18", schemeRef = "CSOP00000000001", defaultInstant), "CSOP_OptionsRCL_V4", None, None),
        SchemeData(schemeInfo(taxYear = "2018/19", schemeRef = "CSOP00000000001", defaultInstant), "CSOP_OptionsRCL_V4", None, None),
        SchemeData(schemeInfo(taxYear = "2019/20", schemeRef = "CSOP00000000001", defaultInstant), "CSOP_OptionsRCL_V4", None, None)
      ).map(Json.toJsObject(_))

      whenReady(
        future = getQueryResult(metaData, presubmissionData),
        timeout = timeout(Span(30, Seconds))
      ) {
        testQueryResults: TestQueryResults => {
          testQueryResults.numMetadataRecords shouldBe 2
          testQueryResults.numPreSubRecords shouldBe 3
          testQueryResults.queryResults shouldBe Seq(PreSubWithoutMetadata("CSOP00000000001", "2019/20", 1701512130000L))
          testQueryResults.queryErrors.length shouldBe 0
        }
      }
    }

    "return query errors when presubmission records have invalid structure" in new MissingMetadataQuerySetup(mockApplicationConfig) {

      val metaData: Seq[JsObject] = Seq(
        createMetadataRecord(taxYear = "2017/18", schemeRef = "CSOP00000000001", defaultInstant),
        createMetadataRecord(taxYear = "2018/19", schemeRef = "CSOP00000000001", defaultInstant)
      ).map(Json.toJsObject(_) ++ JsObject((Seq("x" -> JsNull))))

      //  has schemeInfo with required fields, but with a wrong type to trigger JsError case in validateJson
      val presubmissionData: Seq[JsObject] = Seq(
        JsObject(
          Seq(
            "schemeInfo" -> JsObject(
              Seq(
                "taxYear" -> Json.toJson("2019/20"),
                "schemeRef" -> Json.toJson(123), // wrong type - number instead of string
                "timestamp" -> Json.toJson(defaultInstant.toEpochMilli)
              )
            ),
            "sheetName" -> Json.toJson("CSOP_OptionsRCL_V4")
          )
        )
      )

      whenReady(
        future = getQueryResult(metaData, presubmissionData),
        timeout = timeout(Span(30, Seconds))
      ) {
        testQueryResults: TestQueryResults => {
          testQueryResults.numMetadataRecords shouldBe 2
          testQueryResults.numPreSubRecords shouldBe 1
          testQueryResults.queryResults shouldBe Seq.empty[PreSubWithoutMetadata]
          testQueryResults.queryErrors.length shouldBe 1
          testQueryResults.queryErrors.head shouldBe "/schemeRef: JsonValidationError(List(error.expected.jsstring),List())"
        }
      }
    }
  }
}
