/*
 * Copyright 2019 HM Revenue & Customs
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

package fixtures

import models._
import org.joda.time.{DateTimeZone, DateTime}
import play.api.libs.json.{Json, JsObject}
import scala.collection.mutable.ListBuffer
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.domain.Generator

object Fixtures extends MockitoSugar {

  lazy val nino = new Generator().nextNino.nino

  val timestamp: DateTime = new DateTime().withDate(2015,12,5).withTime(12,50,55,0).withZone(DateTimeZone.UTC) //DateTime.now

  val schemeType = "EMI"

  val schemeInfo: SchemeInfo = SchemeInfo (
    schemeRef = "XA1100000000000",
    timestamp = timestamp,
    schemeId = "123PA12345678",
    taxYear = "2014/15",
    schemeName = "My scheme",
    schemeType = "EMI"
  )

  val summaryData = ErsSummary(
    bundleRef = "123453222",
    isNilReturn = "true",
    fileType = Some("ods"),
    confirmationDateTime = timestamp,
    metaData = ErsMetaData(
      schemeInfo = schemeInfo,
      ipRef = "127.0.0.0",
      aoRef = None,
      empRef = "EMI - MyScheme - XA1100000000000 - 2014/15",
      agentRef = None,
      sapNumber = None
    ),
    altAmendsActivity = None,
    alterationAmends = None,
    groupService = None,
    schemeOrganiser = None,
    companies = None,
    trustees = None,
    nofOfRows = None,
    transferStatus = Some("saved")
  )

  val EMISchemeInfo: SchemeInfo = SchemeInfo (
    schemeRef = "XA1100000000000",
    timestamp = timestamp,
    schemeId = "123PA12345678",
    taxYear = "2014/15",
    schemeName = "My scheme",
    schemeType = schemeType
  )

  val EMIMetaData = ErsMetaData(
    schemeInfo = EMISchemeInfo,
    ipRef = "127.0.0.0",
    aoRef = Some("123PA12345678"),
    empRef = "EMI - MyScheme - XA1100000000000 - 2014/15",
    agentRef = None,
    sapNumber = Some("sap-123456")
  )

  val EMISummaryDate = ErsSummary(
    bundleRef = "123453222",
    isNilReturn = "true",
    fileType = Some("ods"),
    confirmationDateTime = timestamp,
    metaData = EMIMetaData,
    altAmendsActivity = None,
    alterationAmends = None,
    groupService = None,
    schemeOrganiser = None,
    companies = None,
    trustees = None,
    nofOfRows = None,
    transferStatus = Some("saved")
  )

  val scheetName: String = "EMI40_Adjustments_V3"
  val data: Option[ListBuffer[Seq[String]]] = Some(
    ListBuffer(
      Seq("no", "no", "yes", "3", "2015-12-09", "First", "", "Last", nino, "123/123456", "10.1234", "100.12", "10.1234", "10.1234"),
      Seq("no", "no", "no", "", "2015-12-09", "First", "", "Last", nino, "123/123456", "10.1234", "100.12", "10.1234", "10.1234"),
      Seq("yes", "", "", "", "2015-12-09", "First", "Second", "Last", nino, "123/XZ55555555", "10.1234", "100.12", "10.1234", "10.1234")
    )
  )

  val schemeData: SchemeData = SchemeData(
    EMISchemeInfo,
    scheetName,
    None,
    data
  )

  val schemeDataJson: JsObject = Json.toJson(schemeData).as[JsObject]

  val invalidJson: JsObject = Json.obj(
    "metafield1" -> "metavalue1",
    "metafield2" -> "metavalue2",
    "metafield3" -> "metavalue3"
  )

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

  val metadata: ErsSummary = ErsSummary(
    bundleRef = "testbundle",
    isNilReturn = "1",
    fileType = Some("ods"),
    confirmationDateTime = DateTime.now,
    metaData = EMIMetaData,
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
        List(
          companyDetails,
          companyDetails
        )
      )
    ),
    trustees = None,
    nofOfRows = None,
    transferStatus = Some("saved")
  )

  val metadataJson: JsObject = Json.toJson(metadata).as[JsObject]

  val metadataNilReturn: ErsSummary = ErsSummary(
    bundleRef = "testbundle",
    isNilReturn = "2",
    fileType = None,
    confirmationDateTime = DateTime.now,
    metaData = EMIMetaData,
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

  val invalidMetadataNilReturn: ErsSummary = ErsSummary(
    bundleRef = "testbundle",
    isNilReturn = "5",
    fileType = None,
    confirmationDateTime = DateTime.now,
    metaData = EMIMetaData,
    altAmendsActivity = None,
    alterationAmends = None,
    groupService = None,
    schemeOrganiser = None,
    companies = None,
    trustees = None,
    nofOfRows = None,
    transferStatus = Some("saved")
  )

  val invalidMetadataMissingSchemeRef: ErsSummary = ErsSummary(
    bundleRef = "testbundle",
    isNilReturn = "2",
    fileType = None,
    confirmationDateTime = DateTime.now,
    metaData = ErsMetaData(
      schemeInfo = SchemeInfo (
        schemeRef = "",
        timestamp = timestamp,
        schemeId = "123PA12345678",
        taxYear = "2014/15",
        schemeName = "My scheme",
        schemeType = schemeType
      ),
      ipRef = "127.0.0.0",
      aoRef = Some("123PA12345678"),
      empRef = "EMI - MyScheme - XA1100000000000 - 2014/15",
      agentRef = None,
      sapNumber = Some("sap-123456")
    ),
    altAmendsActivity = None,
    alterationAmends = None,
    groupService = None,
    schemeOrganiser = None,
    companies = None,
    trustees = None,
    nofOfRows = None,
    transferStatus = Some("saved")
  )

  val invalidMetadataMissingSchemeType: ErsSummary = ErsSummary(
    bundleRef = "testbundle",
    isNilReturn = "2",
    fileType = None,
    confirmationDateTime = DateTime.now,
    metaData = ErsMetaData(
      schemeInfo = SchemeInfo (
        schemeRef = "XA1100000000000",
        timestamp = timestamp,
        schemeId = "123PA12345678",
        taxYear = "2014/15",
        schemeName = "My scheme",
        schemeType = ""
      ),
      ipRef = "127.0.0.0",
      aoRef = Some("123PA12345678"),
      empRef = "EMI - MyScheme - XA1100000000000 - 2014/15",
      agentRef = None,
      sapNumber = Some("sap-123456")
    ),
    altAmendsActivity = None,
    alterationAmends = None,
    groupService = None,
    schemeOrganiser = None,
    companies = None,
    trustees = None,
    nofOfRows = None,
    transferStatus = Some("saved")
  )

}
