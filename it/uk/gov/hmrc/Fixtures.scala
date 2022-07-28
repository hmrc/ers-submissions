/*
 * Copyright 2022 HM Revenue & Customs
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

import _root_.play.api.libs.json.{JsValue, Json, JsObject}
import models._
import org.joda.time.{DateTime, DateTimeZone}

import scala.collection.mutable.ListBuffer

object Fixtures {
  val invalidPayload: JsObject = Json.obj("invalid data" -> "test")

  val timestamp: DateTime = DateTime.now(DateTimeZone.UTC)

  val schemeInfo: SchemeInfo = SchemeInfo (
    schemeRef = "XA1100000000000",
    timestamp = timestamp,
    schemeId = "123PA12345678",
    taxYear = "2014/15",
    schemeName = "EMI",
    schemeType = "EMI"
  )
  val schemeInfoPayload: JsValue = Json.toJson(schemeInfo)

  val submissionsSchemeData: SubmissionsSchemeData = SubmissionsSchemeData(
    schemeInfo,
    "EMI40_Adjustments_V3",
    UpscanCallback("EMI40_Adjustments_V3", "http://localhost:19000/fakeDownload"),
    1
  )
  val submissionsSchemeDataJson: JsObject = Json.toJson(submissionsSchemeData).as[JsObject]

  val ersMetaData = ErsMetaData(
    schemeInfo = schemeInfo,
    ipRef = "127.0.0.0",
    aoRef = Some("123PA12345678"),
    empRef = "EMI - MyScheme - XA1100000000000 - 2014/15",
    agentRef = None,
    sapNumber = Some("sap-123456")
  )

  val schemeData: SchemeData = SchemeData(
    schemeInfo,
    "EMI40_Adjustments_V3",
    None,
    Some(
      ListBuffer(
        Seq("no", "no", "yes", "3", "2015-12-09", "John", "", "Doe", "AA123456A", "123/XZ55555555", "10.1234", "100.12", "10.1234", "10.1234"),
        Seq("no", "no", "no", "", "2015-12-09", "John", "", "Doe", "AA123456A", "123/XZ55555555", "10.1234", "100.12", "10.1234", "10.1234"),
        Seq("yes", "", "", "", "2015-12-09", "John", "Middle", "Doe", "AA123456A", "123/XZ55555555", "10.1234", "100.12", "10.1234", "10.1234")
      )
    )
  )

  val schemeDataPayload: JsValue = Json.toJson(schemeData)

  val companyDetails: CompanyDetails = CompanyDetails(
    "testCompany",
    "testAddress1",
    Some("testAddress2"),
    Some("testAddress3"),
    Some("testAddress4"),
    Some("NE1 1AA"),
    Some("United Kingdom"),
    Some("1234567890"),
    Some("1234567890")
  )

  def buildErsSummary(isNilReturn: Boolean): ErsSummary = ErsSummary(
    bundleRef = "testbundle",
    isNilReturn = if(isNilReturn) "2" else "1",
    fileType = Some("ods"),
    confirmationDateTime = timestamp,
    metaData = ersMetaData,
    altAmendsActivity = None,
    alterationAmends = None,
    groupService = Some(
      GroupSchemeInfo(
        groupScheme = Option("1"),
        groupSchemeType = Option(".ods")
      )
    ),
    schemeOrganiser = None,
    companies = Some(
      CompanyDetailsList(
        List(companyDetails)
      )
    ),
    trustees = None,
    nofOfRows = None,
    transferStatus = Some("saved")
  )

  def buildErsSummaryPayload(isNilReturn: Boolean): JsValue = Json.toJson(
    buildErsSummary(isNilReturn)
  )
}
