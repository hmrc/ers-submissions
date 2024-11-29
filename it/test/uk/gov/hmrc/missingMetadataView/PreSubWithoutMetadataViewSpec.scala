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

package uk.gov.hmrc.missingMetadataView

import _root_.play.api.libs.json.{JsObject, Json}
import config.ApplicationConfig
import models.{PreSubWithoutMetadata, SchemeData}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.mongo.test.MongoSupport

import java.time.{Instant, LocalDateTime, ZoneOffset}
import scala.concurrent.ExecutionContext

class PreSubWithoutMetadataViewSpec
  extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with MockitoSugar
    with MongoSupport {

  val mockApplicationConfig: ApplicationConfig = mock[ApplicationConfig]

  when(mockApplicationConfig.dateTimeFilterForView).thenReturn("02/05/2023")
  when(mockApplicationConfig.metadataCollection).thenReturn("ers-metadata")
  when(mockApplicationConfig.presubmissionCollection).thenReturn("ers-presubmission")

  val localDateTime: LocalDateTime = LocalDateTime.of(2023, 12, 2, 10, 15, 30)
  val defaultInstant: Instant = localDateTime.toInstant(ZoneOffset.UTC)

  implicit val ec : ExecutionContext = ExecutionContext.global

  "PreSubWithoutMetadataViewService" should {

    "only return presubmission records which were created after the date filter" in new MissingMetadataViewSetup(mockApplicationConfig, mongoComponent) {

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
        future = getViewResult(metaData, presubmissionData),
        timeout = timeout(Span(30, Seconds))
      ) {
        insertedRecordsAndViewRecords: (Long, Long, Seq[PreSubWithoutMetadata]) => {
          insertedRecordsAndViewRecords._1 shouldBe 1
          insertedRecordsAndViewRecords._2 shouldBe 2
          insertedRecordsAndViewRecords._3 shouldBe Seq.empty[PreSubWithoutMetadata]
        }
      }
    }

    "return no presubmission records if all have a matching metadata record" in new MissingMetadataViewSetup(mockApplicationConfig, mongoComponent) {

      val metaData: Seq[JsObject] = Seq(
        createMetadataRecord(taxYear = "2017/18", schemeRef = "CSOP00000000001", defaultInstant),
        createMetadataRecord(taxYear = "2018/19", schemeRef = "CSOP00000000001", defaultInstant)
      ).map(Json.toJsObject(_))

      val presubmissionData: Seq[JsObject] = Seq(
        SchemeData(schemeInfo(taxYear = "2017/18", schemeRef = "CSOP00000000001", defaultInstant), "CSOP_OptionsRCL_V4", None, None),
        SchemeData(schemeInfo(taxYear = "2018/19", schemeRef = "CSOP00000000001", defaultInstant), "CSOP_OptionsRCL_V4", None, None),
      ).map(Json.toJsObject(_))

      whenReady(
        future = getViewResult(metaData, presubmissionData),
        timeout = timeout(Span(30, Seconds))
      ) {
        insertedRecordsAndViewRecords: (Long, Long, Seq[PreSubWithoutMetadata]) => {
          insertedRecordsAndViewRecords._1 shouldBe 2
          insertedRecordsAndViewRecords._2 shouldBe 2
          insertedRecordsAndViewRecords._3 shouldBe Seq.empty[PreSubWithoutMetadata]
        }
      }
    }

    "return presubmission records with no linked metadata records" in new MissingMetadataViewSetup(mockApplicationConfig, mongoComponent){

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
        future = getViewResult(metaData, presubmissionData),
        timeout = timeout(Span(30, Seconds))
      ) {
        insertedRecordsAndViewRecords: (Long, Long, Seq[PreSubWithoutMetadata]) => {
          insertedRecordsAndViewRecords._1 shouldBe 2
          insertedRecordsAndViewRecords._2 shouldBe 3
          insertedRecordsAndViewRecords._3 shouldBe Seq(PreSubWithoutMetadata("CSOP00000000001", "2019/20", 1701512130000L))
        }
      }
    }
  }
}

