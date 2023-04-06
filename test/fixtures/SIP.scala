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

package fixtures

import java.text.SimpleDateFormat

import models._
import org.joda.time.DateTime

object SIP {

  val schemeType: String = "SIP"
  val schemeRef: String = "XA1100000000000"
  val timestamp: DateTime = DateTime.now

  // SIP_Awards_V4
  def buildAwards(withAllFields: Boolean = true, sharesListedOnSE: String, marketValueAgreedHMRC: String): Seq[String] = Seq(
    "2015-12-09",
    "1000",
    "2",
    {
      if(withAllFields) "no"
      else ""
    },
    {
      if(withAllFields) "2/1"
      else ""
    },
    {
      if(withAllFields) "10.1234"
      else ""
    },
    "100.00",
    "10.1234",
    "1000",
    "1000",
    "1000",
    "1000",
    "1000",
    "100",
    sharesListedOnSE,
    if(sharesListedOnSE == "no") marketValueAgreedHMRC else "",
    if(sharesListedOnSE == "no" && marketValueAgreedHMRC == "yes") "aa12345678" else ""
  )

  // SIP_Out_V4
  def buildOutOfPlan(withAllFields: Boolean = true, sharesHeld: String, payeApplied: String): Seq[String] = Seq(
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
    "100.00",
    "100.00",
    "100.00",
    "100.00",
    "10.1234",
    "10.1234",
    "10.1234",
    "10.1234",
    sharesHeld,
    if (sharesHeld == "no") payeApplied else "",
    if (sharesHeld == "no" && payeApplied == "no") "no" else ""
  )

  val schemeInfo: SchemeInfo = SchemeInfo (
    schemeRef = "XA1100000000000",
    timestamp = timestamp,
    schemeId = "123PA12345678",
    taxYear = "2014/15",
    schemeName = "My scheme",
    schemeType = schemeType
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

  val trusteeDetails: TrusteeDetails = TrusteeDetails(
    name = "testCompany",
    addressLine1 = "testAddress1",
    addressLine2 = Some("testAddress2"),
    addressLine3 = Some("testAddress3"),
    addressLine4 = Some("testAddress4"),
    postcode = Some("NE1 1AA"),
    country = Some("UK")
  )

  val trusteeDetailsMin: TrusteeDetails = TrusteeDetails(
    name = "testCompany",
    addressLine1 = "testAddress1",
    addressLine2 = None,
    addressLine3 = None,
    addressLine4 = None,
    postcode = None,
    country = None
  )

  val ersMetadata: ErsMetaData = ErsMetaData(
    schemeInfo = schemeInfo,
    ipRef = "127.0.0.0",
    aoRef = Some("123PA12345678"),
    empRef = "OTHER - MyScheme - XA1100000000000 - 2014/15",
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
    trustees = Some(
      TrusteeDetailsList(
        List(
          trusteeDetails,
          trusteeDetailsMin
        )
      )
    ),
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

  val metadataNilReturnWithAllAltAmmends: ErsSummary = ErsSummary(
    bundleRef = "testbundle",
    isNilReturn = "2",
    fileType = None,
    confirmationDateTime = new DateTime(dateTimeFormat.parse("21 May 2015, 11:12AM")),
    metaData = ersMetadata,
    altAmendsActivity = Some(AltAmendsActivity("1")),
    alterationAmends = Some(
      AlterationAmends(
        altAmendsTerms = Some("1"),
        altAmendsEligibility = Some("1"),
        altAmendsExchange = Some("1"),
        altAmendsVariations = Some("1"),
        altAmendsOther = Some("1")
      )
    ),
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

  val metadataNilReturnWithSomeAltAmmends: ErsSummary = ErsSummary(
    bundleRef = "testbundle",
    isNilReturn = "2",
    fileType = None,
    confirmationDateTime = new DateTime(dateTimeFormat.parse("21 May 2015, 11:12AM")),
    metaData = ersMetadata,
    altAmendsActivity = Some(AltAmendsActivity("1")),
    alterationAmends = Some(
      AlterationAmends(
        altAmendsTerms = Some("1"),
        altAmendsEligibility = None,
        altAmendsExchange = Some("1"),
        altAmendsVariations = Some("2"),
        altAmendsOther = Some("1")
      )
    ),
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
