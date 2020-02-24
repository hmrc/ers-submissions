/*
 * Copyright 2020 HM Revenue & Customs
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

import java.text.SimpleDateFormat

import models._
import org.joda.time.DateTime

object EMI {

  val schemeType: String = "EMI"
  val schemeRef: String = "XA1100000000000"
  val timestamp: DateTime = DateTime.now

  val schemeInfo: SchemeInfo = SchemeInfo (
    schemeRef = schemeRef,
    timestamp = timestamp,
    schemeId = "123PA12345678",
    taxYear = "2015/16",
    schemeName = "My scheme",
    schemeType = schemeType
  )

  // EMI40_Adjustments_V3
  def buildAdjustmentsV3(withAllFields: Boolean = true, disqualifyingEvent: String): Seq[String] = Seq(
    "yes",
    "yes",
    disqualifyingEvent,
    if(disqualifyingEvent == "yes") "4" else "",
    "2011-10-13",
    "First",
    {
      if(withAllFields) "Second"
      else ""
    },
    "Last",
    {
      if(withAllFields) "NINO"
      else ""
    },
    "123/XZ55555555",
    "10.1234",
    "10.14",
    "10.1324",
    "10.1244"
  )

  // EMI40_Replaced_V3
  def buildReplacedV3(withAllFields: Boolean = true): Seq[String] = Seq(
    "2014-12-10",
    "2014-12-10",
    "First",
    {
      if(withAllFields) "Second"
      else ""
    },
    "Last",
    {
      if(withAllFields) "NINO"
      else ""
    },
    "123/XZ55555555",
     "10.1234",
    "company",
    "1 Beth Street",
    {
      if(withAllFields) "Bucknall"
      else ""
    },
    {
      if(withAllFields) "Stoke"
      else ""
    },
    {
      if(withAllFields) "Staffordshire"
      else ""
    },
    "UK",
    "SE1 2AB",
    "XT123456",
    "1234567899"
  )

  // EMI40_RLC_V3
  def buildRLCV3(withAllFields: Boolean = true, disqualifyingEvent: String, moneyValueReceived: String): Seq[String] = Seq(
    "2014-12-10",
    disqualifyingEvent,
    if(disqualifyingEvent == "yes") "1" else "",
    "First",
    {
      if(withAllFields) "Second"
      else ""
    },
    "Last",
    {
      if(withAllFields) "NINO"
      else ""
    },
    "123/XZ55555555",
    "10.12",
    moneyValueReceived,
    if(moneyValueReceived == "yes") "123.1234" else "",
    if(moneyValueReceived == "yes") "yes" else ""
  )

  // EMI40_NonTaxable_V3
  def buildNonTaxableV3(withAllFields: Boolean = true, sharesListedOnSE: String, marketValueAgreedHMRC: String): Seq[String] = Seq(
    "2015-03-03",
    "First",
    {
      if(withAllFields) "Second"
      else ""
    },
    "Last",
    {
      if(withAllFields) "NINO"
      else ""
    },
    "123/XZ55555555",
    "100",
    "10.1234",
    "10.1234",
    "10.1234",
    sharesListedOnSE,
    if(sharesListedOnSE == "no") marketValueAgreedHMRC else "",
    if(sharesListedOnSE == "no" && marketValueAgreedHMRC == "yes") "aa12345678" else "",
    "10.1234",
    "yes"
  )

  // EMI40_Taxable_V3
  def buildTaxableV3(withAllFields: Boolean = true, disqualifyingEvent: String, sharesListedOnSE: String, marketValueAgreedHMRC: String): Seq[String] = Seq(
    "2015-06-04",
    disqualifyingEvent,
    if(disqualifyingEvent == "yes") "3" else "",
    "First",
    {
      if(withAllFields) "Second"
      else ""
    },
    "Last",
    {
      if(withAllFields) "NINO"
      else ""
    },
    "123/XZ55555555",
    "100.00",
    "10.1234",
    "10.1234",
    "10.1234",
    "10.1234",
    "10.1234",
    sharesListedOnSE,
    if(sharesListedOnSE == "no") marketValueAgreedHMRC else "",
    if(sharesListedOnSE == "no" && marketValueAgreedHMRC == "yes") "aa12345678" else "",
    "yes",
    "yes",
    "10.1234"
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
  val companyDetailsMin: CompanyDetails = CompanyDetails(
    "testCompany",
    "testAddress1",
    None,
    None,
    None,
    None,
    None,
    None,
    None
  )

  val ersMetadata: ErsMetaData = ErsMetaData(
    schemeInfo = schemeInfo,
    ipRef = "127.0.0.0",
    aoRef = Some("123PA12345678"),
    empRef = "EMI - MyScheme - XA1100000000000 - 2015/16",
    agentRef = None,
    sapNumber = Some("sap-123456")
  )

  val dateTimeFormat = new SimpleDateFormat("d MMMM yyyy, h:mma")

  val metadata: ErsSummary = ErsSummary(
    bundleRef = "testbundle",
    isNilReturn = "1",
    fileType = Some("ods"),
    confirmationDateTime = new DateTime(dateTimeFormat.parse("21 May 2015, 11:12AM")),
    metaData = ersMetadata,
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
          companyDetailsMin
        )
      )
    ),
    trustees = None,
    nofOfRows = None,
    transferStatus = Some("saved")
  )


  val metadataNilReturn: ErsSummary = ErsSummary(
    bundleRef = "testbundle",
    isNilReturn = "2",
    fileType = None,
    confirmationDateTime = new DateTime(dateTimeFormat.parse("21 May 2015, 11:12AM")),
    metaData = ersMetadata,
    altAmendsActivity = None,
    alterationAmends = None,
    groupService = Some(
      GroupSchemeInfo(
        groupScheme = Option("1"),
        groupSchemeType = Option(".ods")
      )
    ),
    schemeOrganiser = None,
    companies = None,
    trustees = None,
    nofOfRows = None,
    transferStatus = Some("saved")
  )
}
