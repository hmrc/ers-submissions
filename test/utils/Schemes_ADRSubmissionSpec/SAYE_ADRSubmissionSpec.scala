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
import fixtures.{Common, Fixtures, SAYE}
import helpers.ERSTestHelper
import models.{SchemeData, SchemeInfo}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, EitherValues}
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import services.PresubmissionService
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.{ADRSubmission, ConfigUtils, SubmissionCommon}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

class SAYE_ADRSubmissionSpec extends ERSTestHelper with BeforeAndAfter with EitherValues {

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

  "calling generateSubmissionReturn" should {

    "return a valid NilReturn without ammends" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())(any())
      ).thenReturn(
        ERSEnvelope(Future.successful(List()))
      )

      val result = await(mockAdrSubmission.generateSubmission(SAYE.ersSummaryNilReturnWithoutAltAmmends)(request, hc).value)
      result.value - "acknowledgementReference" shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"SAYE",
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
                                                                |"groupPlan":true,
                                                                |"optionsGrantedInYear":false,
                                                                |"optionsExercisedInYear":false,
                                                                |"optionsReleasedCancelledInYear":false,
                                                                |"alterationsAmendmentsMadeInYear":false,
                                                                |"declarationSchedule3":"schedule-3",
                                                                |"numberOfIndividualsGrantedOptions":0,
                                                                |"numberOfContractsWithin5Yr3YrNoBonus":0,
                                                                |"numberOfIndividualsExercisedOptions":0,
                                                                |"numberOfSharesIssuedTransferredOnExerciseOfOptions":0,
                                                                |"numberOfIndividualsSavedMaxLimit":0,
                                                                |"amtSharesPaid":0,
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

    "return a valid NilReturn with some ammends" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())(any())
      ).thenReturn(
        ERSEnvelope(Future.successful(List()))
      )

      val result = await(mockAdrSubmission.generateSubmission(SAYE.ersSummaryNilReturnWithSomeAltAmmends)(request, hc).value)
      result.value - "acknowledgementReference" shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"SAYE",
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
                                                                |"groupPlan":true,
                                                                |"optionsGrantedInYear":false,
                                                                |"optionsExercisedInYear":false,
                                                                |"optionsReleasedCancelledInYear":false,
                                                                |"alterationsAmendmentsMadeInYear":true,
                                                                |"alteration":{
                                                                |"alterationTypes":[
                                                                |{
                                                                |"typeOfAlteration":"first"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"third"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"fifth"
                                                                |}
                                                                |]
                                                                |},
                                                                |"declarationSchedule3":"schedule-3",
                                                                |"numberOfIndividualsGrantedOptions":0,
                                                                |"numberOfContractsWithin5Yr3YrNoBonus":0,
                                                                |"numberOfIndividualsExercisedOptions":0,
                                                                |"numberOfSharesIssuedTransferredOnExerciseOfOptions":0,
                                                                |"numberOfIndividualsSavedMaxLimit":0,
                                                                |"amtSharesPaid":0,
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

    "return a valid NilReturn with all ammends" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())(any())
      ).thenReturn(
        ERSEnvelope(Future.successful(List()))
      )

      val result = await(mockAdrSubmission.generateSubmission(SAYE.ersSummaryNilReturnWithAllAltAmmends)(request, hc).value)
      result.value - "acknowledgementReference" shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"SAYE",
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
                                                                |"groupPlan":true,
                                                                |"optionsGrantedInYear":false,
                                                                |"optionsExercisedInYear":false,
                                                                |"optionsReleasedCancelledInYear":false,
                                                                |"alterationsAmendmentsMadeInYear":true,
                                                                |"alteration":{
                                                                |"alterationTypes":[
                                                                |{
                                                                |"typeOfAlteration":"first"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"second"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"third"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"fourth"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"fifth"
                                                                |}
                                                                |]
                                                                |},
                                                                |"declarationSchedule3":"schedule-3",
                                                                |"numberOfIndividualsGrantedOptions":0,
                                                                |"numberOfContractsWithin5Yr3YrNoBonus":0,
                                                                |"numberOfIndividualsExercisedOptions":0,
                                                                |"numberOfSharesIssuedTransferredOnExerciseOfOptions":0,
                                                                |"numberOfIndividualsSavedMaxLimit":0,
                                                                |"amtSharesPaid":0,
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

    "return a valid not NilReturn with ammends and participants without data" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())(any())
      ).thenReturn(
        ERSEnvelope(Future.successful(List()))
      )

      val result = await(mockAdrSubmission.generateSubmission(SAYE.ersSumarryWithAllAmmends)(request, hc).value)
      result.value - "acknowledgementReference" shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"SAYE",
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
                                                                |"groupPlan":true,
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
                                                                |"optionsGrantedInYear":false,
                                                                |"optionsExercisedInYear":false,
                                                                |"optionsReleasedCancelledInYear":false,
                                                                |"alterationsAmendmentsMadeInYear":true,
                                                                |"alteration":{
                                                                |"alterationTypes":[
                                                                |{
                                                                |"typeOfAlteration":"first"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"second"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"third"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"fourth"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"fifth"
                                                                |}
                                                                |]
                                                                |},
                                                                |"declarationSchedule3":"schedule-3",
                                                                |"numberOfIndividualsGrantedOptions":0,
                                                                |"numberOfContractsWithin5Yr3YrNoBonus":0,
                                                                |"numberOfIndividualsExercisedOptions":0,
                                                                |"numberOfSharesIssuedTransferredOnExerciseOfOptions":0,
                                                                |"numberOfIndividualsSavedMaxLimit":0,
                                                                |"amtSharesPaid":0,
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

    "return a valid not NilReturn with Granted, ammends and participants" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())(any())
      ).thenReturn(
        ERSEnvelope(Future.successful(
          List(
            SchemeData(SAYE.schemeInfo, "SAYE_Granted_V4", None, Some(ListBuffer(SAYE.buildGrantedV4("yes", "yes"))))
          )
        )
      ))

      val result = await(mockAdrSubmission.generateSubmission(SAYE.ersSumarryWithAllAmmends)(request, hc).value)
      result.value - "acknowledgementReference" shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"SAYE",
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
                                                                |"groupPlan":true,
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
                                                                |"optionsGrantedInYear":true,
                                                                |"granted":{
                                                                |"grantEvents":[
                                                                |{
                                                                |"dateOfGrant":"2015-12-31",
                                                                |"numberOfIndividuals":123456,
                                                                |"numberOfSharesGrantedOver":100.0,
                                                                |"marketValuePerShareUsedToDetermineExercisePrice":10.1234,
                                                                |"exercisePricePerShare":10.1234,
                                                                |"sharesListedOnSE":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"optionsExercisedInYear":false,
                                                                |"optionsReleasedCancelledInYear":false,
                                                                |"alterationsAmendmentsMadeInYear":true,
                                                                |"alteration":{
                                                                |"alterationTypes":[
                                                                |{
                                                                |"typeOfAlteration":"first"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"second"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"third"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"fourth"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"fifth"
                                                                |}
                                                                |]
                                                                |},
                                                                |"declarationSchedule3":"schedule-3",
                                                                |"numberOfIndividualsGrantedOptions":0,
                                                                |"numberOfContractsWithin5Yr3YrNoBonus":0,
                                                                |"numberOfIndividualsExercisedOptions":0,
                                                                |"numberOfSharesIssuedTransferredOnExerciseOfOptions":0,
                                                                |"numberOfIndividualsSavedMaxLimit":0,
                                                                |"amtSharesPaid":0,
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

    "return a valid not NilReturn with Granted, ammends and participants from 2 records of sheet data" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())(any())
      ).thenReturn(
        ERSEnvelope(Future.successful(
          List(
            SchemeData(SAYE.schemeInfo, "SAYE_Granted_V4", None, Some(ListBuffer(SAYE.buildGrantedV4("yes", "yes")))),
            SchemeData(SAYE.schemeInfo, "SAYE_Granted_V4", None, Some(ListBuffer(SAYE.buildGrantedV4("yes", "yes"))))
          )
        )
      ))

      val result = await(mockAdrSubmission.generateSubmission(SAYE.ersSumarryWithAllAmmends)(request, hc).value)
      result.value - "acknowledgementReference" shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"SAYE",
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
                                                                |"groupPlan":true,
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
                                                                |"optionsGrantedInYear":true,
                                                                |"granted":{
                                                                |"grantEvents":[
                                                                |{
                                                                |"dateOfGrant":"2015-12-31",
                                                                |"numberOfIndividuals":123456,
                                                                |"numberOfSharesGrantedOver":100.0,
                                                                |"marketValuePerShareUsedToDetermineExercisePrice":10.1234,
                                                                |"exercisePricePerShare":10.1234,
                                                                |"sharesListedOnSE":true
                                                                |},
                                                                |{
                                                                |"dateOfGrant":"2015-12-31",
                                                                |"numberOfIndividuals":123456,
                                                                |"numberOfSharesGrantedOver":100.0,
                                                                |"marketValuePerShareUsedToDetermineExercisePrice":10.1234,
                                                                |"exercisePricePerShare":10.1234,
                                                                |"sharesListedOnSE":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"optionsExercisedInYear":false,
                                                                |"optionsReleasedCancelledInYear":false,
                                                                |"alterationsAmendmentsMadeInYear":true,
                                                                |"alteration":{
                                                                |"alterationTypes":[
                                                                |{
                                                                |"typeOfAlteration":"first"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"second"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"third"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"fourth"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"fifth"
                                                                |}
                                                                |]
                                                                |},
                                                                |"declarationSchedule3":"schedule-3",
                                                                |"numberOfIndividualsGrantedOptions":0,
                                                                |"numberOfContractsWithin5Yr3YrNoBonus":0,
                                                                |"numberOfIndividualsExercisedOptions":0,
                                                                |"numberOfSharesIssuedTransferredOnExerciseOfOptions":0,
                                                                |"numberOfIndividualsSavedMaxLimit":0,
                                                                |"amtSharesPaid":0,
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

    "return a valid not NilReturn with RCL, amends and participants" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())(any())
      ).thenReturn(
        ERSEnvelope(Future.successful(
          List(
            SchemeData(SAYE.schemeInfo, "SAYE_RCL_V4", None, Some(ListBuffer(SAYE.buildRCLV4("yes"))))
          )
        )
      ))

      val result = await(mockAdrSubmission.generateSubmission(SAYE.ersSumarryWithAllAmmends)(request, hc).value)
      result.value - "acknowledgementReference" shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"SAYE",
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
                                                                |"groupPlan":true,
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
                                                                |"optionsReleasedCancelledInYear":true,
                                                                |"released":{
                                                                |"releasedEvents":[
                                                                |{
                                                                |"dateOfEvent":"2015-12-31",
                                                                |"wasMoneyOrValueGiven":true,
                                                                |"amountReleased":10.1234,
                                                                |"individualReleased":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"payeOperatedApplied":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"optionsGrantedInYear":false,
                                                                |"optionsExercisedInYear":false,
                                                                |"alterationsAmendmentsMadeInYear":true,
                                                                |"alteration":{
                                                                |"alterationTypes":[
                                                                |{
                                                                |"typeOfAlteration":"first"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"second"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"third"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"fourth"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"fifth"
                                                                |}
                                                                |]
                                                                |},
                                                                |"declarationSchedule3":"schedule-3",
                                                                |"numberOfIndividualsGrantedOptions":0,
                                                                |"numberOfContractsWithin5Yr3YrNoBonus":0,
                                                                |"numberOfIndividualsExercisedOptions":0,
                                                                |"numberOfSharesIssuedTransferredOnExerciseOfOptions":0,
                                                                |"numberOfIndividualsSavedMaxLimit":0,
                                                                |"amtSharesPaid":0,
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

    "return a valid not NilReturn with RCL, amends and participants from 2 records of sheet data" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())(any())
      ).thenReturn(
        ERSEnvelope(Future.successful(
          List(
            SchemeData(SAYE.schemeInfo, "SAYE_RCL_V4", None, Some(ListBuffer(SAYE.buildRCLV4("yes")))),
            SchemeData(SAYE.schemeInfo, "SAYE_RCL_V4", None, Some(ListBuffer(SAYE.buildRCLV4("yes"))))
          )
        )
      ))

      val result = await(mockAdrSubmission.generateSubmission(SAYE.ersSumarryWithAllAmmends)(request, hc).value)
      result.value - "acknowledgementReference" shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"SAYE",
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
                                                                |"groupPlan":true,
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
                                                                |"optionsReleasedCancelledInYear":true,
                                                                |"released":{
                                                                |"releasedEvents":[
                                                                |{
                                                                |"dateOfEvent":"2015-12-31",
                                                                |"wasMoneyOrValueGiven":true,
                                                                |"amountReleased":10.1234,
                                                                |"individualReleased":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"payeOperatedApplied":true
                                                                |},
                                                                |{
                                                                |"dateOfEvent":"2015-12-31",
                                                                |"wasMoneyOrValueGiven":true,
                                                                |"amountReleased":10.1234,
                                                                |"individualReleased":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"payeOperatedApplied":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"optionsGrantedInYear":false,
                                                                |"optionsExercisedInYear":false,
                                                                |"alterationsAmendmentsMadeInYear":true,
                                                                |"alteration":{
                                                                |"alterationTypes":[
                                                                |{
                                                                |"typeOfAlteration":"first"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"second"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"third"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"fourth"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"fifth"
                                                                |}
                                                                |]
                                                                |},
                                                                |"declarationSchedule3":"schedule-3",
                                                                |"numberOfIndividualsGrantedOptions":0,
                                                                |"numberOfContractsWithin5Yr3YrNoBonus":0,
                                                                |"numberOfIndividualsExercisedOptions":0,
                                                                |"numberOfSharesIssuedTransferredOnExerciseOfOptions":0,
                                                                |"numberOfIndividualsSavedMaxLimit":0,
                                                                |"amtSharesPaid":0,
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

    "return a valid not NilReturn with Exercised, amends and participants" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())(any())
      ).thenReturn(
        ERSEnvelope(Future.successful(
          List(
            SchemeData(SAYE.schemeInfo, "SAYE_Exercised_V4", None, Some(ListBuffer(SAYE.buildExercisedV4(true, sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes"))))
          )
        )
      ))

      val result = await(mockAdrSubmission.generateSubmission(SAYE.ersSumarryWithAllAmmends)(request, hc).value)
      result.value - "acknowledgementReference" shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"SAYE",
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
                                                                |"groupPlan":true,
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
                                                                |"optionsReleasedCancelledInYear":false,
                                                                |"optionsGrantedInYear":false,
                                                                |"optionsExercisedInYear":true,
                                                                |"exercised":{
                                                                |"exerciseEvents":[
                                                                |{
                                                                |"dateOfEvent":"2014-01-01",
                                                                |"individual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"dateOfGrant":"2015-12-31",
                                                                |"numberOfSharesAcquired":10.12,
                                                                |"sharesListedOnSE":true,
                                                                |"actualMarketValuePerShareAtAcquisitionDate":10.1234,
                                                                |"exercisePricePerShare":10.1234,
                                                                |"unrestrictedMarketValuePerShareAtExerciseDate":11.1234,
                                                                |"qualifyForTaxRelief":true,
                                                                |"sharesSoldInConnectionWithTheExercise":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"alterationsAmendmentsMadeInYear":true,
                                                                |"alteration":{
                                                                |"alterationTypes":[
                                                                |{
                                                                |"typeOfAlteration":"first"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"second"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"third"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"fourth"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"fifth"
                                                                |}
                                                                |]
                                                                |},
                                                                |"declarationSchedule3":"schedule-3",
                                                                |"numberOfIndividualsGrantedOptions":0,
                                                                |"numberOfContractsWithin5Yr3YrNoBonus":0,
                                                                |"numberOfIndividualsExercisedOptions":0,
                                                                |"numberOfSharesIssuedTransferredOnExerciseOfOptions":0,
                                                                |"numberOfIndividualsSavedMaxLimit":0,
                                                                |"amtSharesPaid":0,
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

    "return a valid not NilReturn with Exercised, amends and participants from 2 records of sheet data" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())(any())
      ).thenReturn(
        ERSEnvelope(Future.successful(
          List(
            SchemeData(SAYE.schemeInfo, "SAYE_Exercised_V4", None, Some(ListBuffer(SAYE.buildExercisedV4(true, sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes")))),
            SchemeData(SAYE.schemeInfo, "SAYE_Exercised_V4", None, Some(ListBuffer(SAYE.buildExercisedV4(true, sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes"))))
          )
        )
      ))

      val result = await(mockAdrSubmission.generateSubmission(SAYE.ersSumarryWithAllAmmends)(request, hc).value)
      result.value - "acknowledgementReference" shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"SAYE",
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
                                                                |"groupPlan":true,
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
                                                                |"optionsReleasedCancelledInYear":false,
                                                                |"optionsGrantedInYear":false,
                                                                |"optionsExercisedInYear":true,
                                                                |"exercised":{
                                                                |"exerciseEvents":[
                                                                |{
                                                                |"dateOfEvent":"2014-01-01",
                                                                |"individual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"dateOfGrant":"2015-12-31",
                                                                |"numberOfSharesAcquired":10.12,
                                                                |"sharesListedOnSE":true,
                                                                |"actualMarketValuePerShareAtAcquisitionDate":10.1234,
                                                                |"exercisePricePerShare":10.1234,
                                                                |"unrestrictedMarketValuePerShareAtExerciseDate":11.1234,
                                                                |"qualifyForTaxRelief":true,
                                                                |"sharesSoldInConnectionWithTheExercise":true
                                                                |},
                                                                |{
                                                                |"dateOfEvent":"2014-01-01",
                                                                |"individual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"dateOfGrant":"2015-12-31",
                                                                |"numberOfSharesAcquired":10.12,
                                                                |"sharesListedOnSE":true,
                                                                |"actualMarketValuePerShareAtAcquisitionDate":10.1234,
                                                                |"exercisePricePerShare":10.1234,
                                                                |"unrestrictedMarketValuePerShareAtExerciseDate":11.1234,
                                                                |"qualifyForTaxRelief":true,
                                                                |"sharesSoldInConnectionWithTheExercise":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"alterationsAmendmentsMadeInYear":true,
                                                                |"alteration":{
                                                                |"alterationTypes":[
                                                                |{
                                                                |"typeOfAlteration":"first"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"second"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"third"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"fourth"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"fifth"
                                                                |}
                                                                |]
                                                                |},
                                                                |"declarationSchedule3":"schedule-3",
                                                                |"numberOfIndividualsGrantedOptions":0,
                                                                |"numberOfContractsWithin5Yr3YrNoBonus":0,
                                                                |"numberOfIndividualsExercisedOptions":0,
                                                                |"numberOfSharesIssuedTransferredOnExerciseOfOptions":0,
                                                                |"numberOfIndividualsSavedMaxLimit":0,
                                                                |"amtSharesPaid":0,
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

    "return valid json with Granted, RCL, exercised, ammends and participants" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())(any())
      ).thenReturn(
        ERSEnvelope(Future.successful(
          List(
            SchemeData(SAYE.schemeInfo, "SAYE_Granted_V4", None, Some(ListBuffer(SAYE.buildGrantedV4("yes", "yes")))),
            SchemeData(SAYE.schemeInfo, "SAYE_RCL_V4", None, Some(ListBuffer(SAYE.buildRCLV4("yes")))),
            SchemeData(SAYE.schemeInfo, "SAYE_Exercised_V4", None, Some(ListBuffer(SAYE.buildExercisedV4(true, sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes"))))
          )
        )
      ))

      val result = await(mockAdrSubmission.generateSubmission(SAYE.ersSumarryWithAllAmmends)(request, hc).value)
      result.value - "acknowledgementReference" shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"SAYE",
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
                                                                |"groupPlan":true,
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
                                                                |"optionsGrantedInYear":true,
                                                                |"granted":{
                                                                |"grantEvents":[
                                                                |{
                                                                |"dateOfGrant":"2015-12-31",
                                                                |"numberOfIndividuals":123456,
                                                                |"numberOfSharesGrantedOver":100.0,
                                                                |"marketValuePerShareUsedToDetermineExercisePrice":10.1234,
                                                                |"exercisePricePerShare":10.1234,
                                                                |"sharesListedOnSE":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"optionsExercisedInYear":true,
                                                                |"exercised":{
                                                                |"exerciseEvents":[
                                                                |{
                                                                |"dateOfEvent":"2014-01-01",
                                                                |"individual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"dateOfGrant":"2015-12-31",
                                                                |"numberOfSharesAcquired":10.12,
                                                                |"sharesListedOnSE":true,
                                                                |"actualMarketValuePerShareAtAcquisitionDate":10.1234,
                                                                |"exercisePricePerShare":10.1234,
                                                                |"unrestrictedMarketValuePerShareAtExerciseDate":11.1234,
                                                                |"qualifyForTaxRelief":true,
                                                                |"sharesSoldInConnectionWithTheExercise":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"optionsReleasedCancelledInYear":true,
                                                                |"released":{
                                                                |"releasedEvents":[
                                                                |{
                                                                |"dateOfEvent":"2015-12-31",
                                                                |"wasMoneyOrValueGiven":true,
                                                                |"amountReleased":10.1234,
                                                                |"individualReleased":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"payeOperatedApplied":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"alterationsAmendmentsMadeInYear":true,
                                                                |"alteration":{
                                                                |"alterationTypes":[
                                                                |{
                                                                |"typeOfAlteration":"first"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"second"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"third"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"fourth"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"fifth"
                                                                |}
                                                                |]
                                                                |},
                                                                |"declarationSchedule3":"schedule-3",
                                                                |"numberOfIndividualsGrantedOptions":0,
                                                                |"numberOfContractsWithin5Yr3YrNoBonus":0,
                                                                |"numberOfIndividualsExercisedOptions":0,
                                                                |"numberOfSharesIssuedTransferredOnExerciseOfOptions":0,
                                                                |"numberOfIndividualsSavedMaxLimit":0,
                                                                |"amtSharesPaid":0,
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

    "return valid json with Granted, RCL, exercised, ammends and participants from 2 records of sheet data" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())(any())
      ).thenReturn(
        ERSEnvelope(Future.successful(
          List(
            SchemeData(SAYE.schemeInfo, "SAYE_Granted_V4", None, Some(ListBuffer(SAYE.buildGrantedV4("yes", "yes")))),
            SchemeData(SAYE.schemeInfo, "SAYE_Granted_V4", None, Some(ListBuffer(SAYE.buildGrantedV4("yes", "yes")))),
            SchemeData(SAYE.schemeInfo, "SAYE_RCL_V4", None, Some(ListBuffer(SAYE.buildRCLV4("yes")))),
            SchemeData(SAYE.schemeInfo, "SAYE_RCL_V4", None, Some(ListBuffer(SAYE.buildRCLV4("yes")))),
            SchemeData(SAYE.schemeInfo, "SAYE_Exercised_V4", None, Some(ListBuffer(SAYE.buildExercisedV4(true, sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes")))),
            SchemeData(SAYE.schemeInfo, "SAYE_Exercised_V4", None, Some(ListBuffer(SAYE.buildExercisedV4(true, sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes"))))
          )
        )
      ))

      val result = await(mockAdrSubmission.generateSubmission(SAYE.ersSumarryWithAllAmmends)(request, hc).value)
      result.value - "acknowledgementReference" shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"SAYE",
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
                                                                |"groupPlan":true,
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
                                                                |"optionsGrantedInYear":true,
                                                                |"granted":{
                                                                |"grantEvents":[
                                                                |{
                                                                |"dateOfGrant":"2015-12-31",
                                                                |"numberOfIndividuals":123456,
                                                                |"numberOfSharesGrantedOver":100.0,
                                                                |"marketValuePerShareUsedToDetermineExercisePrice":10.1234,
                                                                |"exercisePricePerShare":10.1234,
                                                                |"sharesListedOnSE":true
                                                                |},
                                                                |{
                                                                |"dateOfGrant":"2015-12-31",
                                                                |"numberOfIndividuals":123456,
                                                                |"numberOfSharesGrantedOver":100.0,
                                                                |"marketValuePerShareUsedToDetermineExercisePrice":10.1234,
                                                                |"exercisePricePerShare":10.1234,
                                                                |"sharesListedOnSE":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"optionsExercisedInYear":true,
                                                                |"exercised":{
                                                                |"exerciseEvents":[
                                                                |{
                                                                |"dateOfEvent":"2014-01-01",
                                                                |"individual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"dateOfGrant":"2015-12-31",
                                                                |"numberOfSharesAcquired":10.12,
                                                                |"sharesListedOnSE":true,
                                                                |"actualMarketValuePerShareAtAcquisitionDate":10.1234,
                                                                |"exercisePricePerShare":10.1234,
                                                                |"unrestrictedMarketValuePerShareAtExerciseDate":11.1234,
                                                                |"qualifyForTaxRelief":true,
                                                                |"sharesSoldInConnectionWithTheExercise":true
                                                                |},
                                                                |{
                                                                |"dateOfEvent":"2014-01-01",
                                                                |"individual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"dateOfGrant":"2015-12-31",
                                                                |"numberOfSharesAcquired":10.12,
                                                                |"sharesListedOnSE":true,
                                                                |"actualMarketValuePerShareAtAcquisitionDate":10.1234,
                                                                |"exercisePricePerShare":10.1234,
                                                                |"unrestrictedMarketValuePerShareAtExerciseDate":11.1234,
                                                                |"qualifyForTaxRelief":true,
                                                                |"sharesSoldInConnectionWithTheExercise":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"optionsReleasedCancelledInYear":true,
                                                                |"released":{
                                                                |"releasedEvents":[
                                                                |{
                                                                |"dateOfEvent":"2015-12-31",
                                                                |"wasMoneyOrValueGiven":true,
                                                                |"amountReleased":10.1234,
                                                                |"individualReleased":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"payeOperatedApplied":true
                                                                |},
                                                                |{
                                                                |"dateOfEvent":"2015-12-31",
                                                                |"wasMoneyOrValueGiven":true,
                                                                |"amountReleased":10.1234,
                                                                |"individualReleased":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"payeOperatedApplied":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"alterationsAmendmentsMadeInYear":true,
                                                                |"alteration":{
                                                                |"alterationTypes":[
                                                                |{
                                                                |"typeOfAlteration":"first"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"second"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"third"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"fourth"
                                                                |},
                                                                |{
                                                                |"typeOfAlteration":"fifth"
                                                                |}
                                                                |]
                                                                |},
                                                                |"declarationSchedule3":"schedule-3",
                                                                |"numberOfIndividualsGrantedOptions":0,
                                                                |"numberOfContractsWithin5Yr3YrNoBonus":0,
                                                                |"numberOfIndividualsExercisedOptions":0,
                                                                |"numberOfSharesIssuedTransferredOnExerciseOfOptions":0,
                                                                |"numberOfIndividualsSavedMaxLimit":0,
                                                                |"amtSharesPaid":0,
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

  }
  //SAYE_Exercised_V4
  "calling generateJson for Exercised_V4" should {

    val configData: Config = Common.loadConfiguration(SAYE.sayeSchemeType, "SAYE_Exercised_V4", mockConfigUtils)

    "create a valid JSON with allFields = (true or false), sharesListedOnSE = \"yes\", marketValueAgreedHMRC = \"yes\"" in {
      val result = mockAdrSubmission.buildJson(
      configData,
        ListBuffer(
          SAYE.buildExercisedV4(true, sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes"),
          SAYE.buildExercisedV4(false, sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes")
        )
      )
      result shouldBe Json.parse("""{
                                   |"optionsExercisedInYear":true,
                                   |"exercised":{
                                   |"exerciseEvents":[
                                   |{
                                   |"dateOfEvent":"2014-01-01",
                                   |"individual":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateOfGrant":"2015-12-31",
                                   |"numberOfSharesAcquired":10.12,
                                   |"sharesListedOnSE":true,
                                   |"actualMarketValuePerShareAtAcquisitionDate":10.1234,
                                   |"exercisePricePerShare":10.1234,
                                   |"unrestrictedMarketValuePerShareAtExerciseDate":11.1234,
                                   |"qualifyForTaxRelief":true,
                                   |"sharesSoldInConnectionWithTheExercise":true
                                   |},
                                   |{
                                   |"dateOfEvent":"2014-01-01",
                                   |"individual":{
                                   |"firstName":"First",
                                   |"surname":"Last",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateOfGrant":"2015-12-31",
                                   |"numberOfSharesAcquired":10.12,
                                   |"sharesListedOnSE":true,
                                   |"actualMarketValuePerShareAtAcquisitionDate":10.1234,
                                   |"exercisePricePerShare":10.1234,
                                   |"unrestrictedMarketValuePerShareAtExerciseDate":11.1234,
                                   |"qualifyForTaxRelief":true,
                                   |"sharesSoldInConnectionWithTheExercise":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create a valid JSON with allFields = true, sharesListedOnSE = (\"yes\" or \"no\"), marketValueAgreedHMRC = \"yes\"" in {
      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          SAYE.buildExercisedV4(true, sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes"),
          SAYE.buildExercisedV4(true, sharesListedOnSE = "no", marketValueAgreedHMRC = "yes")
        )
      )
      result shouldBe Json.parse("""{
                                   |"optionsExercisedInYear":true,
                                   |"exercised":{
                                   |"exerciseEvents":[
                                   |{
                                   |"dateOfEvent":"2014-01-01",
                                   |"individual":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateOfGrant":"2015-12-31",
                                   |"numberOfSharesAcquired":10.12,
                                   |"sharesListedOnSE":true,
                                   |"actualMarketValuePerShareAtAcquisitionDate":10.1234,
                                   |"exercisePricePerShare":10.1234,
                                   |"unrestrictedMarketValuePerShareAtExerciseDate":11.1234,
                                   |"qualifyForTaxRelief":true,
                                   |"sharesSoldInConnectionWithTheExercise":true
                                   |},
                                   |{
                                   |"dateOfEvent":"2014-01-01",
                                   |"individual":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateOfGrant":"2015-12-31",
                                   |"numberOfSharesAcquired":10.12,
                                   |"sharesListedOnSE":false,
                                   |"marketValueAgreedHMRC":true,
                                   |"hmrcRef":"aa12345678",
                                   |"actualMarketValuePerShareAtAcquisitionDate":10.1234,
                                   |"exercisePricePerShare":10.1234,
                                   |"unrestrictedMarketValuePerShareAtExerciseDate":11.1234,
                                   |"qualifyForTaxRelief":true,
                                   |"sharesSoldInConnectionWithTheExercise":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create a valid JSON with allFields = true, sharesListedOnSE = \"no\", marketValueAgreedHMRC = (\"yes\" or \"no\")" in {
      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          SAYE.buildExercisedV4(true, sharesListedOnSE = "no", marketValueAgreedHMRC = "yes"),
          SAYE.buildExercisedV4(true, sharesListedOnSE = "no", marketValueAgreedHMRC = "no")
        )
      )
      result shouldBe Json.parse("""{
                                   |"optionsExercisedInYear":true,
                                   |"exercised":{
                                   |"exerciseEvents":[
                                   |{
                                   |"dateOfEvent":"2014-01-01",
                                   |"individual":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateOfGrant":"2015-12-31",
                                   |"numberOfSharesAcquired":10.12,
                                   |"sharesListedOnSE":false,
                                   |"marketValueAgreedHMRC":true,
                                   |"hmrcRef":"aa12345678",
                                   |"actualMarketValuePerShareAtAcquisitionDate":10.1234,
                                   |"exercisePricePerShare":10.1234,
                                   |"unrestrictedMarketValuePerShareAtExerciseDate":11.1234,
                                   |"qualifyForTaxRelief":true,
                                   |"sharesSoldInConnectionWithTheExercise":true
                                   |},
                                   |{
                                   |"dateOfEvent":"2014-01-01",
                                   |"individual":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateOfGrant":"2015-12-31",
                                   |"numberOfSharesAcquired":10.12,
                                   |"sharesListedOnSE":false,
                                   |"marketValueAgreedHMRC":false,
                                   |"actualMarketValuePerShareAtAcquisitionDate":10.1234,
                                   |"exercisePricePerShare":10.1234,
                                   |"unrestrictedMarketValuePerShareAtExerciseDate":11.1234,
                                   |"qualifyForTaxRelief":true,
                                   |"sharesSoldInConnectionWithTheExercise":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }
  }

  // SAYE_Granted_V4
  "calling generateJson for Granted_V4" should {

    val configData: Config = Common.loadConfiguration(SAYE.sayeSchemeType, "SAYE_Granted_V4", mockConfigUtils)

    "create valid JSON with sharesListedOnSE = (\"yes\" or \"no\"), marketValueAgreedHMRC = \"yes\"" in {

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          SAYE.buildGrantedV4(sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes"),
          SAYE.buildGrantedV4(sharesListedOnSE = "no", marketValueAgreedHMRC = "yes")
        )
      )

      result shouldBe Json.parse("""{
                                   |"optionsGrantedInYear":true,
                                   |"granted":{
                                   |"grantEvents":[
                                   |{
                                   |"dateOfGrant":"2015-12-31",
                                   |"numberOfIndividuals":123456,
                                   |"numberOfSharesGrantedOver":100.0,
                                   |"marketValuePerShareUsedToDetermineExercisePrice":10.1234,
                                   |"exercisePricePerShare":10.1234,
                                   |"sharesListedOnSE":true
                                   |},
                                   |{
                                   |"dateOfGrant":"2015-12-31",
                                   |"numberOfIndividuals":123456,
                                   |"numberOfSharesGrantedOver":100.0,
                                   |"marketValuePerShareUsedToDetermineExercisePrice":10.1234,
                                   |"exercisePricePerShare":10.1234,
                                   |"sharesListedOnSE":false,
                                   |"marketValueAgreedHMRC":true,
                                   |"hmrcRef":"aa12345678"
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)

    }

    "create valid JSON with sharesListedOnSE = \"no\", marketValueAgreedHMRC = (\"yes\" or \"no\")" in {

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          SAYE.buildGrantedV4(sharesListedOnSE = "no", marketValueAgreedHMRC = "yes"),
          SAYE.buildGrantedV4(sharesListedOnSE = "no", marketValueAgreedHMRC = "no")
        )
      )

      result shouldBe Json.parse("""{
                                   |"optionsGrantedInYear":true,
                                   |"granted":{
                                   |"grantEvents":[
                                   |{
                                   |"dateOfGrant":"2015-12-31",
                                   |"numberOfIndividuals":123456,
                                   |"numberOfSharesGrantedOver":100.0,
                                   |"marketValuePerShareUsedToDetermineExercisePrice":10.1234,
                                   |"exercisePricePerShare":10.1234,
                                   |"sharesListedOnSE":false,
                                   |"marketValueAgreedHMRC":true,
                                   |"hmrcRef":"aa12345678"
                                   |},
                                   |{
                                   |"dateOfGrant":"2015-12-31",
                                   |"numberOfIndividuals":123456,
                                   |"numberOfSharesGrantedOver":100.0,
                                   |"marketValuePerShareUsedToDetermineExercisePrice":10.1234,
                                   |"exercisePricePerShare":10.1234,
                                   |"sharesListedOnSE":false,
                                   |"marketValueAgreedHMRC":false
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)

    }
  }

  // SAYE_RCL_V4
  "calling generateJson for RCL_V4" should {

    val configData: Config = Common.loadConfiguration(SAYE.sayeSchemeType, "SAYE_RCL_V4", mockConfigUtils)

    "create valid JSON with wasMoneyOrValueGiven = \"yes\"" in {

      val result = mockAdrSubmission.buildJson(configData,ListBuffer(
        SAYE.buildRCLV4(wasMoneyOrValueGiven="yes")
      ))

      result shouldBe Json.parse("""{
                                   |"optionsReleasedCancelledInYear":true,
                                   |"released":{
                                   |"releasedEvents":[
                                   |{
                                   |"dateOfEvent":"2015-12-31",
                                   |"wasMoneyOrValueGiven":true,
                                   |"amountReleased":10.1234,
                                   |"individualReleased":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"payeOperatedApplied":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)

    }

    "create valid JSON with wasMoneyOrValueGiven = \"no\"" in {

      val result = mockAdrSubmission.buildJson(configData,ListBuffer(
        SAYE.buildRCLV4(wasMoneyOrValueGiven="no")
      ))

      result shouldBe Json.parse("""{
                                   |"optionsReleasedCancelledInYear":true,
                                   |"released":{
                                   |"releasedEvents":[
                                   |{
                                   |"dateOfEvent":"2015-12-31",
                                   |"wasMoneyOrValueGiven":false,
                                   |"individualReleased":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"payeOperatedApplied":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)

    }

    "create valid JSON without a secondName" in {

      val result = mockAdrSubmission.buildJson(configData,ListBuffer(
        SAYE.buildRCLV4(secondName="")
      ))

      result shouldBe Json.parse("""{
                                   |"optionsReleasedCancelledInYear":true,
                                   |"released":{
                                   |"releasedEvents":[
                                   |{
                                   |"dateOfEvent":"2015-12-31",
                                   |"wasMoneyOrValueGiven":true,
                                   |"amountReleased":10.1234,
                                   |"individualReleased":{
                                   |"firstName":"First",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"payeOperatedApplied":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)

    }

    "create valid JSON without a nino" in {

      val result = mockAdrSubmission.buildJson(configData,ListBuffer(
        SAYE.buildRCLV4(nino="")
      ))

      result shouldBe Json.parse("""{
                                   |"optionsReleasedCancelledInYear":true,
                                   |"released":{
                                   |"releasedEvents":[
                                   |{
                                   |"dateOfEvent":"2015-12-31",
                                   |"wasMoneyOrValueGiven":true,
                                   |"amountReleased":10.1234,
                                   |"individualReleased":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"payeOperatedApplied":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)

    }

  }

}
