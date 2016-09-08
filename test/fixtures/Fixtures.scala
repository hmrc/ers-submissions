/*
 * Copyright 2016 HM Revenue & Customs
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

import com.typesafe.config.Config
import models._
import org.joda.time.{DateTimeZone, DateTime}
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.{Json, JsObject}
import utils.SubmissionCommon._
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
    status = Some("saved")
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
    status = Some("saved")
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

  val adjustmentEMI: SchemeData = schemeData

  val replacedEMI: SchemeData = SchemeData(
    EMISchemeInfo,
    "EMI40_Replaced_V3",
    None,
    Some(
      ListBuffer(
        Seq("2014-01-01", "2014-01-01", "First", "Second", "Last", nino, "123/123456", "1234.5678", "Company Name", "Company address line 1", "Company address line 2", "Company address line 3", "Company address line 4", "UK", "ZZ98 1ZZ", "1234567899", "XT123456"),
        Seq("2014-01-01", "2014-01-01", "First", "Second", "Last", nino, "123/123456", "1234.5678", "Company Name", "Company address line 1", "Company address line 2", "Company address line 3", "Company address line 4", "UK", "ZZ98 1ZZ", "1234567899", "XT123456")
      )
    )
  )

  val rlcEMI: SchemeData = SchemeData(
    EMISchemeInfo,
    "EMI40_RLC_V3",
    None,
    Some(
      ListBuffer(
        Seq("2014-01-01", "true", "1", "First", "Second", "Last", nino, "123/123456", "10.12", "true", "1234.5678", "true"),
        Seq("2014-01-02", "true", "1", "First", "Second", "Last", nino, "123/123456", "10.12", "true", "1234.5678", "true")
      )
    )
  )

  val nonTaxableEMI: SchemeData = SchemeData(
    EMISchemeInfo,
    "EMI40_NonTaxable_V3",
    None,
    Some(
      ListBuffer(
        Seq("2014-09-08", "First", "", "Last", nino, "333/123456", "299.99", "1.1234", "1.0001", "2.2222", "no", "yes", "CM1234567", "300.0200", "no"),
        Seq("2014-05-15", "First", "", "Last", "", "", "", "", "", "", "yes", "", "", "", "")
      )
    )
  )

  val taxableEMI: SchemeData = SchemeData(
    EMISchemeInfo,
    "EMI40_Taxable_V3",
    None,
    Some(
      ListBuffer(
        Seq("2014-01-01", "no", "", "First", "Second", "Last", nino, "123/123456", "10.12", "1234.5678", "1234.5678", "1234.5678", "1234.5678", "1234.5678", "yes", "", "", "yes", "yes", "1234.5678"),
        Seq("2014-01-02", "no", "", "First", "Second", "Last", nino, "123/123456", "10.12", "1234.5678", "1234.5678", "1234.5678", "1234.5678", "1234.5678", "yes", "", "", "yes", "yes", "1234.5678")
      )
    )
  )

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
    status = Some("saved")
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
    status = Some("saved")
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
    status = Some("saved")
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
    status = Some("saved")
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
    status = Some("saved")
  )

  val postSubmissionData: PostSubmissionData = PostSubmissionData (
    EMISchemeInfo,
    "waiting",
    schemeDataJson
  )

  val otherSchemeType: String = "OTHER"

  val ersJsonStoreInfo = ErsJsonStoreInfo(postSubmissionData.schemeInfo, Some("123456789"), Some("fileName") ,  Some("32190382343934".toLong), Some("321903823439342".toLong), "JsonSaved")

}
