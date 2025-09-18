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

package utils.Schemes_ADRSubmissionSpec

import com.typesafe.config.Config
import common.ERSEnvelope
import connectors.ADRConnector
import fixtures.{Common, Fixtures, OTHER}
import helpers.ERSTestHelper
import models.{SchemeData, SchemeInfo}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, EitherValues}
import play.api.libs.json._
import play.api.test.FakeRequest
import services.PresubmissionService
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.{ADRSubmission, ConfigUtils, SubmissionCommon}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

class OTHER_ADRSubmissionSpec extends ERSTestHelper with BeforeAndAfter with EitherValues {

  implicit val hc: HeaderCarrier = new HeaderCarrier()
  implicit val request: FakeRequest[JsObject] = FakeRequest().withBody(Fixtures.metadataJson)

  val mockSubmissionCommon: SubmissionCommon = app.injector.instanceOf[SubmissionCommon]
  val mockPresubmissionService: PresubmissionService = mock[PresubmissionService]
  val mockADRconnector: ADRConnector = mock[ADRConnector]
  val mockConfigUtils: ConfigUtils = app.injector.instanceOf[ConfigUtils]

  val mockAdrSubmission: ADRSubmission = new ADRSubmission(
    mockSubmissionCommon,
    mockPresubmissionService,
    mockADRconnector,
    mockConfigUtils
  )

  val mockHttpResponse = HttpResponse(200, "Success")

  before {
    reset(mockPresubmissionService, mockADRconnector)

    when(mockADRconnector.sendDataStream(any(), any())(any[ExecutionContext], any[HeaderCarrier]))
      .thenReturn(ERSEnvelope(Future.successful(mockHttpResponse)))

    when(mockPresubmissionService.compareSheetsNumber(any[Int], any[SchemeInfo]())(any[HeaderCarrier]))
      .thenReturn(ERSEnvelope(Future.successful((true, 0L))))
  }

