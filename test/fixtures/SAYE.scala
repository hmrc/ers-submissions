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

import java.text.SimpleDateFormat

import models._
import org.joda.time.DateTime


object SAYE {

  val sayeSchemeType: String = "SAYE"
  val schemeRef: String = "XA1100000000000"
  val timestamp: DateTime = DateTime.now


  def buildExercisedV3(allFields: Boolean, sharesListedOnSE: String, marketValueAgreedHMRC: String): Seq[String] = Seq(
    "2014-01-01",
    "First",
    {
      if(allFields)"Second"
      else ""
    },
    "Last",
    {
      if(allFields)"NINO"
      else ""
    },
    "123/XZ55555555",
    "2015-12-31",
    "10.12",
    sharesListedOnSE,
    if(sharesListedOnSE == "no") marketValueAgreedHMRC else "",
    if(sharesListedOnSE == "no" && marketValueAgreedHMRC == "yes") "aa12345678" else "",
    "10.1234",
    "10.1234",
    "11.1234",
    "yes",
    "yes"
  )

  def buildGrantedV3(sharesListedOnSE: String, marketValueAgreedHMRC: String): Seq[String] = Seq(
    "2015-12-31",
    "123456",
    "100.00",
    "10.1234",
    "10.1234",
    sharesListedOnSE,
    if(sharesListedOnSE == "no") marketValueAgreedHMRC else "",
    if(sharesListedOnSE == "no" && marketValueAgreedHMRC == "yes") "aa12345678" else ""
  )

  def buildRCLV3(wasMoneyOrValueGiven:String="yes",secondName:String="Second",nino:String="NINO"): Seq[String] = Seq(
    "2015-12-31",
    wasMoneyOrValueGiven,
    if(wasMoneyOrValueGiven == "yes") "10.1234" else "",
    "First",
    secondName,
    "Last",
    nino,
    "123/XZ55555555",
    "yes"
  )

  val schemeInfo: SchemeInfo = SchemeInfo (
    schemeRef = "XA1100000000000",
    timestamp = timestamp,
    schemeId = "123PA12345678",
    taxYear = "2014/15",
    schemeName = "My scheme",
    schemeType = sayeSchemeType
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
    empRef = "OTHER - MyScheme - XA1100000000000 - 2014/15",
    agentRef = None,
    sapNumber = Some("sap-123456")
  )

  val dateTimeFormat = new SimpleDateFormat("d MMMM yyyy, h:mma")

  val ersSumarry: ErsSummary = ErsSummary(
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

  val ersSumarryWithAllAmmends: ErsSummary = ErsSummary(
    bundleRef = "testbundle",
    isNilReturn = "1",
    fileType = Some("ods"),
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


  val ersSummaryNilReturn: ErsSummary = ErsSummary(
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


  val ersSummaryNilReturnWithAllAltAmmends: ErsSummary = ErsSummary(
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

  val ersSummaryNilReturnWithSomeAltAmmends: ErsSummary = ErsSummary(
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

  val ersSummaryNilReturnWithoutAltAmmends: ErsSummary = ErsSummary(
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
