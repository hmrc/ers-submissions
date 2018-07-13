/*
 * Copyright 2018 HM Revenue & Customs
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
import scala.collection.mutable.ListBuffer

object OTHER {

  val otherSchemeType: String = "OTHER"
  val schemeRef: String = "XA1100000000000"
  val timestamp: DateTime = DateTime.now

  val schemeInfo: SchemeInfo = SchemeInfo (
    schemeRef = "XA1100000000000",
    timestamp = timestamp,
    schemeId = "123PA12345678",
    taxYear = "2014/15",
    schemeName = "My scheme",
    schemeType = otherSchemeType
  )

  def buildGrantedV3(): Seq[String] = Seq(
    "2015-10-10", 
    "10.00",
    "10.1234", 
    "100.00"
  )

  def buildGrantedV3Empty(): Seq[String] = Seq(
    "",
    "",
    "",
    ""
  )
  
  def buildOptionV3(
                      taxAvoidance: String = "yes",
                      optionsExercised: String = "yes",
                      sharesListedOnSE: String = "yes",
                      agreedHMRC: String = "yes",
                      valueReceivedOnRACL: String = "yes"
                        ) : Seq[String] = Seq(
    "2014-08-09",
    taxAvoidance,
    if(taxAvoidance == "yes") "12345678" else "",
    "First",
    "Second",
    "Last",
    "NINO",
    "123/XZ55555555",
    "2014-08-09",
    "Company Name",
    "Company Address 1",
    "Company Address 2",
    "Company Address 3",
    "Company Address 4",
    "Company Country",
    "SR77BS",
    "AC097609",
    "1234567800",
    "123/XZ55555555",
    "Company Name",
    "Company Address 1",
    "Company Address 2",
    "Company Address 3",
    "Company Address 4",
    "Company Country",
    "SR77BS",
    "AC097609",
    "1234567800",
    "123/XZ55555555",
    optionsExercised,
    if(optionsExercised == "yes") "100.00" else "",
    if(optionsExercised == "yes") "10.1234" else "",
    if(optionsExercised == "yes") "10.1234" else "",
    if(optionsExercised == "yes") sharesListedOnSE else "",
    if(optionsExercised == "yes" && sharesListedOnSE == "no") agreedHMRC else "",
    if(optionsExercised == "yes" && sharesListedOnSE == "no" && agreedHMRC == "yes") "aa12345678" else "",
    if(optionsExercised == "yes") "10.1234" else "",
    if(optionsExercised == "no") valueReceivedOnRACL else "",
    if(optionsExercised == "no" && valueReceivedOnRACL == "yes") "10.1234" else "",
    "no",
    "yes",
    "no"
  )

  def buildAquisitionV3(
                         taxAvoidance: String = "yes",
                         sharesPartOfLargestClass: String = "yes",
                         sharesListedOnSE: String = "yes",
                         marketValueAgreedHMRC: String = "yes",
                         hasAnElectionBeenMadeToDisregardRestrictions: String = "yes",
                         artificialReductionInValueOnAcquisition: String = "yes",
                         sharesIssuedUnderAnEmployeeShareholderArrangement: String = "yes"
                         ): Seq[String] = Seq(
    "2014-08-30",
    taxAvoidance,
    if(taxAvoidance == "yes") "12345678" else "",
    "First",
    "Second",
    "Last",
    "NINO",
    "123/XZ55555555",
    "Company Name",
    "Company Address 1",
    "Company Address 2",
    "Company Address 3",
    "Company Address 4",
    "Company Country",
    "SR77BS",
    "AC097609",
    "1234567800",
    "123/XZ55555555",
    "1",
    sharesPartOfLargestClass,
    if(sharesPartOfLargestClass == "yes") sharesListedOnSE else "",
    if(sharesPartOfLargestClass == "yes" && sharesListedOnSE == "no") marketValueAgreedHMRC else "",
    if(sharesPartOfLargestClass == "yes" && sharesListedOnSE == "no" && marketValueAgreedHMRC == "yes") "aa12345678" else "",
    "100.0",
    "2",
    "3",
    "0.6",
    "10.1234",
    "10.1234",
    hasAnElectionBeenMadeToDisregardRestrictions,
    if(hasAnElectionBeenMadeToDisregardRestrictions == "yes") "all" else "",
    "10.1234",
    "10.1234",
    "yes",
    artificialReductionInValueOnAcquisition,
    if(artificialReductionInValueOnAcquisition == "yes") "1" else "",
    sharesIssuedUnderAnEmployeeShareholderArrangement,
    if(sharesIssuedUnderAnEmployeeShareholderArrangement == "yes") "yes" else "",
    "no",
    "yes"
  )

  def buildRestrictedSecuritiesV3(
                                 taxAvoidance: String = "yes",
                                 sharesListedOnSE: String = "yes",
                                 agreedHMRC: String = "yes"
                                   ): Seq[String] = Seq(
    "2015-08-19",
    taxAvoidance,
    if(taxAvoidance == "yes") "12345678" else "",
    "First",
    "Second",
    "Last",
    "NINO",
    "123/XZ55555555",
    "2018-09-12",
    "100.00",
    "10.1234",
    sharesListedOnSE,
    if(sharesListedOnSE == "no") agreedHMRC else "",
    if(sharesListedOnSE == "no" && agreedHMRC == "yes") "aa12345678" else "",
    "2018-09-12",
    "10.1234",
    "10.1234",
    "no",
    "no",
    "no"
  )

  def buildBenefitsV3(taxAvoidance: String = "yes"): Seq[String] = Seq(
    "2015-08-19",
    taxAvoidance,
    if(taxAvoidance == "yes") "12345678" else "",
    "First",
    "Second",
    "Last",
    "NINO",
    "123/XZ55555555",
    "2014-08-09",
    "100.00",
    "10.1234",
    "yes",
    "yes"
  )

  def buildConvertableV3(taxAvoidance: String = "yes"): Seq[String] = Seq(
    "2015-08-19",
    taxAvoidance,
    if(taxAvoidance == "yes") "12345678" else "",
    "First",
    "Second",
    "Last",
    "NINO",
    "123/XZ55555555",
    "2014-08-09",
    "100.00",
    "10.1234",
    "10.1234",
    "yes",
    "yes",
    "yes"
  )

  def buildNotionalV3(taxAvoidance: String = "yes"): Seq[String] = Seq(
    "2015-08-19",
    taxAvoidance,
    if(taxAvoidance == "yes") "12345678" else "",
    "First",
    "Second",
    "Last",
    "NINO",
    "123/XZ55555555",
    "2014-08-09",
    "100.00",
    "10.1234",
    "no",
    "no"
  )

  def buildEnchancementV3(taxAvoidance: String = "yes"): Seq[String] = Seq(
    "2015-08-19",
    taxAvoidance,
    if(taxAvoidance == "yes") "12345678" else "",
    "First",
    "Second",
    "Last",
    "NINO",
    "123/XZ55555555",
    "2014-08-09",
    "100.00",
    "10.1234",
    "10.1234",
    "no",
    "no"
  )

  def buildSoldV3(taxAvoidance: String = "yes"): Seq[String] = Seq(
    "2015-08-19",
    taxAvoidance,
    if(taxAvoidance == "yes") "12345678" else "",
    "First",
    "Second",
    "Last",
    "NINO",
    "123/XZ55555555",
    "100.00",
    "10.1234",
    "10.1234",
    "10.1234",
    "no",
    "no"
  )

  def buildOthersData(sheetName: String, data: ListBuffer[Seq[String]]): SchemeData = SchemeData(
    schemeInfo,
    sheetName,
    None,
    Some(
      data
    )
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