  "calling generateSubmissionReturn for OTHER" should {

    "create valid json for NilReturn" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())(any())
      ).thenReturn(
        ERSEnvelope(Future.successful(List()))
      )

      val result = await(mockAdrSubmission.generateSubmission(OTHER.metadataNilReturn)(request, hc).value)
      result.value - ("acknowledgementReference") shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"OTHER",
                                                                |"schemeReference":"XA1100000000000",
                                                                |"taxYear":"2014/15",
                                                                |"submissionTimestamp":"2015-05-21T11:12:00",
                                                                |"vendorId":" ",
                                                                |"userType":" ",
                                                                |"credentialId":" ",
                                                                |"submissionType":"EOY-RETURN",
                                                                |"submitter":{
                                                                |"firstName":"",
                                                                |"secondName":"",
                                                                |"surname":"",
                                                                |"address":{
                                                                |"addressLine1":"",
                                                                |"addressLine2":"",
                                                                |"addressLine3":"",
                                                                |"addressLine4":"",
                                                                |"country":"",
                                                                |"postcode":"",
                                                                |"emailAddress":" ",
                                                                |"telephoneNumber":" "
                                                                |}
                                                                |},
                                                                |"submissionReturn":{
                                                                |"submitANilReturn":true,
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)

    }

    "create valid json for not NilReturn without data" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())(any())
      ).thenReturn(
        ERSEnvelope(List())
      )

      val result = await(mockAdrSubmission.generateSubmission(OTHER.metadata)(request, hc).value)
      result.value - ("acknowledgementReference") shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"OTHER",
                                                                |"schemeReference":"XA1100000000000",
                                                                |"taxYear":"2014/15",
                                                                |"submissionTimestamp":"2015-05-21T11:12:00",
                                                                |"vendorId":" ",
                                                                |"userType":" ",
                                                                |"credentialId":" ",
                                                                |"submissionType":"EOY-RETURN",
                                                                |"submitter":{
                                                                |"firstName":"",
                                                                |"secondName":"",
                                                                |"surname":"",
                                                                |"address":{
                                                                |"addressLine1":"",
                                                                |"addressLine2":"",
                                                                |"addressLine3":"",
                                                                |"addressLine4":"",
                                                                |"country":"",
                                                                |"postcode":"",
                                                                |"emailAddress":" ",
                                                                |"telephoneNumber":" "
                                                                |}
                                                                |},
                                                                |"submissionReturn":{
                                                                |"submitANilReturn":false,
                                                                |"participatingCompany":{
                                                                |"participants":[
                                                                |{
                                                                |"companyName":"testCompany",
                                                                |"companyAddress":{
                                                                |"addressLine1":"testAddress1",
                                                                |"addressLine2":"testAddress2",
                                                                |"addressLine3":"testAddress3",
                                                                |"addressLine4":"testAddress4",
                                                                |"country":"United Kingdom",
                                                                |"postcode":"NE1 1AA"
                                                                |},
                                                                |"companyCRN":"1234567890",
                                                                |"companyCTRef":"1234567890"
                                                                |},
                                                                |{
                                                                |"companyName":"testCompany",
                                                                |"companyAddress":{
                                                                |"addressLine1":"testAddress1"
                                                                |}
                                                                |}
                                                                |]
                                                                |},
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)

    }

    "create valid json for not Nil return with all sheets" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())(any())
      ).thenReturn(
        ERSEnvelope(
          List(
            SchemeData(OTHER.schemeInfo, "Other_Grants_V4", None, Some(ListBuffer(OTHER.buildGrantedV4()))),
            SchemeData(OTHER.schemeInfo, "Other_Options_V4", None, Some(ListBuffer(OTHER.buildOptionV4("yes", "yes", "yes", "yes", "yes")))),
            SchemeData(OTHER.schemeInfo, "Other_Acquisition_V4", None, Some(ListBuffer(OTHER.buildAquisitionV4("yes", "yes", "yes", "yes", "yes", "yes", "yes")))),
            SchemeData(OTHER.schemeInfo, "Other_RestrictedSecurities_V4", None, Some(ListBuffer(OTHER.buildRestrictedSecuritiesV4("yes", "yes", "no")))),
            SchemeData(OTHER.schemeInfo, "Other_OtherBenefits_V4", None, Some(ListBuffer(OTHER.buildBenefitsV4("yes")))),
            SchemeData(OTHER.schemeInfo, "Other_Convertible_V4", None, Some(ListBuffer(OTHER.buildConvertableV4("yes")))),
            SchemeData(OTHER.schemeInfo, "Other_Notional_V4", None, Some(ListBuffer(OTHER.buildNotionalV4("yes")))),
            SchemeData(OTHER.schemeInfo, "Other_Enhancement_V4", None, Some(ListBuffer(OTHER.buildEnchancementV4("yes")))),
            SchemeData(OTHER.schemeInfo, "Other_Sold_V4", None, Some(ListBuffer(OTHER.buildSoldV4("yes"))))
          )
        )
      )

      val result = await(mockAdrSubmission.generateSubmission(OTHER.metadata)(request, hc).value)
      result.value - ("acknowledgementReference") shouldBe Json.parse("""{
                                                                 |"regime":"ERS",
                                                                 |"schemeType":"OTHER",
                                                                 |"schemeReference":"XA1100000000000",
                                                                 |"taxYear":"2014/15",
                                                                 |"submissionTimestamp":"2015-05-21T11:12:00",
                                                                 |"vendorId":" ",
                                                                 |"userType":" ",
                                                                 |"credentialId":" ",
                                                                 |"submissionType":"EOY-RETURN",
                                                                 |"submitter":{
                                                                 |"firstName":"",
                                                                 |"secondName":"",
                                                                 |"surname":"",
                                                                 |"address":{
                                                                 |"addressLine1":"",
                                                                 |"addressLine2":"",
                                                                 |"addressLine3":"",
                                                                 |"addressLine4":"",
                                                                 |"country":"",
                                                                 |"postcode":"",
                                                                 |"emailAddress":" ",
                                                                 |"telephoneNumber":" "
                                                                 |}
                                                                 |},
                                                                 |"submissionReturn":{
                                                                 |"granted":{
                                                                 |"grantEvents":[
                                                                 |{
                                                                 |"dateOfGrant":"2015-10-10",
                                                                 |"numberOfEmployeesGrantedOptions":10,
                                                                 |"umv":10.1234,
                                                                 |"numberOfSharesOverWhichOptionsGranted":100.0
                                                                 |}
                                                                 |]
                                                                 |},
                                                                 |"option":{
                                                                 |"optionEvents":[
                                                                 |{
                                                                 |"dateOfEvent":"2014-08-09",
                                                                 |"inRelationToASchemeWithADOTASRef":true,
                                                                 |"dotasRef":12345678,
                                                                 |"individualOptions":{
                                                                 |"firstName":"First",
                                                                 |"secondName":"Second",
                                                                 |"surname":"Last",
                                                                 |"nino":"NINO",
                                                                 |"payeReference":"123/XZ55555555"
                                                                 |},
                                                                 |"dateOfGrant":"2014-08-09",
                                                                 |"grantorCompany":{
                                                                 |"companyName":"Company Name",
                                                                 |"companyAddress":{
                                                                 |"addressLine1":"Company Address 1",
                                                                 |"addressLine2":"Company Address 2",
                                                                 |"addressLine3":"Company Address 3",
                                                                 |"addressLine4":"Company Address 4",
                                                                 |"country":"Company Country",
                                                                 |"postcode":"SR77BS"
                                                                 |},
                                                                 |"companyCRN":"AC097609",
                                                                 |"companyCTRef":"1234567800",
                                                                 |"companyPAYERef":"123/XZ55555555"
                                                                 |},
                                                                 |"secUOPCompany":{
                                                                 |"companyName":"Company Name",
                                                                 |"companyAddress":{
                                                                 |"addressLine1":"Company Address 1",
                                                                 |"addressLine2":"Company Address 2",
                                                                 |"addressLine3":"Company Address 3",
                                                                 |"addressLine4":"Company Address 4",
                                                                 |"country":"Company Country",
                                                                 |"postcode":"SR77BS"
                                                                 |},
                                                                 |"companyCRN":"AC097609",
                                                                 |"companyCTRef":"1234567800",
                                                                 |"companyPAYERef":"123/XZ55555555"
                                                                 |},
                                                                 |"optionsExercised":true,
                                                                 |"numberOfSecuritiesAcquired":100.0,
                                                                 |"exercisePricePerSecurity":10.1234,
                                                                 |"marketValuePerSecurityAcquired":10.1234,
                                                                 |"sharesListedOnSE":true,
                                                                 |"amountDeductible":10.1234,
                                                                 |"nicsElectionAgreementEnteredInto":false,
                                                                 |"payeOperatedApplied":true,
                                                                 |"adjusmentMadeForUKDuties":false
                                                                 |}
                                                                 |]
                                                                 |},
                                                                 |"acquisition":{
                                                                 |"acquisitionEvents":[
                                                                 |{
                                                                 |"dateOfEvent":"2014-08-30",
                                                                 |"inRelationToASchemeWithADOTASRef":true,
                                                                 |"dotasRef":"12345678",
                                                                 |"individualOptions":{
                                                                 |"firstName":"First",
                                                                 |"secondName":"Second",
                                                                 |"surname":"Last",
                                                                 |"nino":"NINO",
                                                                 |"payeReference":"123/XZ55555555"
                                                                 |},
                                                                 |"secAwdCompany":{
                                                                 |"companyName":"Company Name",
                                                                 |"companyAddress":{
                                                                 |"addressLine1":"Company Address 1",
                                                                 |"addressLine2":"Company Address 2",
                                                                 |"addressLine3":"Company Address 3",
                                                                 |"addressLine4":"Company Address 4",
                                                                 |"country":"Company Country",
                                                                 |"postcode":"SR77BS"
                                                                 |},
                                                                 |"companyCRN":"AC097609",
                                                                 |"companyCTRef":"1234567800",
                                                                 |"companyPAYERef":"123/XZ55555555"
                                                                 |},
                                                                 |"secAwdDescription":"1",
                                                                 |"sharesPartOfLargestClass":true,
                                                                 |"sharesListedOnSE":true,
                                                                 |"numberOfSharesIssued":100.0,
                                                                 |"restrictedUnrestrictedConvertible":"2",
                                                                 |"natureOfRestriction":"3",
                                                                 |"lengthOfTimeOfRestrictionsInYears":0.6,
                                                                 |"actualMarketValuePerShareAtAcquisitionDate":10.1234,
                                                                 |"unrestrictedMarketValuePerShareAtAcquisitionDate":10.1234,
                                                                 |"hasAnElectionBeenMadeToDisregardRestrictions":true,
                                                                 |"allSomeRestrictionsDisregarded":"all",
                                                                 |"marketValuePerShareIgnoringConversionRights":10.1234,
                                                                 |"totalPricePaid":10.1234,
                                                                 |"paidInSterling":true,
                                                                 |"artificialReductionInValueOnAcquisition":true,
                                                                 |"natureOfArtificialReductionByReason":"1",
                                                                 |"sharesIssuedUnderAnEmployeeShareholderArrangement":true,
                                                                 |"totalMarketValueOfShares2000OrMore":true,
                                                                 |"payeOperatedApplied":false,
                                                                 |"adjusmentMadeForUKDuties":true
                                                                 |}
                                                                 |]
                                                                 |},
                                                                 |"postAcquisitionRestricted":{
                                                                 |"paRestricted":[
                                                                 |{
                                                                 |"dateOfEvent":"2015-08-19",
                                                                 |"inRelationToASchemeWithADOTASRef":true,
                                                                 |"dotasRef":12345678,
                                                                 |"individualPAR":{
                                                                 |"firstName":"First",
                                                                 |"secondName":"Second",
                                                                 |"surname":"Last",
                                                                 |"nino":"NINO",
                                                                 |"payeReference":"123/XZ55555555"
                                                                 |},
                                                                 |"dateSecuritiesOriginallyAcquired":"2018-09-12",
                                                                 |"numberOfSecuritiesOriginallyAcquired":100.0,
                                                                 |"totalChargeableAmount":10.1234,
                                                                 |"sharesListedOnSE":true,
                                                                 |"dateOfVariation":"2018-09-12",
                                                                 |"marketValuePerSecurityDirectlyBeforeVariation":10.1234,
                                                                 |"marketValuePerSecuritiesDirectlyAfterVariation":10.1234,
                                                                 |"nicsElectionAgreementEnteredInto":false,
                                                                 |"payeOperatedApplied":false,
                                                                 |"adjusmentMadeForUKDuties":false
                                                                 |}
                                                                 |]
                                                                 |},
                                                                 |"postAcquisitionOther":{
                                                                 |"paOtherEvents":[
                                                                 |{
                                                                 |"dateOfEvent":"2015-08-19",
                                                                 |"inRelationToASchemeWithADOTASRef":true,
                                                                 |"dotasRef":12345678,
                                                                 |"individualPAO":{
                                                                 |"firstName":"First",
                                                                 |"secondName":"Second",
                                                                 |"surname":"Last",
                                                                 |"nino":"NINO",
                                                                 |"payeReference":"123/XZ55555555"
                                                                 |},
                                                                 |"dateSecuritiesOriginallyAcquired":"2014-08-09",
                                                                 |"numberOfSecuritiesOriginallyAcquired":100.0,
                                                                 |"amountOrMarketValueOfTheBenefit":10.1234,
                                                                 |"payeOperatedApplied":true,
                                                                 |"adjusmentMadeForUKDuties":true
                                                                 |}
                                                                 |]
                                                                 |},
                                                                 |"postAcquisitionConvertible":{
                                                                 |"paConvertible":[
                                                                 |{
                                                                 |"dateOfEvent":"2015-08-19",
                                                                 |"inRelationToASchemeWithADOTASRef":true,
                                                                 |"dotasRef":12345678,
                                                                 |"individualPAC":{
                                                                 |"firstName":"First",
                                                                 |"secondName":"Second",
                                                                 |"surname":"Last",
                                                                 |"nino":"NINO",
                                                                 |"payeReference":"123/XZ55555555"
                                                                 |},
                                                                 |"dateSecuritiesOriginallyAcquired":"2014-08-09",
                                                                 |"numberOfSecuritiesOriginallyAcquired":100.0,
                                                                 |"amountOrMarketValueOfTheBenefit":10.1234,
                                                                 |"totalChargeableAmount":10.1234,
                                                                 |"nicsElectionAgreementEnteredInto":true,
                                                                 |"payeOperatedApplied":true,
                                                                 |"adjusmentMadeForUKDuties":true
                                                                 |}
                                                                 |]
                                                                 |},
                                                                 |"postAcquisitionDischarge":{
                                                                 |"paDischarge":[
                                                                 |{
                                                                 |"dateOfEvent":"2015-08-19",
                                                                 |"inRelationToASchemeWithADOTASRef":true,
                                                                 |"dotasRef":12345678,
                                                                 |"individualPAD":{
                                                                 |"firstName":"First",
                                                                 |"secondName":"Second",
                                                                 |"surname":"Last",
                                                                 |"nino":"NINO",
                                                                 |"payeReference":"123/XZ55555555"
                                                                 |},
                                                                 |"dateSecuritiesOriginallyAcquired":"2014-08-09",
                                                                 |"numberOfSecuritiesOriginallyAcquired":100.0,
                                                                 |"amountOfNotionalLoanOutstanding":10.1234,
                                                                 |"payeOperatedApplied":false,
                                                                 |"adjusmentMadeForUKDuties":false
                                                                 |}
                                                                 |]
                                                                 |},
                                                                 |"postAcquisitionArtificial":{
                                                                 |"paArtificial":[
                                                                 |{
                                                                 |"dateOfEvent":"2015-08-19",
                                                                 |"inRelationToASchemeWithADOTASRef":true,
                                                                 |"dotasRef":12345678,
                                                                 |"individualOptionsPAA":{
                                                                 |"firstName":"First",
                                                                 |"secondName":"Second",
                                                                 |"surname":"Last",
                                                                 |"nino":"NINO",
                                                                 |"payeReference":"123/XZ55555555"
                                                                 |},
                                                                 |"dateSecuritiesOriginallyAcquired":"2014-08-09",
                                                                 |"numberOfSecuritiesOriginallyAcquired":100.0,
                                                                 |"totalUMVOn5AprilOrDateOfDisposalIfEarlier":10.1234,
                                                                 |"totalUMVIgnoringArtificialIncreaseOnDateOfTaxableEvent":10.1234,
                                                                 |"payeOperatedApplied":false,
                                                                 |"adjusmentMadeForUKDuties":false
                                                                 |}
                                                                 |]
                                                                 |},
                                                                 |"postAcquisitionSold":{
                                                                 |"paSold":[
                                                                 |{
                                                                 |"dateOfEvent":"2015-08-19",
                                                                 |"inRelationToASchemeWithADOTASRef":true,
                                                                 |"dotasRef":12345678,
                                                                 |"individualOptionsPAS":{
                                                                 |"firstName":"First",
                                                                 |"secondName":"Second",
                                                                 |"surname":"Last",
                                                                 |"nino":"NINO",
                                                                 |"payeReference":"123/XZ55555555"
                                                                 |},
                                                                 |"numberOfSecuritiesOriginallyAcquired":100.0,
                                                                 |"amountReceivedOnDisposal":10.1234,
                                                                 |"totalMarketValueOnDisposal":10.1234,
                                                                 |"expensesIncurred":10.1234,
                                                                 |"payeOperatedApplied":false,
                                                                 |"adjusmentMadeForUKDuties":false
                                                                 |}
                                                                 |]
                                                                 |},
                                                                 |"submitANilReturn":false,
                                                                 |"participatingCompany":{
                                                                 |"participants":[
                                                                 |{
                                                                 |"companyName":"testCompany",
                                                                 |"companyAddress":{
                                                                 |"addressLine1":"testAddress1",
                                                                 |"addressLine2":"testAddress2",
                                                                 |"addressLine3":"testAddress3",
                                                                 |"addressLine4":"testAddress4",
                                                                 |"country":"United Kingdom",
                                                                 |"postcode":"NE1 1AA"
                                                                 |},
                                                                 |"companyCRN":"1234567890",
                                                                 |"companyCTRef":"1234567890"
                                                                 |},
                                                                 |{
                                                                 |"companyName":"testCompany",
                                                                 |"companyAddress":{
                                                                 |"addressLine1":"testAddress1"
                                                                 |}
                                                                 |}
                                                                 |]
                                                                 |},
                                                                 |"declaration":"declaration"
                                                                 |}
                                                                 |}""".stripMargin)
    }

    "create valid json for not Nil return with all sheets from 2 records of sheet data" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())(any())
      ).thenReturn(
        ERSEnvelope(Future.successful(
          List(
            SchemeData(OTHER.schemeInfo, "Other_Grants_V4", None, Some(ListBuffer(OTHER.buildGrantedV4()))),
            SchemeData(OTHER.schemeInfo, "Other_Grants_V4", None, Some(ListBuffer(OTHER.buildGrantedV4()))),
            SchemeData(OTHER.schemeInfo, "Other_Options_V4", None, Some(ListBuffer(OTHER.buildOptionV4("yes", "yes", "yes", "yes", "yes")))),
            SchemeData(OTHER.schemeInfo, "Other_Options_V4", None, Some(ListBuffer(OTHER.buildOptionV4("yes", "yes", "yes", "yes", "yes")))),
            SchemeData(OTHER.schemeInfo, "Other_Acquisition_V4", None, Some(ListBuffer(OTHER.buildAquisitionV4("yes", "yes", "yes", "yes", "yes", "yes", "yes")))),
            SchemeData(OTHER.schemeInfo, "Other_Acquisition_V4", None, Some(ListBuffer(OTHER.buildAquisitionV4("yes", "yes", "yes", "yes", "yes", "yes", "yes")))),
            SchemeData(OTHER.schemeInfo, "Other_RestrictedSecurities_V4", None, Some(ListBuffer(OTHER.buildRestrictedSecuritiesV4("yes", "yes", "no")))),
            SchemeData(OTHER.schemeInfo, "Other_RestrictedSecurities_V4", None, Some(ListBuffer(OTHER.buildRestrictedSecuritiesV4("yes", "yes", "no")))),
            SchemeData(OTHER.schemeInfo, "Other_OtherBenefits_V4", None, Some(ListBuffer(OTHER.buildBenefitsV4("yes")))),
            SchemeData(OTHER.schemeInfo, "Other_OtherBenefits_V4", None, Some(ListBuffer(OTHER.buildBenefitsV4("yes")))),
            SchemeData(OTHER.schemeInfo, "Other_Convertible_V4", None, Some(ListBuffer(OTHER.buildConvertableV4("yes")))),
            SchemeData(OTHER.schemeInfo, "Other_Convertible_V4", None, Some(ListBuffer(OTHER.buildConvertableV4("yes")))),
            SchemeData(OTHER.schemeInfo, "Other_Notional_V4", None, Some(ListBuffer(OTHER.buildNotionalV4("yes")))),
            SchemeData(OTHER.schemeInfo, "Other_Notional_V4", None, Some(ListBuffer(OTHER.buildNotionalV4("yes")))),
            SchemeData(OTHER.schemeInfo, "Other_Enhancement_V4", None, Some(ListBuffer(OTHER.buildEnchancementV4("yes")))),
            SchemeData(OTHER.schemeInfo, "Other_Enhancement_V4", None, Some(ListBuffer(OTHER.buildEnchancementV4("yes")))),
            SchemeData(OTHER.schemeInfo, "Other_Sold_V4", None, Some(ListBuffer(OTHER.buildSoldV4("yes")))),
            SchemeData(OTHER.schemeInfo, "Other_Sold_V4", None, Some(ListBuffer(OTHER.buildSoldV4("yes"))))
          )
        )
      ))

      val result = await(mockAdrSubmission.generateSubmission(OTHER.metadata)(request, hc).value)
      result.value - ("acknowledgementReference") shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"OTHER",
                                                                |"schemeReference":"XA1100000000000",
                                                                |"taxYear":"2014/15",
                                                                |"submissionTimestamp":"2015-05-21T11:12:00",
                                                                |"vendorId":" ",
                                                                |"userType":" ",
                                                                |"credentialId":" ",
                                                                |"submissionType":"EOY-RETURN",
                                                                |"submitter":{
                                                                |"firstName":"",
                                                                |"secondName":"",
                                                                |"surname":"",
                                                                |"address":{
                                                                |"addressLine1":"",
                                                                |"addressLine2":"",
                                                                |"addressLine3":"",
                                                                |"addressLine4":"",
                                                                |"country":"",
                                                                |"postcode":"",
                                                                |"emailAddress":" ",
                                                                |"telephoneNumber":" "
                                                                |}
                                                                |},
                                                                |"submissionReturn":{
                                                                |"granted":{
                                                                |"grantEvents":[
                                                                |{
                                                                |"dateOfGrant":"2015-10-10",
                                                                |"numberOfEmployeesGrantedOptions":10,
                                                                |"umv":10.1234,
                                                                |"numberOfSharesOverWhichOptionsGranted":100.0
                                                                |},
                                                                |{
                                                                |"dateOfGrant":"2015-10-10",
                                                                |"numberOfEmployeesGrantedOptions":10,
                                                                |"umv":10.1234,
                                                                |"numberOfSharesOverWhichOptionsGranted":100.0
                                                                |}
                                                                |]
                                                                |},
                                                                |"option":{
                                                                |"optionEvents":[
                                                                |{
                                                                |"dateOfEvent":"2014-08-09",
                                                                |"inRelationToASchemeWithADOTASRef":true,
                                                                |"dotasRef":12345678,
                                                                |"individualOptions":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"dateOfGrant":"2014-08-09",
                                                                |"grantorCompany":{
                                                                |"companyName":"Company Name",
                                                                |"companyAddress":{
                                                                |"addressLine1":"Company Address 1",
                                                                |"addressLine2":"Company Address 2",
                                                                |"addressLine3":"Company Address 3",
                                                                |"addressLine4":"Company Address 4",
                                                                |"country":"Company Country",
                                                                |"postcode":"SR77BS"
                                                                |},
                                                                |"companyCRN":"AC097609",
                                                                |"companyCTRef":"1234567800",
                                                                |"companyPAYERef":"123/XZ55555555"
                                                                |},
                                                                |"secUOPCompany":{
                                                                |"companyName":"Company Name",
                                                                |"companyAddress":{
                                                                |"addressLine1":"Company Address 1",
                                                                |"addressLine2":"Company Address 2",
                                                                |"addressLine3":"Company Address 3",
                                                                |"addressLine4":"Company Address 4",
                                                                |"country":"Company Country",
                                                                |"postcode":"SR77BS"
                                                                |},
                                                                |"companyCRN":"AC097609",
                                                                |"companyCTRef":"1234567800",
                                                                |"companyPAYERef":"123/XZ55555555"
                                                                |},
                                                                |"optionsExercised":true,
                                                                |"numberOfSecuritiesAcquired":100.0,
                                                                |"exercisePricePerSecurity":10.1234,
                                                                |"marketValuePerSecurityAcquired":10.1234,
                                                                |"sharesListedOnSE":true,
                                                                |"amountDeductible":10.1234,
                                                                |"nicsElectionAgreementEnteredInto":false,
                                                                |"payeOperatedApplied":true,
                                                                |"adjusmentMadeForUKDuties":false
                                                                |},
                                                                |{
                                                                |"dateOfEvent":"2014-08-09",
                                                                |"inRelationToASchemeWithADOTASRef":true,
                                                                |"dotasRef":12345678,
                                                                |"individualOptions":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"dateOfGrant":"2014-08-09",
                                                                |"grantorCompany":{
                                                                |"companyName":"Company Name",
                                                                |"companyAddress":{
                                                                |"addressLine1":"Company Address 1",
                                                                |"addressLine2":"Company Address 2",
                                                                |"addressLine3":"Company Address 3",
                                                                |"addressLine4":"Company Address 4",
                                                                |"country":"Company Country",
                                                                |"postcode":"SR77BS"
                                                                |},
                                                                |"companyCRN":"AC097609",
                                                                |"companyCTRef":"1234567800",
                                                                |"companyPAYERef":"123/XZ55555555"
                                                                |},
                                                                |"secUOPCompany":{
                                                                |"companyName":"Company Name",
                                                                |"companyAddress":{
                                                                |"addressLine1":"Company Address 1",
                                                                |"addressLine2":"Company Address 2",
                                                                |"addressLine3":"Company Address 3",
                                                                |"addressLine4":"Company Address 4",
                                                                |"country":"Company Country",
                                                                |"postcode":"SR77BS"
                                                                |},
                                                                |"companyCRN":"AC097609",
                                                                |"companyCTRef":"1234567800",
                                                                |"companyPAYERef":"123/XZ55555555"
                                                                |},
                                                                |"optionsExercised":true,
                                                                |"numberOfSecuritiesAcquired":100.0,
                                                                |"exercisePricePerSecurity":10.1234,
                                                                |"marketValuePerSecurityAcquired":10.1234,
                                                                |"sharesListedOnSE":true,
                                                                |"amountDeductible":10.1234,
                                                                |"nicsElectionAgreementEnteredInto":false,
                                                                |"payeOperatedApplied":true,
                                                                |"adjusmentMadeForUKDuties":false
                                                                |}
                                                                |]
                                                                |},
                                                                |"acquisition":{
                                                                |"acquisitionEvents":[
                                                                |{
                                                                |"dateOfEvent":"2014-08-30",
                                                                |"inRelationToASchemeWithADOTASRef":true,
                                                                |"dotasRef":"12345678",
                                                                |"individualOptions":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"secAwdCompany":{
                                                                |"companyName":"Company Name",
                                                                |"companyAddress":{
                                                                |"addressLine1":"Company Address 1",
                                                                |"addressLine2":"Company Address 2",
                                                                |"addressLine3":"Company Address 3",
                                                                |"addressLine4":"Company Address 4",
                                                                |"country":"Company Country",
                                                                |"postcode":"SR77BS"
                                                                |},
                                                                |"companyCRN":"AC097609",
                                                                |"companyCTRef":"1234567800",
                                                                |"companyPAYERef":"123/XZ55555555"
                                                                |},
                                                                |"secAwdDescription":"1",
                                                                |"sharesPartOfLargestClass":true,
                                                                |"sharesListedOnSE":true,
                                                                |"numberOfSharesIssued":100.0,
                                                                |"restrictedUnrestrictedConvertible":"2",
                                                                |"natureOfRestriction":"3",
                                                                |"lengthOfTimeOfRestrictionsInYears":0.6,
                                                                |"actualMarketValuePerShareAtAcquisitionDate":10.1234,
                                                                |"unrestrictedMarketValuePerShareAtAcquisitionDate":10.1234,
                                                                |"hasAnElectionBeenMadeToDisregardRestrictions":true,
                                                                |"allSomeRestrictionsDisregarded":"all",
                                                                |"marketValuePerShareIgnoringConversionRights":10.1234,
                                                                |"totalPricePaid":10.1234,
                                                                |"paidInSterling":true,
                                                                |"artificialReductionInValueOnAcquisition":true,
                                                                |"natureOfArtificialReductionByReason":"1",
                                                                |"sharesIssuedUnderAnEmployeeShareholderArrangement":true,
                                                                |"totalMarketValueOfShares2000OrMore":true,
                                                                |"payeOperatedApplied":false,
                                                                |"adjusmentMadeForUKDuties":true
                                                                |},
                                                                |{
                                                                |"dateOfEvent":"2014-08-30",
                                                                |"inRelationToASchemeWithADOTASRef":true,
                                                                |"dotasRef":"12345678",
                                                                |"individualOptions":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"secAwdCompany":{
                                                                |"companyName":"Company Name",
                                                                |"companyAddress":{
                                                                |"addressLine1":"Company Address 1",
                                                                |"addressLine2":"Company Address 2",
                                                                |"addressLine3":"Company Address 3",
                                                                |"addressLine4":"Company Address 4",
                                                                |"country":"Company Country",
                                                                |"postcode":"SR77BS"
                                                                |},
                                                                |"companyCRN":"AC097609",
                                                                |"companyCTRef":"1234567800",
                                                                |"companyPAYERef":"123/XZ55555555"
                                                                |},
                                                                |"secAwdDescription":"1",
                                                                |"sharesPartOfLargestClass":true,
                                                                |"sharesListedOnSE":true,
                                                                |"numberOfSharesIssued":100.0,
                                                                |"restrictedUnrestrictedConvertible":"2",
                                                                |"natureOfRestriction":"3",
                                                                |"lengthOfTimeOfRestrictionsInYears":0.6,
                                                                |"actualMarketValuePerShareAtAcquisitionDate":10.1234,
                                                                |"unrestrictedMarketValuePerShareAtAcquisitionDate":10.1234,
                                                                |"hasAnElectionBeenMadeToDisregardRestrictions":true,
                                                                |"allSomeRestrictionsDisregarded":"all",
                                                                |"marketValuePerShareIgnoringConversionRights":10.1234,
                                                                |"totalPricePaid":10.1234,
                                                                |"paidInSterling":true,
                                                                |"artificialReductionInValueOnAcquisition":true,
                                                                |"natureOfArtificialReductionByReason":"1",
                                                                |"sharesIssuedUnderAnEmployeeShareholderArrangement":true,
                                                                |"totalMarketValueOfShares2000OrMore":true,
                                                                |"payeOperatedApplied":false,
                                                                |"adjusmentMadeForUKDuties":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"postAcquisitionRestricted":{
                                                                |"paRestricted":[
                                                                |{
                                                                |"dateOfEvent":"2015-08-19",
                                                                |"inRelationToASchemeWithADOTASRef":true,
                                                                |"dotasRef":12345678,
                                                                |"individualPAR":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"dateSecuritiesOriginallyAcquired":"2018-09-12",
                                                                |"numberOfSecuritiesOriginallyAcquired":100.0,
                                                                |"totalChargeableAmount":10.1234,
                                                                |"sharesListedOnSE":true,
                                                                |"dateOfVariation":"2018-09-12",
                                                                |"marketValuePerSecurityDirectlyBeforeVariation":10.1234,
                                                                |"marketValuePerSecuritiesDirectlyAfterVariation":10.1234,
                                                                |"nicsElectionAgreementEnteredInto":false,
                                                                |"payeOperatedApplied":false,
                                                                |"adjusmentMadeForUKDuties":false
                                                                |},
                                                                |{
                                                                |"dateOfEvent":"2015-08-19",
                                                                |"inRelationToASchemeWithADOTASRef":true,
                                                                |"dotasRef":12345678,
                                                                |"individualPAR":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"dateSecuritiesOriginallyAcquired":"2018-09-12",
                                                                |"numberOfSecuritiesOriginallyAcquired":100.0,
                                                                |"totalChargeableAmount":10.1234,
                                                                |"sharesListedOnSE":true,
                                                                |"dateOfVariation":"2018-09-12",
                                                                |"marketValuePerSecurityDirectlyBeforeVariation":10.1234,
                                                                |"marketValuePerSecuritiesDirectlyAfterVariation":10.1234,
                                                                |"nicsElectionAgreementEnteredInto":false,
                                                                |"payeOperatedApplied":false,
                                                                |"adjusmentMadeForUKDuties":false
                                                                |}
                                                                |]
                                                                |},
                                                                |"postAcquisitionOther":{
                                                                |"paOtherEvents":[
                                                                |{
                                                                |"dateOfEvent":"2015-08-19",
                                                                |"inRelationToASchemeWithADOTASRef":true,
                                                                |"dotasRef":12345678,
                                                                |"individualPAO":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"dateSecuritiesOriginallyAcquired":"2014-08-09",
                                                                |"numberOfSecuritiesOriginallyAcquired":100.0,
                                                                |"amountOrMarketValueOfTheBenefit":10.1234,
                                                                |"payeOperatedApplied":true,
                                                                |"adjusmentMadeForUKDuties":true
                                                                |},
                                                                |{
                                                                |"dateOfEvent":"2015-08-19",
                                                                |"inRelationToASchemeWithADOTASRef":true,
                                                                |"dotasRef":12345678,
                                                                |"individualPAO":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"dateSecuritiesOriginallyAcquired":"2014-08-09",
                                                                |"numberOfSecuritiesOriginallyAcquired":100.0,
                                                                |"amountOrMarketValueOfTheBenefit":10.1234,
                                                                |"payeOperatedApplied":true,
                                                                |"adjusmentMadeForUKDuties":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"postAcquisitionConvertible":{
                                                                |"paConvertible":[
                                                                |{
                                                                |"dateOfEvent":"2015-08-19",
                                                                |"inRelationToASchemeWithADOTASRef":true,
                                                                |"dotasRef":12345678,
                                                                |"individualPAC":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"dateSecuritiesOriginallyAcquired":"2014-08-09",
                                                                |"numberOfSecuritiesOriginallyAcquired":100.0,
                                                                |"amountOrMarketValueOfTheBenefit":10.1234,
                                                                |"totalChargeableAmount":10.1234,
                                                                |"nicsElectionAgreementEnteredInto":true,
                                                                |"payeOperatedApplied":true,
                                                                |"adjusmentMadeForUKDuties":true
                                                                |},
                                                                |{
                                                                |"dateOfEvent":"2015-08-19",
                                                                |"inRelationToASchemeWithADOTASRef":true,
                                                                |"dotasRef":12345678,
                                                                |"individualPAC":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"dateSecuritiesOriginallyAcquired":"2014-08-09",
                                                                |"numberOfSecuritiesOriginallyAcquired":100.0,
                                                                |"amountOrMarketValueOfTheBenefit":10.1234,
                                                                |"totalChargeableAmount":10.1234,
                                                                |"nicsElectionAgreementEnteredInto":true,
                                                                |"payeOperatedApplied":true,
                                                                |"adjusmentMadeForUKDuties":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"postAcquisitionDischarge":{
                                                                |"paDischarge":[
                                                                |{
                                                                |"dateOfEvent":"2015-08-19",
                                                                |"inRelationToASchemeWithADOTASRef":true,
                                                                |"dotasRef":12345678,
                                                                |"individualPAD":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"dateSecuritiesOriginallyAcquired":"2014-08-09",
                                                                |"numberOfSecuritiesOriginallyAcquired":100.0,
                                                                |"amountOfNotionalLoanOutstanding":10.1234,
                                                                |"payeOperatedApplied":false,
                                                                |"adjusmentMadeForUKDuties":false
                                                                |},
                                                                |{
                                                                |"dateOfEvent":"2015-08-19",
                                                                |"inRelationToASchemeWithADOTASRef":true,
                                                                |"dotasRef":12345678,
                                                                |"individualPAD":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"dateSecuritiesOriginallyAcquired":"2014-08-09",
                                                                |"numberOfSecuritiesOriginallyAcquired":100.0,
                                                                |"amountOfNotionalLoanOutstanding":10.1234,
                                                                |"payeOperatedApplied":false,
                                                                |"adjusmentMadeForUKDuties":false
                                                                |}
                                                                |]
                                                                |},
                                                                |"postAcquisitionArtificial":{
                                                                |"paArtificial":[
                                                                |{
                                                                |"dateOfEvent":"2015-08-19",
                                                                |"inRelationToASchemeWithADOTASRef":true,
                                                                |"dotasRef":12345678,
                                                                |"individualOptionsPAA":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"dateSecuritiesOriginallyAcquired":"2014-08-09",
                                                                |"numberOfSecuritiesOriginallyAcquired":100.0,
                                                                |"totalUMVOn5AprilOrDateOfDisposalIfEarlier":10.1234,
                                                                |"totalUMVIgnoringArtificialIncreaseOnDateOfTaxableEvent":10.1234,
                                                                |"payeOperatedApplied":false,
                                                                |"adjusmentMadeForUKDuties":false
                                                                |},
                                                                |{
                                                                |"dateOfEvent":"2015-08-19",
                                                                |"inRelationToASchemeWithADOTASRef":true,
                                                                |"dotasRef":12345678,
                                                                |"individualOptionsPAA":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"dateSecuritiesOriginallyAcquired":"2014-08-09",
                                                                |"numberOfSecuritiesOriginallyAcquired":100.0,
                                                                |"totalUMVOn5AprilOrDateOfDisposalIfEarlier":10.1234,
                                                                |"totalUMVIgnoringArtificialIncreaseOnDateOfTaxableEvent":10.1234,
                                                                |"payeOperatedApplied":false,
                                                                |"adjusmentMadeForUKDuties":false
                                                                |}
                                                                |]
                                                                |},
                                                                |"postAcquisitionSold":{
                                                                |"paSold":[
                                                                |{
                                                                |"dateOfEvent":"2015-08-19",
                                                                |"inRelationToASchemeWithADOTASRef":true,
                                                                |"dotasRef":12345678,
                                                                |"individualOptionsPAS":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfSecuritiesOriginallyAcquired":100.0,
                                                                |"amountReceivedOnDisposal":10.1234,
                                                                |"totalMarketValueOnDisposal":10.1234,
                                                                |"expensesIncurred":10.1234,
                                                                |"payeOperatedApplied":false,
                                                                |"adjusmentMadeForUKDuties":false
                                                                |},
                                                                |{
                                                                |"dateOfEvent":"2015-08-19",
                                                                |"inRelationToASchemeWithADOTASRef":true,
                                                                |"dotasRef":12345678,
                                                                |"individualOptionsPAS":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfSecuritiesOriginallyAcquired":100.0,
                                                                |"amountReceivedOnDisposal":10.1234,
                                                                |"totalMarketValueOnDisposal":10.1234,
                                                                |"expensesIncurred":10.1234,
                                                                |"payeOperatedApplied":false,
                                                                |"adjusmentMadeForUKDuties":false
                                                                |}
                                                                |]
                                                                |},
                                                                |"submitANilReturn":false,
                                                                |"participatingCompany":{
                                                                |"participants":[
                                                                |{
                                                                |"companyName":"testCompany",
                                                                |"companyAddress":{
                                                                |"addressLine1":"testAddress1",
                                                                |"addressLine2":"testAddress2",
                                                                |"addressLine3":"testAddress3",
                                                                |"addressLine4":"testAddress4",
                                                                |"country":"United Kingdom",
                                                                |"postcode":"NE1 1AA"
                                                                |},
                                                                |"companyCRN":"1234567890",
                                                                |"companyCTRef":"1234567890"
                                                                |},
                                                                |{
                                                                |"companyName":"testCompany",
                                                                |"companyAddress":{
                                                                |"addressLine1":"testAddress1"
                                                                |}
                                                                |}
                                                                |]
                                                                |},
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

  }

  "calling generateJson for OTHER" should {

    // Granted V4

    "create valid JSON for empty Granted V4" in {

      val sheetName: String = "Other_Grants_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildGrantedV4Empty()
        )
      )

      result shouldBe Json.obj()

    }

    "create valid JSON for Granted V4" in {

      val sheetName: String = "Other_Grants_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildGrantedV4()
        )
      )

      result shouldBe Json.parse("""{
                                  |"granted":{
                                  |"grantEvents":[
                                  |{
                                  |"dateOfGrant":"2015-10-10",
                                  |"numberOfEmployeesGrantedOptions":10.0,
                                  |"umv":10.1234,
                                  |"numberOfSharesOverWhichOptionsGranted":100.0
                                  |}
                                  |]
                                  |}
                                  |}""".stripMargin)

    }

    // Options V4
    "create valid JSON for Options V4 with given TaxAvoidance = yes, optionsExercised = yes, sharesListedOnSE = yes, agreedHMRC = yes, valueReceivedOnRACL = yes" in {

      val sheetName: String = "Other_Options_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildOptionV4(
            "yes", "yes", "yes", "yes", "yes"
          )
        )
      )

      result shouldBe Json.parse("""{
                                   |"option":{
                                   |"optionEvents":[
                                   |{
                                   |"dateOfEvent":"2014-08-09",
                                   |"inRelationToASchemeWithADOTASRef":true,
                                   |"dotasRef":12345678,
                                   |"individualOptions":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateOfGrant":"2014-08-09",
                                   |"grantorCompany":{
                                   |"companyName":"Company Name",
                                   |"companyAddress":{
                                   |"addressLine1":"Company Address 1",
                                   |"addressLine2":"Company Address 2",
                                   |"addressLine3":"Company Address 3",
                                   |"addressLine4":"Company Address 4",
                                   |"country":"Company Country",
                                   |"postcode":"SR77BS"
                                   |},
                                   |"companyCRN":"AC097609",
                                   |"companyCTRef":"1234567800",
                                   |"companyPAYERef":"123/XZ55555555"
                                   |},
                                   |"secUOPCompany":{
                                   |"companyName":"Company Name",
                                   |"companyAddress":{
                                   |"addressLine1":"Company Address 1",
                                   |"addressLine2":"Company Address 2",
                                   |"addressLine3":"Company Address 3",
                                   |"addressLine4":"Company Address 4",
                                   |"country":"Company Country",
                                   |"postcode":"SR77BS"
                                   |},
                                   |"companyCRN":"AC097609",
                                   |"companyCTRef":"1234567800",
                                   |"companyPAYERef":"123/XZ55555555"
                                   |},
                                   |"optionsExercised":true,
                                   |"numberOfSecuritiesAcquired":100.0,
                                   |"exercisePricePerSecurity":10.1234,
                                   |"marketValuePerSecurityAcquired":10.1234,
                                   |"sharesListedOnSE":true,
                                   |"amountDeductible":10.1234,
                                   |"nicsElectionAgreementEnteredInto":false,
                                   |"payeOperatedApplied":true,
                                   |"adjusmentMadeForUKDuties":false
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON for Options V4 with given TaxAvoidance = no, optionsExercised = yes, sharesListedOnSE = yes, agreedHMRC = yes, valueReceivedOnRACL = yes" in {

      val sheetName: String = "Other_Options_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildOptionV4(
            "no", "yes", "yes", "yes", "yes"
          )
        )
      )

      result shouldBe Json.parse("""{
                                   |"option":{
                                   |"optionEvents":[
                                   |{
                                   |"dateOfEvent":"2014-08-09",
                                   |"inRelationToASchemeWithADOTASRef":false,
                                   |"individualOptions":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateOfGrant":"2014-08-09",
                                   |"grantorCompany":{
                                   |"companyName":"Company Name",
                                   |"companyAddress":{
                                   |"addressLine1":"Company Address 1",
                                   |"addressLine2":"Company Address 2",
                                   |"addressLine3":"Company Address 3",
                                   |"addressLine4":"Company Address 4",
                                   |"country":"Company Country",
                                   |"postcode":"SR77BS"
                                   |},
                                   |"companyCRN":"AC097609",
                                   |"companyCTRef":"1234567800",
                                   |"companyPAYERef":"123/XZ55555555"
                                   |},
                                   |"secUOPCompany":{
                                   |"companyName":"Company Name",
                                   |"companyAddress":{
                                   |"addressLine1":"Company Address 1",
                                   |"addressLine2":"Company Address 2",
                                   |"addressLine3":"Company Address 3",
                                   |"addressLine4":"Company Address 4",
                                   |"country":"Company Country",
                                   |"postcode":"SR77BS"
                                   |},
                                   |"companyCRN":"AC097609",
                                   |"companyCTRef":"1234567800",
                                   |"companyPAYERef":"123/XZ55555555"
                                   |},
                                   |"optionsExercised":true,
                                   |"numberOfSecuritiesAcquired":100.0,
                                   |"exercisePricePerSecurity":10.1234,
                                   |"marketValuePerSecurityAcquired":10.1234,
                                   |"sharesListedOnSE":true,
                                   |"amountDeductible":10.1234,
                                   |"nicsElectionAgreementEnteredInto":false,
                                   |"payeOperatedApplied":true,
                                   |"adjusmentMadeForUKDuties":false
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON for Options V4 with given TaxAvoidance = no, optionsExercised = yes, sharesListedOnSE = no, agreedHMRC = yes, valueReceivedOnRACL = yes" in {

      val sheetName: String = "Other_Options_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildOptionV4(
            "no", "yes", "no", "yes", "yes"
          )
        )
      )

      result shouldBe Json.parse("""{
                                   |"option":{
                                   |"optionEvents":[
                                   |{
                                   |"dateOfEvent":"2014-08-09",
                                   |"inRelationToASchemeWithADOTASRef":false,
                                   |"individualOptions":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateOfGrant":"2014-08-09",
                                   |"grantorCompany":{
                                   |"companyName":"Company Name",
                                   |"companyAddress":{
                                   |"addressLine1":"Company Address 1",
                                   |"addressLine2":"Company Address 2",
                                   |"addressLine3":"Company Address 3",
                                   |"addressLine4":"Company Address 4",
                                   |"country":"Company Country",
                                   |"postcode":"SR77BS"
                                   |},
                                   |"companyCRN":"AC097609",
                                   |"companyCTRef":"1234567800",
                                   |"companyPAYERef":"123/XZ55555555"
                                   |},
                                   |"secUOPCompany":{
                                   |"companyName":"Company Name",
                                   |"companyAddress":{
                                   |"addressLine1":"Company Address 1",
                                   |"addressLine2":"Company Address 2",
                                   |"addressLine3":"Company Address 3",
                                   |"addressLine4":"Company Address 4",
                                   |"country":"Company Country",
                                   |"postcode":"SR77BS"
                                   |},
                                   |"companyCRN":"AC097609",
                                   |"companyCTRef":"1234567800",
                                   |"companyPAYERef":"123/XZ55555555"
                                   |},
                                   |"optionsExercised":true,
                                   |"numberOfSecuritiesAcquired":100.0,
                                   |"exercisePricePerSecurity":10.1234,
                                   |"marketValuePerSecurityAcquired":10.1234,
                                   |"sharesListedOnSE":false,
                                   |"marketValueAgreedHMRC": true,
                                   |"hmrcRef": "aa12345678",
                                   |"amountDeductible":10.1234,
                                   |"nicsElectionAgreementEnteredInto":false,
                                   |"payeOperatedApplied":true,
                                   |"adjusmentMadeForUKDuties":false
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON for Options V4 with given TaxAvoidance = no, optionsExercised = yes, sharesListedOnSE = no, agreedHMRC = no, valueReceivedOnRACL = yes" in {

      val sheetName: String = "Other_Options_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildOptionV4(
            "no", "yes", "no", "no", "yes"
          )
        )
      )

      result shouldBe Json.parse("""{
                                   |"option":{
                                   |"optionEvents":[
                                   |{
                                   |"dateOfEvent":"2014-08-09",
                                   |"inRelationToASchemeWithADOTASRef":false,
                                   |"individualOptions":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateOfGrant":"2014-08-09",
                                   |"grantorCompany":{
                                   |"companyName":"Company Name",
                                   |"companyAddress":{
                                   |"addressLine1":"Company Address 1",
                                   |"addressLine2":"Company Address 2",
                                   |"addressLine3":"Company Address 3",
                                   |"addressLine4":"Company Address 4",
                                   |"country":"Company Country",
                                   |"postcode":"SR77BS"
                                   |},
                                   |"companyCRN":"AC097609",
                                   |"companyCTRef":"1234567800",
                                   |"companyPAYERef":"123/XZ55555555"
                                   |},
                                   |"secUOPCompany":{
                                   |"companyName":"Company Name",
                                   |"companyAddress":{
                                   |"addressLine1":"Company Address 1",
                                   |"addressLine2":"Company Address 2",
                                   |"addressLine3":"Company Address 3",
                                   |"addressLine4":"Company Address 4",
                                   |"country":"Company Country",
                                   |"postcode":"SR77BS"
                                   |},
                                   |"companyCRN":"AC097609",
                                   |"companyCTRef":"1234567800",
                                   |"companyPAYERef":"123/XZ55555555"
                                   |},
                                   |"optionsExercised":true,
                                   |"numberOfSecuritiesAcquired":100.0,
                                   |"exercisePricePerSecurity":10.1234,
                                   |"marketValuePerSecurityAcquired":10.1234,
                                   |"sharesListedOnSE":false,
                                   |"marketValueAgreedHMRC": false,
                                   |"amountDeductible":10.1234,
                                   |"nicsElectionAgreementEnteredInto":false,
                                   |"payeOperatedApplied":true,
                                   |"adjusmentMadeForUKDuties":false
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON for Options V4 with given TaxAvoidance = no, optionsExercised = yes, sharesListedOnSE = no, agreedHMRC = no, valueReceivedOnRACL = no" in {

      val sheetName: String = "Other_Options_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildOptionV4(
            "no", "yes", "no", "no", "no"
          )
        )
      )

      result shouldBe Json.parse("""{
                                   |"option":{
                                   |"optionEvents":[
                                   |{
                                   |"dateOfEvent":"2014-08-09",
                                   |"inRelationToASchemeWithADOTASRef":false,
                                   |"individualOptions":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateOfGrant":"2014-08-09",
                                   |"grantorCompany":{
                                   |"companyName":"Company Name",
                                   |"companyAddress":{
                                   |"addressLine1":"Company Address 1",
                                   |"addressLine2":"Company Address 2",
                                   |"addressLine3":"Company Address 3",
                                   |"addressLine4":"Company Address 4",
                                   |"country":"Company Country",
                                   |"postcode":"SR77BS"
                                   |},
                                   |"companyCRN":"AC097609",
                                   |"companyCTRef":"1234567800",
                                   |"companyPAYERef":"123/XZ55555555"
                                   |},
                                   |"secUOPCompany":{
                                   |"companyName":"Company Name",
                                   |"companyAddress":{
                                   |"addressLine1":"Company Address 1",
                                   |"addressLine2":"Company Address 2",
                                   |"addressLine3":"Company Address 3",
                                   |"addressLine4":"Company Address 4",
                                   |"country":"Company Country",
                                   |"postcode":"SR77BS"
                                   |},
                                   |"companyCRN":"AC097609",
                                   |"companyCTRef":"1234567800",
                                   |"companyPAYERef":"123/XZ55555555"
                                   |},
                                   |"optionsExercised":true,
                                   |"numberOfSecuritiesAcquired":100.0,
                                   |"exercisePricePerSecurity":10.1234,
                                   |"marketValuePerSecurityAcquired":10.1234,
                                   |"sharesListedOnSE":false,
                                   |"marketValueAgreedHMRC": false,
                                   |"amountDeductible":10.1234,
                                   |"nicsElectionAgreementEnteredInto":false,
                                   |"payeOperatedApplied":true,
                                   |"adjusmentMadeForUKDuties":false
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON for Options V4 with given TaxAvoidance = no, optionsExercised = no, sharesListedOnSE = no, agreedHMRC = no, valueReceivedOnRACL = yes" in {

      val sheetName: String = "Other_Options_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildOptionV4(
            "no", "no", "no", "no", "yes"
          )
        )
      )

      result shouldBe Json.parse("""{
                                   |"option":{
                                   |"optionEvents":[
                                   |{
                                   |"dateOfEvent":"2014-08-09",
                                   |"inRelationToASchemeWithADOTASRef":false,
                                   |"individualOptions":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateOfGrant":"2014-08-09",
                                   |"grantorCompany":{
                                   |"companyName":"Company Name",
                                   |"companyAddress":{
                                   |"addressLine1":"Company Address 1",
                                   |"addressLine2":"Company Address 2",
                                   |"addressLine3":"Company Address 3",
                                   |"addressLine4":"Company Address 4",
                                   |"country":"Company Country",
                                   |"postcode":"SR77BS"
                                   |},
                                   |"companyCRN":"AC097609",
                                   |"companyCTRef":"1234567800",
                                   |"companyPAYERef":"123/XZ55555555"
                                   |},
                                   |"secUOPCompany":{
                                   |"companyName":"Company Name",
                                   |"companyAddress":{
                                   |"addressLine1":"Company Address 1",
                                   |"addressLine2":"Company Address 2",
                                   |"addressLine3":"Company Address 3",
                                   |"addressLine4":"Company Address 4",
                                   |"country":"Company Country",
                                   |"postcode":"SR77BS"
                                   |},
                                   |"companyCRN":"AC097609",
                                   |"companyCTRef":"1234567800",
                                   |"companyPAYERef":"123/XZ55555555"
                                   |},
                                   |"optionsExercised":false,
                                   |"valueReceivedOnRACL": true,
                                   |"amountReceived": 10.1234,
                                   |"nicsElectionAgreementEnteredInto":false,
                                   |"payeOperatedApplied":true,
                                   |"adjusmentMadeForUKDuties":false
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON for Options V4 with given TaxAvoidance = no, optionsExercised = no, sharesListedOnSE = no, agreedHMRC = no, valueReceivedOnRACL = no" in {

      val sheetName: String = "Other_Options_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildOptionV4(
            "no", "no", "no", "no", "no"
          )
        )
      )

      result shouldBe Json.parse("""{
                                   |"option":{
                                   |"optionEvents":[
                                   |{
                                   |"dateOfEvent":"2014-08-09",
                                   |"inRelationToASchemeWithADOTASRef":false,
                                   |"individualOptions":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateOfGrant":"2014-08-09",
                                   |"grantorCompany":{
                                   |"companyName":"Company Name",
                                   |"companyAddress":{
                                   |"addressLine1":"Company Address 1",
                                   |"addressLine2":"Company Address 2",
                                   |"addressLine3":"Company Address 3",
                                   |"addressLine4":"Company Address 4",
                                   |"country":"Company Country",
                                   |"postcode":"SR77BS"
                                   |},
                                   |"companyCRN":"AC097609",
                                   |"companyCTRef":"1234567800",
                                   |"companyPAYERef":"123/XZ55555555"
                                   |},
                                   |"secUOPCompany":{
                                   |"companyName":"Company Name",
                                   |"companyAddress":{
                                   |"addressLine1":"Company Address 1",
                                   |"addressLine2":"Company Address 2",
                                   |"addressLine3":"Company Address 3",
                                   |"addressLine4":"Company Address 4",
                                   |"country":"Company Country",
                                   |"postcode":"SR77BS"
                                   |},
                                   |"companyCRN":"AC097609",
                                   |"companyCTRef":"1234567800",
                                   |"companyPAYERef":"123/XZ55555555"
                                   |},
                                   |"optionsExercised":false,
                                   |"valueReceivedOnRACL": false,
                                   |"nicsElectionAgreementEnteredInto":false,
                                   |"payeOperatedApplied":true,
                                   |"adjusmentMadeForUKDuties":false
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    // Aquisition V4

    "create valid JSON for Aquisition V4 when given taxAvoidance = yes, sharesPartOfLargestClass = yes, sharesListedOnSE = yes, marketValueAgreedHMRC = yes, hasAnElectionBeenMadeToDisregardRestrictions = yes, artificialReductionInValueOnAcquisition = yes, sharesIssuedUnderAnEmployeeShareholderArrangement = yes" in {

      val sheetName: String = "Other_Acquisition_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildAquisitionV4(
            "yes", "yes", "yes", "yes", "yes", "yes", "yes"
          )
        )
      )

      result shouldBe Json.parse("""{
                                   |"acquisition":{
                                   |"acquisitionEvents":[
                                   |{
                                   |"dateOfEvent":"2014-08-30",
                                   |"inRelationToASchemeWithADOTASRef":true,
                                   |"dotasRef":"12345678",
                                   |"individualOptions":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"secAwdCompany":{
                                   |"companyName":"Company Name",
                                   |"companyAddress":{
                                   |"addressLine1":"Company Address 1",
                                   |"addressLine2":"Company Address 2",
                                   |"addressLine3":"Company Address 3",
                                   |"addressLine4":"Company Address 4",
                                   |"country":"Company Country",
                                   |"postcode":"SR77BS"
                                   |},
                                   |"companyCRN":"AC097609",
                                   |"companyCTRef":"1234567800",
                                   |"companyPAYERef":"123/XZ55555555"
                                   |},
                                   |"secAwdDescription":"1",
                                   |"sharesPartOfLargestClass":true,
                                   |"sharesListedOnSE":true,
                                   |"numberOfSharesIssued":100.0,
                                   |"restrictedUnrestrictedConvertible":"2",
                                   |"natureOfRestriction":"3",
                                   |"lengthOfTimeOfRestrictionsInYears":0.6,
                                   |"actualMarketValuePerShareAtAcquisitionDate":10.1234,
                                   |"unrestrictedMarketValuePerShareAtAcquisitionDate":10.1234,
                                   |"hasAnElectionBeenMadeToDisregardRestrictions":true,
                                   |"allSomeRestrictionsDisregarded":"all",
                                   |"marketValuePerShareIgnoringConversionRights":10.1234,
                                   |"totalPricePaid":10.1234,
                                   |"paidInSterling":true,
                                   |"artificialReductionInValueOnAcquisition":true,
                                   |"natureOfArtificialReductionByReason":"1",
                                   |"sharesIssuedUnderAnEmployeeShareholderArrangement":true,
                                   |"totalMarketValueOfShares2000OrMore":true,
                                   |"payeOperatedApplied":false,
                                   |"adjusmentMadeForUKDuties":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON for Aquisition V4 when given taxAvoidance = no, sharesPartOfLargestClass = yes, sharesListedOnSE = yes, marketValueAgreedHMRC = yes, hasAnElectionBeenMadeToDisregardRestrictions = yes, artificialReductionInValueOnAcquisition = yes, sharesIssuedUnderAnEmployeeShareholderArrangement = yes" in {

      val sheetName: String = "Other_Acquisition_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildAquisitionV4(
            "no", "yes", "yes", "yes", "yes", "yes", "yes"
          )
        )
      )

      result shouldBe Json.parse("""{
                                   |"acquisition":{
                                   |"acquisitionEvents":[
                                   |{
                                   |"dateOfEvent":"2014-08-30",
                                   |"inRelationToASchemeWithADOTASRef":false,
                                   |"individualOptions":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"secAwdCompany":{
                                   |"companyName":"Company Name",
                                   |"companyAddress":{
                                   |"addressLine1":"Company Address 1",
                                   |"addressLine2":"Company Address 2",
                                   |"addressLine3":"Company Address 3",
                                   |"addressLine4":"Company Address 4",
                                   |"country":"Company Country",
                                   |"postcode":"SR77BS"
                                   |},
                                   |"companyCRN":"AC097609",
                                   |"companyCTRef":"1234567800",
                                   |"companyPAYERef":"123/XZ55555555"
                                   |},
                                   |"secAwdDescription":"1",
                                   |"sharesPartOfLargestClass":true,
                                   |"sharesListedOnSE":true,
                                   |"numberOfSharesIssued":100.0,
                                   |"restrictedUnrestrictedConvertible":"2",
                                   |"natureOfRestriction":"3",
                                   |"lengthOfTimeOfRestrictionsInYears":0.6,
                                   |"actualMarketValuePerShareAtAcquisitionDate":10.1234,
                                   |"unrestrictedMarketValuePerShareAtAcquisitionDate":10.1234,
                                   |"hasAnElectionBeenMadeToDisregardRestrictions":true,
                                   |"allSomeRestrictionsDisregarded":"all",
                                   |"marketValuePerShareIgnoringConversionRights":10.1234,
                                   |"totalPricePaid":10.1234,
                                   |"paidInSterling":true,
                                   |"artificialReductionInValueOnAcquisition":true,
                                   |"natureOfArtificialReductionByReason":"1",
                                   |"sharesIssuedUnderAnEmployeeShareholderArrangement":true,
                                   |"totalMarketValueOfShares2000OrMore":true,
                                   |"payeOperatedApplied":false,
                                   |"adjusmentMadeForUKDuties":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON for Aquisition V4 when given taxAvoidance = no, sharesPartOfLargestClass = yes, sharesListedOnSE = no, marketValueAgreedHMRC = yes, hasAnElectionBeenMadeToDisregardRestrictions = yes, artificialReductionInValueOnAcquisition = yes, sharesIssuedUnderAnEmployeeShareholderArrangement = yes" in {

      val sheetName: String = "Other_Acquisition_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildAquisitionV4(
            "no", "yes", "no", "yes", "yes", "yes", "yes"
          )
        )
      )

      result shouldBe Json.parse("""{
                                   |"acquisition":{
                                   |"acquisitionEvents":[
                                   |{
                                   |"dateOfEvent":"2014-08-30",
                                   |"inRelationToASchemeWithADOTASRef":false,
                                   |"individualOptions":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"secAwdCompany":{
                                   |"companyName":"Company Name",
                                   |"companyAddress":{
                                   |"addressLine1":"Company Address 1",
                                   |"addressLine2":"Company Address 2",
                                   |"addressLine3":"Company Address 3",
                                   |"addressLine4":"Company Address 4",
                                   |"country":"Company Country",
                                   |"postcode":"SR77BS"
                                   |},
                                   |"companyCRN":"AC097609",
                                   |"companyCTRef":"1234567800",
                                   |"companyPAYERef":"123/XZ55555555"
                                   |},
                                   |"secAwdDescription":"1",
                                   |"sharesPartOfLargestClass":true,
                                   |"sharesListedOnSE":false,
                                   |"marketValueAgreedHMRC": true,
                                   |"hmrcRef": "aa12345678",
                                   |"numberOfSharesIssued":100.0,
                                   |"restrictedUnrestrictedConvertible":"2",
                                   |"natureOfRestriction":"3",
                                   |"lengthOfTimeOfRestrictionsInYears":0.6,
                                   |"actualMarketValuePerShareAtAcquisitionDate":10.1234,
                                   |"unrestrictedMarketValuePerShareAtAcquisitionDate":10.1234,
                                   |"hasAnElectionBeenMadeToDisregardRestrictions":true,
                                   |"allSomeRestrictionsDisregarded":"all",
                                   |"marketValuePerShareIgnoringConversionRights":10.1234,
                                   |"totalPricePaid":10.1234,
                                   |"paidInSterling":true,
                                   |"artificialReductionInValueOnAcquisition":true,
                                   |"natureOfArtificialReductionByReason":"1",
                                   |"sharesIssuedUnderAnEmployeeShareholderArrangement":true,
                                   |"totalMarketValueOfShares2000OrMore":true,
                                   |"payeOperatedApplied":false,
                                   |"adjusmentMadeForUKDuties":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON for Aquisition V4 when given taxAvoidance = no, sharesPartOfLargestClass = yes, sharesListedOnSE = no, marketValueAgreedHMRC = no, hasAnElectionBeenMadeToDisregardRestrictions = yes, artificialReductionInValueOnAcquisition = yes, sharesIssuedUnderAnEmployeeShareholderArrangement = yes" in {

      val sheetName: String = "Other_Acquisition_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildAquisitionV4(
            "no", "yes", "no", "no", "yes", "yes", "yes"
          )
        )
      )

      result shouldBe Json.parse("""{
                                   |"acquisition":{
                                   |"acquisitionEvents":[
                                   |{
                                   |"dateOfEvent":"2014-08-30",
                                   |"inRelationToASchemeWithADOTASRef":false,
                                   |"individualOptions":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"secAwdCompany":{
                                   |"companyName":"Company Name",
                                   |"companyAddress":{
                                   |"addressLine1":"Company Address 1",
                                   |"addressLine2":"Company Address 2",
                                   |"addressLine3":"Company Address 3",
                                   |"addressLine4":"Company Address 4",
                                   |"country":"Company Country",
                                   |"postcode":"SR77BS"
                                   |},
                                   |"companyCRN":"AC097609",
                                   |"companyCTRef":"1234567800",
                                   |"companyPAYERef":"123/XZ55555555"
                                   |},
                                   |"secAwdDescription":"1",
                                   |"sharesPartOfLargestClass":true,
                                   |"sharesListedOnSE":false,
                                   |"marketValueAgreedHMRC": false,
                                   |"numberOfSharesIssued":100.0,
                                   |"restrictedUnrestrictedConvertible":"2",
                                   |"natureOfRestriction":"3",
                                   |"lengthOfTimeOfRestrictionsInYears":0.6,
                                   |"actualMarketValuePerShareAtAcquisitionDate":10.1234,
                                   |"unrestrictedMarketValuePerShareAtAcquisitionDate":10.1234,
                                   |"hasAnElectionBeenMadeToDisregardRestrictions":true,
                                   |"allSomeRestrictionsDisregarded":"all",
                                   |"marketValuePerShareIgnoringConversionRights":10.1234,
                                   |"totalPricePaid":10.1234,
                                   |"paidInSterling":true,
                                   |"artificialReductionInValueOnAcquisition":true,
                                   |"natureOfArtificialReductionByReason":"1",
                                   |"sharesIssuedUnderAnEmployeeShareholderArrangement":true,
                                   |"totalMarketValueOfShares2000OrMore":true,
                                   |"payeOperatedApplied":false,
                                   |"adjusmentMadeForUKDuties":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON for Aquisition V4 when given taxAvoidance = no, sharesPartOfLargestClass = no, sharesListedOnSE = no, marketValueAgreedHMRC = no, hasAnElectionBeenMadeToDisregardRestrictions = yes, artificialReductionInValueOnAcquisition = yes, sharesIssuedUnderAnEmployeeShareholderArrangement = yes" in {

      val sheetName: String = "Other_Acquisition_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildAquisitionV4(
            "no", "no", "no", "no", "yes", "yes", "yes"
          )
        )
      )

      result shouldBe Json.parse("""{
                                   |"acquisition":{
                                   |"acquisitionEvents":[
                                   |{
                                   |"dateOfEvent":"2014-08-30",
                                   |"inRelationToASchemeWithADOTASRef":false,
                                   |"individualOptions":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"secAwdCompany":{
                                   |"companyName":"Company Name",
                                   |"companyAddress":{
                                   |"addressLine1":"Company Address 1",
                                   |"addressLine2":"Company Address 2",
                                   |"addressLine3":"Company Address 3",
                                   |"addressLine4":"Company Address 4",
                                   |"country":"Company Country",
                                   |"postcode":"SR77BS"
                                   |},
                                   |"companyCRN":"AC097609",
                                   |"companyCTRef":"1234567800",
                                   |"companyPAYERef":"123/XZ55555555"
                                   |},
                                   |"secAwdDescription":"1",
                                   |"sharesPartOfLargestClass":false,
                                   |"numberOfSharesIssued":100.0,
                                   |"restrictedUnrestrictedConvertible":"2",
                                   |"natureOfRestriction":"3",
                                   |"lengthOfTimeOfRestrictionsInYears":0.6,
                                   |"actualMarketValuePerShareAtAcquisitionDate":10.1234,
                                   |"unrestrictedMarketValuePerShareAtAcquisitionDate":10.1234,
                                   |"hasAnElectionBeenMadeToDisregardRestrictions":true,
                                   |"allSomeRestrictionsDisregarded":"all",
                                   |"marketValuePerShareIgnoringConversionRights":10.1234,
                                   |"totalPricePaid":10.1234,
                                   |"paidInSterling":true,
                                   |"artificialReductionInValueOnAcquisition":true,
                                   |"natureOfArtificialReductionByReason":"1",
                                   |"sharesIssuedUnderAnEmployeeShareholderArrangement":true,
                                   |"totalMarketValueOfShares2000OrMore":true,
                                   |"payeOperatedApplied":false,
                                   |"adjusmentMadeForUKDuties":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON for Aquisition V4 when given taxAvoidance = no, sharesPartOfLargestClass = no, sharesListedOnSE = no, marketValueAgreedHMRC = no, hasAnElectionBeenMadeToDisregardRestrictions = no, artificialReductionInValueOnAcquisition = yes, sharesIssuedUnderAnEmployeeShareholderArrangement = yes" in {

      val sheetName: String = "Other_Acquisition_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildAquisitionV4(
            "no", "no", "no", "no", "no", "yes", "yes"
          )
        )
      )

      result shouldBe Json.parse("""{
                                   |"acquisition":{
                                   |"acquisitionEvents":[
                                   |{
                                   |"dateOfEvent":"2014-08-30",
                                   |"inRelationToASchemeWithADOTASRef":false,
                                   |"individualOptions":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"secAwdCompany":{
                                   |"companyName":"Company Name",
                                   |"companyAddress":{
                                   |"addressLine1":"Company Address 1",
                                   |"addressLine2":"Company Address 2",
                                   |"addressLine3":"Company Address 3",
                                   |"addressLine4":"Company Address 4",
                                   |"country":"Company Country",
                                   |"postcode":"SR77BS"
                                   |},
                                   |"companyCRN":"AC097609",
                                   |"companyCTRef":"1234567800",
                                   |"companyPAYERef":"123/XZ55555555"
                                   |},
                                   |"secAwdDescription":"1",
                                   |"sharesPartOfLargestClass":false,
                                   |"numberOfSharesIssued":100.0,
                                   |"restrictedUnrestrictedConvertible":"2",
                                   |"natureOfRestriction":"3",
                                   |"lengthOfTimeOfRestrictionsInYears":0.6,
                                   |"actualMarketValuePerShareAtAcquisitionDate":10.1234,
                                   |"unrestrictedMarketValuePerShareAtAcquisitionDate":10.1234,
                                   |"hasAnElectionBeenMadeToDisregardRestrictions":false,
                                   |"marketValuePerShareIgnoringConversionRights":10.1234,
                                   |"totalPricePaid":10.1234,
                                   |"paidInSterling":true,
                                   |"artificialReductionInValueOnAcquisition":true,
                                   |"natureOfArtificialReductionByReason":"1",
                                   |"sharesIssuedUnderAnEmployeeShareholderArrangement":true,
                                   |"totalMarketValueOfShares2000OrMore":true,
                                   |"payeOperatedApplied":false,
                                   |"adjusmentMadeForUKDuties":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON for Aquisition V4 when given taxAvoidance = no, sharesPartOfLargestClass = no, sharesListedOnSE = no, marketValueAgreedHMRC = no, hasAnElectionBeenMadeToDisregardRestrictions = no, artificialReductionInValueOnAcquisition = no, sharesIssuedUnderAnEmployeeShareholderArrangement = yes" in {

      val sheetName: String = "Other_Acquisition_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildAquisitionV4(
            "no", "no", "no", "no", "no", "no", "yes"
          )
        )
      )

      result shouldBe Json.parse("""{
                                   |"acquisition":{
                                   |"acquisitionEvents":[
                                   |{
                                   |"dateOfEvent":"2014-08-30",
                                   |"inRelationToASchemeWithADOTASRef":false,
                                   |"individualOptions":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"secAwdCompany":{
                                   |"companyName":"Company Name",
                                   |"companyAddress":{
                                   |"addressLine1":"Company Address 1",
                                   |"addressLine2":"Company Address 2",
                                   |"addressLine3":"Company Address 3",
                                   |"addressLine4":"Company Address 4",
                                   |"country":"Company Country",
                                   |"postcode":"SR77BS"
                                   |},
                                   |"companyCRN":"AC097609",
                                   |"companyCTRef":"1234567800",
                                   |"companyPAYERef":"123/XZ55555555"
                                   |},
                                   |"secAwdDescription":"1",
                                   |"sharesPartOfLargestClass":false,
                                   |"numberOfSharesIssued":100.0,
                                   |"restrictedUnrestrictedConvertible":"2",
                                   |"natureOfRestriction":"3",
                                   |"lengthOfTimeOfRestrictionsInYears":0.6,
                                   |"actualMarketValuePerShareAtAcquisitionDate":10.1234,
                                   |"unrestrictedMarketValuePerShareAtAcquisitionDate":10.1234,
                                   |"hasAnElectionBeenMadeToDisregardRestrictions":false,
                                   |"marketValuePerShareIgnoringConversionRights":10.1234,
                                   |"totalPricePaid":10.1234,
                                   |"paidInSterling":true,
                                   |"artificialReductionInValueOnAcquisition":false,
                                   |"sharesIssuedUnderAnEmployeeShareholderArrangement":true,
                                   |"totalMarketValueOfShares2000OrMore":true,
                                   |"payeOperatedApplied":false,
                                   |"adjusmentMadeForUKDuties":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON for Aquisition V4 when given taxAvoidance = no, sharesPartOfLargestClass = no, sharesListedOnSE = no, marketValueAgreedHMRC = no, hasAnElectionBeenMadeToDisregardRestrictions = no, artificialReductionInValueOnAcquisition = no, sharesIssuedUnderAnEmployeeShareholderArrangement = no" in {

      val sheetName: String = "Other_Acquisition_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildAquisitionV4(
            "no", "no", "no", "no", "no", "no", "no"
          )
        )
      )

      result shouldBe Json.parse("""{
                                   |"acquisition":{
                                   |"acquisitionEvents":[
                                   |{
                                   |"dateOfEvent":"2014-08-30",
                                   |"inRelationToASchemeWithADOTASRef":false,
                                   |"individualOptions":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"secAwdCompany":{
                                   |"companyName":"Company Name",
                                   |"companyAddress":{
                                   |"addressLine1":"Company Address 1",
                                   |"addressLine2":"Company Address 2",
                                   |"addressLine3":"Company Address 3",
                                   |"addressLine4":"Company Address 4",
                                   |"country":"Company Country",
                                   |"postcode":"SR77BS"
                                   |},
                                   |"companyCRN":"AC097609",
                                   |"companyCTRef":"1234567800",
                                   |"companyPAYERef":"123/XZ55555555"
                                   |},
                                   |"secAwdDescription":"1",
                                   |"sharesPartOfLargestClass":false,
                                   |"numberOfSharesIssued":100.0,
                                   |"restrictedUnrestrictedConvertible":"2",
                                   |"natureOfRestriction":"3",
                                   |"lengthOfTimeOfRestrictionsInYears":0.6,
                                   |"actualMarketValuePerShareAtAcquisitionDate":10.1234,
                                   |"unrestrictedMarketValuePerShareAtAcquisitionDate":10.1234,
                                   |"hasAnElectionBeenMadeToDisregardRestrictions":false,
                                   |"marketValuePerShareIgnoringConversionRights":10.1234,
                                   |"totalPricePaid":10.1234,
                                   |"paidInSterling":true,
                                   |"artificialReductionInValueOnAcquisition":false,
                                   |"sharesIssuedUnderAnEmployeeShareholderArrangement":false,
                                   |"payeOperatedApplied":false,
                                   |"adjusmentMadeForUKDuties":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    // Restricted Securities V4

    "create valid JSON for Restricted Securities V4 if TaxAvoidance and SE are given and Agreed HMRC is not" in {

      val sheetName: String = "Other_RestrictedSecurities_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildRestrictedSecuritiesV4(
            "yes", "yes", "no"
          )
        )
      )

      result shouldBe Json.parse("""{
                                   |"postAcquisitionRestricted":{
                                   |"paRestricted":[
                                   |{
                                   |"dateOfEvent":"2015-08-19",
                                   |"inRelationToASchemeWithADOTASRef":true,
                                   |"dotasRef":12345678,
                                   |"individualPAR":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateSecuritiesOriginallyAcquired":"2018-09-12",
                                   |"numberOfSecuritiesOriginallyAcquired":100.0,
                                   |"totalChargeableAmount":10.1234,
                                   |"sharesListedOnSE":true,
                                   |"dateOfVariation":"2018-09-12",
                                   |"marketValuePerSecurityDirectlyBeforeVariation":10.1234,
                                   |"marketValuePerSecuritiesDirectlyAfterVariation":10.1234,
                                   |"nicsElectionAgreementEnteredInto":false,
                                   |"payeOperatedApplied":false,
                                   |"adjusmentMadeForUKDuties":false
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON for Restricted Securities V4 if TaxAvoidance and SE are not given and Agreed HMRC is given" in {

      val sheetName: String = "Other_RestrictedSecurities_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildRestrictedSecuritiesV4(
            "no", "no", "yes"
          )
        )
      )

      result shouldBe Json.parse("""{
                                   |"postAcquisitionRestricted":{
                                   |"paRestricted":[
                                   |{
                                   |"dateOfEvent":"2015-08-19",
                                   |"inRelationToASchemeWithADOTASRef":false,
                                   |"individualPAR":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateSecuritiesOriginallyAcquired":"2018-09-12",
                                   |"numberOfSecuritiesOriginallyAcquired":100.0,
                                   |"totalChargeableAmount":10.1234,
                                   |"sharesListedOnSE":false,
                                   |"marketValueAgreedHMRC":true,
                                   |"hmrcRef":"aa12345678",
                                   |"dateOfVariation":"2018-09-12",
                                   |"marketValuePerSecurityDirectlyBeforeVariation":10.1234,
                                   |"marketValuePerSecuritiesDirectlyAfterVariation":10.1234,
                                   |"nicsElectionAgreementEnteredInto":false,
                                   |"payeOperatedApplied":false,
                                   |"adjusmentMadeForUKDuties":false
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON for Restricted Securities V4 if TaxAvoidance is given and SE and Agreed HMRC are not" in {

      val sheetName: String = "Other_RestrictedSecurities_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildRestrictedSecuritiesV4(
            "yes", "no", "no"
          )
        )
      )

      result shouldBe Json.parse("""{
                                   |"postAcquisitionRestricted":{
                                   |"paRestricted":[
                                   |{
                                   |"dateOfEvent":"2015-08-19",
                                   |"inRelationToASchemeWithADOTASRef":true,
                                   |"dotasRef":12345678,
                                   |"individualPAR":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateSecuritiesOriginallyAcquired":"2018-09-12",
                                   |"numberOfSecuritiesOriginallyAcquired":100.0,
                                   |"totalChargeableAmount":10.1234,
                                   |"sharesListedOnSE":false,
                                   |"marketValueAgreedHMRC":false,
                                   |"dateOfVariation":"2018-09-12",
                                   |"marketValuePerSecurityDirectlyBeforeVariation":10.1234,
                                   |"marketValuePerSecuritiesDirectlyAfterVariation":10.1234,
                                   |"nicsElectionAgreementEnteredInto":false,
                                   |"payeOperatedApplied":false,
                                   |"adjusmentMadeForUKDuties":false
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON for Restricted Securities V4 with different values" in {

      val sheetName: String = "Other_RestrictedSecurities_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildRestrictedSecuritiesV4("yes", "yes", "no"),
          OTHER.buildRestrictedSecuritiesV4("no", "no", "yes"),
          OTHER.buildRestrictedSecuritiesV4("yes", "no", "no")
        )
      )

      result shouldBe Json.parse("""{
                                   |"postAcquisitionRestricted":{
                                   |"paRestricted":[
                                   |{
                                   |"dateOfEvent":"2015-08-19",
                                   |"inRelationToASchemeWithADOTASRef":true,
                                   |"dotasRef":12345678,
                                   |"individualPAR":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateSecuritiesOriginallyAcquired":"2018-09-12",
                                   |"numberOfSecuritiesOriginallyAcquired":100.0,
                                   |"totalChargeableAmount":10.1234,
                                   |"sharesListedOnSE":true,
                                   |"dateOfVariation":"2018-09-12",
                                   |"marketValuePerSecurityDirectlyBeforeVariation":10.1234,
                                   |"marketValuePerSecuritiesDirectlyAfterVariation":10.1234,
                                   |"nicsElectionAgreementEnteredInto":false,
                                   |"payeOperatedApplied":false,
                                   |"adjusmentMadeForUKDuties":false
                                   |},
                                   |{
                                   |"dateOfEvent":"2015-08-19",
                                   |"inRelationToASchemeWithADOTASRef":false,
                                   |"individualPAR":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateSecuritiesOriginallyAcquired":"2018-09-12",
                                   |"numberOfSecuritiesOriginallyAcquired":100.0,
                                   |"totalChargeableAmount":10.1234,
                                   |"sharesListedOnSE":false,
                                   |"marketValueAgreedHMRC":true,
                                   |"hmrcRef":"aa12345678",
                                   |"dateOfVariation":"2018-09-12",
                                   |"marketValuePerSecurityDirectlyBeforeVariation":10.1234,
                                   |"marketValuePerSecuritiesDirectlyAfterVariation":10.1234,
                                   |"nicsElectionAgreementEnteredInto":false,
                                   |"payeOperatedApplied":false,
                                   |"adjusmentMadeForUKDuties":false
                                   |},
                                   |{
                                   |"dateOfEvent":"2015-08-19",
                                   |"inRelationToASchemeWithADOTASRef":true,
                                   |"dotasRef":12345678,
                                   |"individualPAR":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateSecuritiesOriginallyAcquired":"2018-09-12",
                                   |"numberOfSecuritiesOriginallyAcquired":100.0,
                                   |"totalChargeableAmount":10.1234,
                                   |"sharesListedOnSE":false,
                                   |"marketValueAgreedHMRC":false,
                                   |"dateOfVariation":"2018-09-12",
                                   |"marketValuePerSecurityDirectlyBeforeVariation":10.1234,
                                   |"marketValuePerSecuritiesDirectlyAfterVariation":10.1234,
                                   |"nicsElectionAgreementEnteredInto":false,
                                   |"payeOperatedApplied":false,
                                   |"adjusmentMadeForUKDuties":false
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    // Benefits V4

    "create valid JSON for Benefits V4 if TaxAvoidance = yes" in {

      val sheetName: String = "Other_OtherBenefits_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildBenefitsV4("yes")
        )
      )

      result shouldBe Json.parse("""{
                                   |"postAcquisitionOther":{
                                   |"paOtherEvents":[
                                   |{
                                   |"dateOfEvent":"2015-08-19",
                                   |"inRelationToASchemeWithADOTASRef":true,
                                   |"dotasRef":12345678,
                                   |"individualPAO":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateSecuritiesOriginallyAcquired":"2014-08-09",
                                   |"numberOfSecuritiesOriginallyAcquired":100.0,
                                   |"amountOrMarketValueOfTheBenefit":10.1234,
                                   |"payeOperatedApplied":true,
                                   |"adjusmentMadeForUKDuties":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)

    }

    "create valid JSON for Benefits V4 if TaxAvoidance = no" in {

      val sheetName: String = "Other_OtherBenefits_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildBenefitsV4("no")
        )
      )

      result shouldBe Json.parse("""{
                                   |"postAcquisitionOther":{
                                   |"paOtherEvents":[
                                   |{
                                   |"dateOfEvent":"2015-08-19",
                                   |"inRelationToASchemeWithADOTASRef":false,
                                   |"individualPAO":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateSecuritiesOriginallyAcquired":"2014-08-09",
                                   |"numberOfSecuritiesOriginallyAcquired":100.0,
                                   |"amountOrMarketValueOfTheBenefit":10.1234,
                                   |"payeOperatedApplied":true,
                                   |"adjusmentMadeForUKDuties":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)

    }

    // Convertable V4

    "create valid JSON for Convertable V4 if TaxAvoidance = yes" in {

      val sheetName: String = "Other_Convertible_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildConvertableV4("yes")
        )
      )

      result shouldBe Json.parse("""{
                                   |"postAcquisitionConvertible":{
                                   |"paConvertible":[
                                   |{
                                   |"dateOfEvent":"2015-08-19",
                                   |"inRelationToASchemeWithADOTASRef":true,
                                   |"dotasRef":12345678,
                                   |"individualPAC":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateSecuritiesOriginallyAcquired":"2014-08-09",
                                   |"numberOfSecuritiesOriginallyAcquired":100.0,
                                   |"amountOrMarketValueOfTheBenefit":10.1234,
                                   |"totalChargeableAmount":10.1234,
                                   |"nicsElectionAgreementEnteredInto":true,
                                   |"payeOperatedApplied":true,
                                   |"adjusmentMadeForUKDuties":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)

    }

    "create valid JSON for Convertable V4 if TaxAvoidance = no" in {

      val sheetName: String = "Other_Convertible_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildConvertableV4("no")
        )
      )

      result shouldBe Json.parse("""{
                                   |"postAcquisitionConvertible":{
                                   |"paConvertible":[
                                   |{
                                   |"dateOfEvent":"2015-08-19",
                                   |"inRelationToASchemeWithADOTASRef":false,
                                   |"individualPAC":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateSecuritiesOriginallyAcquired":"2014-08-09",
                                   |"numberOfSecuritiesOriginallyAcquired":100.0,
                                   |"amountOrMarketValueOfTheBenefit":10.1234,
                                   |"totalChargeableAmount":10.1234,
                                   |"nicsElectionAgreementEnteredInto":true,
                                   |"payeOperatedApplied":true,
                                   |"adjusmentMadeForUKDuties":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)

    }

    // Notional V4

    "create valid JSON for Notional V4 if TaxAvoidance = yes" in {

      val sheetName: String = "Other_Notional_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildNotionalV4("yes")
        )
      )

      result shouldBe Json.parse("""{
                                   |"postAcquisitionDischarge":{
                                   |"paDischarge":[
                                   |{
                                   |"dateOfEvent":"2015-08-19",
                                   |"inRelationToASchemeWithADOTASRef":true,
                                   |"dotasRef":12345678,
                                   |"individualPAD":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateSecuritiesOriginallyAcquired":"2014-08-09",
                                   |"numberOfSecuritiesOriginallyAcquired":100.0,
                                   |"amountOfNotionalLoanOutstanding":10.1234,
                                   |"payeOperatedApplied":false,
                                   |"adjusmentMadeForUKDuties":false
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)

    }

    "create valid JSON for Notional V4 if TaxAvoidance = no" in {

      val sheetName: String = "Other_Notional_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildNotionalV4("no")
        )
      )

      result shouldBe Json.parse("""{
                                   |"postAcquisitionDischarge":{
                                   |"paDischarge":[
                                   |{
                                   |"dateOfEvent":"2015-08-19",
                                   |"inRelationToASchemeWithADOTASRef":false,
                                   |"individualPAD":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateSecuritiesOriginallyAcquired":"2014-08-09",
                                   |"numberOfSecuritiesOriginallyAcquired":100.0,
                                   |"amountOfNotionalLoanOutstanding":10.1234,
                                   |"payeOperatedApplied":false,
                                   |"adjusmentMadeForUKDuties":false
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)

    }

    // Enchancement V4

    "create valid JSON for Enchancement V4 if TaxAvoidance = yes" in {

      val sheetName: String = "Other_Enhancement_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildEnchancementV4("yes")
        )
      )

      result shouldBe Json.parse("""{
                                   |"postAcquisitionArtificial":{
                                   |"paArtificial":[
                                   |{
                                   |"dateOfEvent":"2015-08-19",
                                   |"inRelationToASchemeWithADOTASRef":true,
                                   |"dotasRef":12345678,
                                   |"individualOptionsPAA":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateSecuritiesOriginallyAcquired":"2014-08-09",
                                   |"numberOfSecuritiesOriginallyAcquired":100.0,
                                   |"totalUMVOn5AprilOrDateOfDisposalIfEarlier":10.1234,
                                   |"totalUMVIgnoringArtificialIncreaseOnDateOfTaxableEvent":10.1234,
                                   |"payeOperatedApplied":false,
                                   |"adjusmentMadeForUKDuties":false
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)

    }

    "create valid JSON for Enchancement V4 if TaxAvoidance = no" in {

      val sheetName: String = "Other_Enhancement_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildEnchancementV4("no")
        )
      )

      result shouldBe Json.parse("""{
                                   |"postAcquisitionArtificial":{
                                   |"paArtificial":[
                                   |{
                                   |"dateOfEvent":"2015-08-19",
                                   |"inRelationToASchemeWithADOTASRef":false,
                                   |"individualOptionsPAA":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateSecuritiesOriginallyAcquired":"2014-08-09",
                                   |"numberOfSecuritiesOriginallyAcquired":100.0,
                                   |"totalUMVOn5AprilOrDateOfDisposalIfEarlier":10.1234,
                                   |"totalUMVIgnoringArtificialIncreaseOnDateOfTaxableEvent":10.1234,
                                   |"payeOperatedApplied":false,
                                   |"adjusmentMadeForUKDuties":false
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)

    }

    // Sold V4

    "create valid JSON for Sold V4 if TaxAvoidance = yes" in {

      val sheetName: String = "Other_Sold_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildSoldV4("yes")
        )
      )

      result shouldBe Json.parse("""{
                                   |"postAcquisitionSold":{
                                   |"paSold":[
                                   |{
                                   |"dateOfEvent":"2015-08-19",
                                   |"inRelationToASchemeWithADOTASRef":true,
                                   |"dotasRef":12345678,
                                   |"individualOptionsPAS":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfSecuritiesOriginallyAcquired":100.0,
                                   |"amountReceivedOnDisposal":10.1234,
                                   |"totalMarketValueOnDisposal":10.1234,
                                   |"expensesIncurred":10.1234,
                                   |"payeOperatedApplied":false,
                                   |"adjusmentMadeForUKDuties":false
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)

    }

    "create valid JSON for Sold V4 if TaxAvoidance = no" in {

      val sheetName: String = "Other_Sold_V4"
      val configData: Config = Common.loadConfiguration(OTHER.otherSchemeType, sheetName, mockConfigUtils)

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          OTHER.buildSoldV4("no")
        )
      )

      result shouldBe Json.parse("""{
                                   |"postAcquisitionSold":{
                                   |"paSold":[
                                   |{
                                   |"dateOfEvent":"2015-08-19",
                                   |"inRelationToASchemeWithADOTASRef":false,
                                   |"individualOptionsPAS":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfSecuritiesOriginallyAcquired":100.0,
                                   |"amountReceivedOnDisposal":10.1234,
                                   |"totalMarketValueOnDisposal":10.1234,
                                   |"expensesIncurred":10.1234,
                                   |"payeOperatedApplied":false,
                                   |"adjusmentMadeForUKDuties":false
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)

    }

  }

}
