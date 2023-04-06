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
import fixtures.{CSOP, Common, Fixtures}
import helpers.ERSTestHelper
import models.{SchemeData, SchemeInfo}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import services.PresubmissionService
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggingAndRexceptions.ADRExceptionEmitter
import utils.{ADRSubmission, ConfigUtils, SubmissionCommon}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class CSOP_ADRSubmissionSpec
  extends ERSTestHelper with BeforeAndAfter {

  val mockSubmissionCommon: SubmissionCommon = app.injector.instanceOf[SubmissionCommon]
  val mockAdrExceptionEmitter: ADRExceptionEmitter = mock[ADRExceptionEmitter]
  val mockConfigUtils: ConfigUtils = app.injector.instanceOf[ConfigUtils]
  val mockPresubmissionService: PresubmissionService = mock[PresubmissionService]

  implicit val hc: HeaderCarrier = new HeaderCarrier()
  implicit val request: FakeRequest[JsObject] = FakeRequest().withBody(Fixtures.metadataJson)

  val adrSubmission: ADRSubmission = new ADRSubmission(
    mockSubmissionCommon,
    mockPresubmissionService,
    mockAdrExceptionEmitter,
    mockConfigUtils
  )
  def before(fun : => scala.Any): Unit  = {
    super.before(())
    reset(mockPresubmissionService)
  }

  "calling generateSubmissionReturn" should {

    "return a valid NilReturn without ammends" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(List())
      )

      val result = await(adrSubmission.generateSubmission()(request, hc, CSOP.metadataNilReturnWithoutAltAmmends))
      result - "acknowledgementReference" shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"CSOP",
                                                                |"schemeReference":"XA1100000000000",
                                                                |"taxYear":"2015/16",
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
                                                                |"optionsReleasedExchangesCancelledLapsedInYear":false,
                                                                |"alterationsAmendmentsMadeInYear":false,
                                                                |"declarationPart2_8Schedule4":"schedule-4",
                                                                |"numberSharesWithNewOptionsGrantedInYear":0,
                                                                |"numberIndividualsGrantedNewOptions":0,
                                                                |"numberSharesIssuedTransferredOnExerciseOfOptionsDuringTheYear":0,
                                                                |"numberParticipantsWhoExercisedAllOptionsInTheYear":0,
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

    "return a valid NilReturn without some ammends" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(List())
      )

      val result = await(adrSubmission.generateSubmission()(request, hc, CSOP.metadataNilReturnWithSomeAltAmmends))
      result - "acknowledgementReference" shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"CSOP",
                                                                |"schemeReference":"XA1100000000000",
                                                                |"taxYear":"2015/16",
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
                                                                |"optionsReleasedExchangesCancelledLapsedInYear":false,
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
                                                                |"declarationPart2_8Schedule4":"schedule-4",
                                                                |"numberSharesWithNewOptionsGrantedInYear":0,
                                                                |"numberIndividualsGrantedNewOptions":0,
                                                                |"numberSharesIssuedTransferredOnExerciseOfOptionsDuringTheYear":0,
                                                                |"numberParticipantsWhoExercisedAllOptionsInTheYear":0,
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

    "return a valid NilReturn with list of all ammends" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(List())
      )

      val result = await(adrSubmission.generateSubmission()(request, hc, CSOP.metadataNilReturnWithAllAltAmmends))
      result - "acknowledgementReference"  shouldBe Json.parse("""{
                                                                 |"regime":"ERS",
                                                                 |"schemeType":"CSOP",
                                                                 |"schemeReference":"XA1100000000000",
                                                                 |"taxYear":"2015/16",
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
                                                                 |"optionsReleasedExchangesCancelledLapsedInYear":false,
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
                                                                 |"declarationPart2_8Schedule4":"schedule-4",
                                                                 |"numberSharesWithNewOptionsGrantedInYear":0,
                                                                 |"numberIndividualsGrantedNewOptions":0,
                                                                 |"numberSharesIssuedTransferredOnExerciseOfOptionsDuringTheYear":0,
                                                                 |"numberParticipantsWhoExercisedAllOptionsInTheYear":0,
                                                                 |"declaration":"declaration"
                                                                 |}
                                                                 |}""".stripMargin)
    }

    "create valid json for not NulReturn without data with participants" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(List())
      )

      val result = await(adrSubmission.generateSubmission()(request, hc, CSOP.metadata))
      result-("acknowledgementReference") shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"CSOP",
                                                                |"schemeReference":"XA1100000000000",
                                                                |"taxYear":"2015/16",
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
                                                                |"optionsReleasedExchangesCancelledLapsedInYear":false,
                                                                |"alterationsAmendmentsMadeInYear":false,
                                                                |"declarationPart2_8Schedule4":"schedule-4",
                                                                |"numberSharesWithNewOptionsGrantedInYear":0,
                                                                |"numberIndividualsGrantedNewOptions":0,
                                                                |"numberSharesIssuedTransferredOnExerciseOfOptionsDuringTheYear":0,
                                                                |"numberParticipantsWhoExercisedAllOptionsInTheYear":0,
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

    "create valid json for not NulReturn with OptinsGranted and participants" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(
          List(SchemeData(CSOP.schemeInfo, "CSOP_OptionsGranted_V4", None, Some(ListBuffer(CSOP.buildGrantedV4("yes", "yes")))))
        )
      )

      val result = await(adrSubmission.generateSubmission()(request, hc, CSOP.metadata))
      result-("acknowledgementReference") shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"CSOP",
                                                                |"schemeReference":"XA1100000000000",
                                                                |"taxYear":"2015/16",
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
                                                                |"grant":{
                                                                |"grants":[
                                                                |{
                                                                |"dateOfGrant":"2015-12-09",
                                                                |"numberOfIndividuals":123456,
                                                                |"numberOfSharesGrantedOver":50.6,
                                                                |"umvPerShareUsedToDetermineTheExPrice":10.9821,
                                                                |"exercisePricePerShare":8.2587,
                                                                |"sharesListedOnSE":true,
                                                                |"employeeHoldSharesGreaterThan30K":false
                                                                |}
                                                                |]
                                                                |},
                                                                |"optionsExercisedInYear":false,
                                                                |"optionsReleasedExchangesCancelledLapsedInYear":false,
                                                                |"alterationsAmendmentsMadeInYear":false,
                                                                |"declarationPart2_8Schedule4":"schedule-4",
                                                                |"numberSharesWithNewOptionsGrantedInYear":0,
                                                                |"numberIndividualsGrantedNewOptions":0,
                                                                |"numberSharesIssuedTransferredOnExerciseOfOptionsDuringTheYear":0,
                                                                |"numberParticipantsWhoExercisedAllOptionsInTheYear":0,
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

    "create valid json for not NulReturn with OptinsGranted and participants from 2 records of sheet data" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(
          List(
            SchemeData(CSOP.schemeInfo, "CSOP_OptionsGranted_V4", None, Some(ListBuffer(CSOP.buildGrantedV4("yes", "yes")))),
            SchemeData(CSOP.schemeInfo, "CSOP_OptionsGranted_V4", None, Some(ListBuffer(CSOP.buildGrantedV4("yes", "yes"))))
          )
        )
      )

      val result = await(adrSubmission.generateSubmission()(request, hc, CSOP.metadata))
      result-("acknowledgementReference") shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"CSOP",
                                                                |"schemeReference":"XA1100000000000",
                                                                |"taxYear":"2015/16",
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
                                                                |"grant":{
                                                                |"grants":[
                                                                |{
                                                                |"dateOfGrant":"2015-12-09",
                                                                |"numberOfIndividuals":123456,
                                                                |"numberOfSharesGrantedOver":50.6,
                                                                |"umvPerShareUsedToDetermineTheExPrice":10.9821,
                                                                |"exercisePricePerShare":8.2587,
                                                                |"sharesListedOnSE":true,
                                                                |"employeeHoldSharesGreaterThan30K":false
                                                                |},
                                                                |{
                                                                |"dateOfGrant":"2015-12-09",
                                                                |"numberOfIndividuals":123456,
                                                                |"numberOfSharesGrantedOver":50.6,
                                                                |"umvPerShareUsedToDetermineTheExPrice":10.9821,
                                                                |"exercisePricePerShare":8.2587,
                                                                |"sharesListedOnSE":true,
                                                                |"employeeHoldSharesGreaterThan30K":false
                                                                |}
                                                                |]
                                                                |},
                                                                |"optionsExercisedInYear":false,
                                                                |"optionsReleasedExchangesCancelledLapsedInYear":false,
                                                                |"alterationsAmendmentsMadeInYear":false,
                                                                |"declarationPart2_8Schedule4":"schedule-4",
                                                                |"numberSharesWithNewOptionsGrantedInYear":0,
                                                                |"numberIndividualsGrantedNewOptions":0,
                                                                |"numberSharesIssuedTransferredOnExerciseOfOptionsDuringTheYear":0,
                                                                |"numberParticipantsWhoExercisedAllOptionsInTheYear":0,
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

    "create valid json for not NilReturn with Released and participants" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(
          List(SchemeData(CSOP.schemeInfo, "CSOP_OptionsRCL_V4", None, Some(ListBuffer(CSOP.buildOptionsRCL(true, "yes")))))
        )
      )

      val result = await(adrSubmission.generateSubmission()(request, hc, CSOP.metadata))
      result-("acknowledgementReference") shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"CSOP",
                                                                |"schemeReference":"XA1100000000000",
                                                                |"taxYear":"2015/16",
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
                                                                |"optionsReleasedExchangesCancelledLapsedInYear":true,
                                                                |"released":{
                                                                |"releasedEvents":[
                                                                |{
                                                                |"dateOfEvent":"2015-12-09",
                                                                |"wasMoneyOrValueGiven":true,
                                                                |"amtOrValue":10.9821,
                                                                |"releasedIndividual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"payeOperatedApplied":false
                                                                |}
                                                                |]
                                                                |},
                                                                |"alterationsAmendmentsMadeInYear":false,
                                                                |"declarationPart2_8Schedule4":"schedule-4",
                                                                |"numberSharesWithNewOptionsGrantedInYear":0,
                                                                |"numberIndividualsGrantedNewOptions":0,
                                                                |"numberSharesIssuedTransferredOnExerciseOfOptionsDuringTheYear":0,
                                                                |"numberParticipantsWhoExercisedAllOptionsInTheYear":0,
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

    "create valid json for not NilReturn with Released and participants from 2 records of sheet data" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(
          List(
            SchemeData(CSOP.schemeInfo, "CSOP_OptionsRCL_V4", None, Some(ListBuffer(CSOP.buildOptionsRCL(true, "yes")))),
            SchemeData(CSOP.schemeInfo, "CSOP_OptionsRCL_V4", None, Some(ListBuffer(CSOP.buildOptionsRCL(false, "no"))))
          )
        )
      )

      val result = await(adrSubmission.generateSubmission()(request, hc, CSOP.metadata))
      result-("acknowledgementReference") shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"CSOP",
                                                                |"schemeReference":"XA1100000000000",
                                                                |"taxYear":"2015/16",
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
                                                                |"optionsReleasedExchangesCancelledLapsedInYear":true,
                                                                |"released":{
                                                                |"releasedEvents":[
                                                                |{
                                                                |"dateOfEvent":"2015-12-09",
                                                                |"wasMoneyOrValueGiven":true,
                                                                |"amtOrValue":10.9821,
                                                                |"releasedIndividual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"payeOperatedApplied":false
                                                                |},
                                                                |{
                                                                |"dateOfEvent":"2015-12-09",
                                                                |"wasMoneyOrValueGiven":false,
                                                                |"releasedIndividual":{
                                                                |"firstName":"First",
                                                                |"surname":"Last",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"payeOperatedApplied":false
                                                                |}
                                                                |]
                                                                |},
                                                                |"alterationsAmendmentsMadeInYear":false,
                                                                |"declarationPart2_8Schedule4":"schedule-4",
                                                                |"numberSharesWithNewOptionsGrantedInYear":0,
                                                                |"numberIndividualsGrantedNewOptions":0,
                                                                |"numberSharesIssuedTransferredOnExerciseOfOptionsDuringTheYear":0,
                                                                |"numberParticipantsWhoExercisedAllOptionsInTheYear":0,
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

    "create valid json for not NilReturn with OptionsExercised and participants" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(
          List(SchemeData(CSOP.schemeInfo, "CSOP_OptionsExercised_V4", None, Some(ListBuffer(CSOP.buildOptionsExercised(withAllFields = true, sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes", payeOperated = "yes")))))
        )
      )

      val result = await(adrSubmission.generateSubmission()(request, hc, CSOP.metadata))
      result-("acknowledgementReference") shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"CSOP",
                                                                |"schemeReference":"XA1100000000000",
                                                                |"taxYear":"2015/16",
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
                                                                |"optionsExercisedInYear":true,
                                                                |"exercised":{
                                                                |"exercisedEvents":[
                                                                |{
                                                                |"dateOfExercise":"2015-12-09",
                                                                |"individual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"dateOfGrant":"2015-12-10",
                                                                |"numberSharesAcquired":100.0,
                                                                |"sharesPartOfLargestClass":true,
                                                                |"sharesListedOnSE":true,
                                                                |"amvPerShareAtAcquisitionDate":10.1234,
                                                                |"exerciseValuePerShare":10.1234,
                                                                |"umvPerShareAtExerciseDate":10.1234,
                                                                |"qualifyForTaxRelief":true,
                                                                |"payeOperatedApplied":true,
                                                                |"deductibleAmount":10.1234,
                                                                |"nicsElectionAgreementEnteredInto":true,
                                                                |"sharesDisposedOnSameDay":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"optionsReleasedExchangesCancelledLapsedInYear":false,
                                                                |"alterationsAmendmentsMadeInYear":false,
                                                                |"declarationPart2_8Schedule4":"schedule-4",
                                                                |"numberSharesWithNewOptionsGrantedInYear":0,
                                                                |"numberIndividualsGrantedNewOptions":0,
                                                                |"numberSharesIssuedTransferredOnExerciseOfOptionsDuringTheYear":0,
                                                                |"numberParticipantsWhoExercisedAllOptionsInTheYear":0,
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

    "create valid json for not NilReturn with OptionsExercised and participants from 2 records of sheet data" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(
          List(
            SchemeData(CSOP.schemeInfo, "CSOP_OptionsExercised_V4", None, Some(ListBuffer(CSOP.buildOptionsExercised(withAllFields = true, sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes", payeOperated = "yes")))),
            SchemeData(CSOP.schemeInfo, "CSOP_OptionsExercised_V4", None, Some(ListBuffer(CSOP.buildOptionsExercised(withAllFields = true, sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes", payeOperated = "yes"))))
          )
        )
      )

      val result = await(adrSubmission.generateSubmission()(request, hc, CSOP.metadata))
      result-("acknowledgementReference") shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"CSOP",
                                                                |"schemeReference":"XA1100000000000",
                                                                |"taxYear":"2015/16",
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
                                                                |"optionsExercisedInYear":true,
                                                                |"exercised":{
                                                                |"exercisedEvents":[
                                                                |{
                                                                |"dateOfExercise":"2015-12-09",
                                                                |"individual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"dateOfGrant":"2015-12-10",
                                                                |"numberSharesAcquired":100.0,
                                                                |"sharesPartOfLargestClass":true,
                                                                |"sharesListedOnSE":true,
                                                                |"amvPerShareAtAcquisitionDate":10.1234,
                                                                |"exerciseValuePerShare":10.1234,
                                                                |"umvPerShareAtExerciseDate":10.1234,
                                                                |"qualifyForTaxRelief":true,
                                                                |"payeOperatedApplied":true,
                                                                |"deductibleAmount":10.1234,
                                                                |"nicsElectionAgreementEnteredInto":true,
                                                                |"sharesDisposedOnSameDay":true
                                                                |},
                                                                |{
                                                                |"dateOfExercise":"2015-12-09",
                                                                |"individual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"dateOfGrant":"2015-12-10",
                                                                |"numberSharesAcquired":100.0,
                                                                |"sharesPartOfLargestClass":true,
                                                                |"sharesListedOnSE":true,
                                                                |"amvPerShareAtAcquisitionDate":10.1234,
                                                                |"exerciseValuePerShare":10.1234,
                                                                |"umvPerShareAtExerciseDate":10.1234,
                                                                |"qualifyForTaxRelief":true,
                                                                |"payeOperatedApplied":true,
                                                                |"deductibleAmount":10.1234,
                                                                |"nicsElectionAgreementEnteredInto":true,
                                                                |"sharesDisposedOnSameDay":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"optionsReleasedExchangesCancelledLapsedInYear":false,
                                                                |"alterationsAmendmentsMadeInYear":false,
                                                                |"declarationPart2_8Schedule4":"schedule-4",
                                                                |"numberSharesWithNewOptionsGrantedInYear":0,
                                                                |"numberIndividualsGrantedNewOptions":0,
                                                                |"numberSharesIssuedTransferredOnExerciseOfOptionsDuringTheYear":0,
                                                                |"numberParticipantsWhoExercisedAllOptionsInTheYear":0,
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

    "create valid json for not NilReturn with participants, ammends, OptinsGranted, Released and OptionsExercised" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(
          List(
            SchemeData(CSOP.schemeInfo, "CSOP_OptionsGranted_V4", None, Some(ListBuffer(CSOP.buildGrantedV4("yes", "yes")))),
            SchemeData(CSOP.schemeInfo, "CSOP_OptionsRCL_V4", None, Some(ListBuffer(CSOP.buildOptionsRCL(true, "yes")))),
            SchemeData(CSOP.schemeInfo, "CSOP_OptionsExercised_V4", None, Some(ListBuffer(CSOP.buildOptionsExercised(withAllFields = true, sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes", payeOperated = "yes"))))
          )
        )
      )

      val result = await(adrSubmission.generateSubmission()(request, hc, CSOP.metadataWithAllAmmends))
      result-("acknowledgementReference") shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"CSOP",
                                                                |"schemeReference":"XA1100000000000",
                                                                |"taxYear":"2015/16",
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
                                                                |"grant":{
                                                                |"grants":[
                                                                |{
                                                                |"dateOfGrant":"2015-12-09",
                                                                |"numberOfIndividuals":123456,
                                                                |"numberOfSharesGrantedOver":50.6,
                                                                |"umvPerShareUsedToDetermineTheExPrice":10.9821,
                                                                |"exercisePricePerShare":8.2587,
                                                                |"sharesListedOnSE":true,
                                                                |"employeeHoldSharesGreaterThan30K":false
                                                                |}
                                                                |]
                                                                |},
                                                                |"optionsExercisedInYear":true,
                                                                |"exercised":{
                                                                |"exercisedEvents":[
                                                                |{
                                                                |"dateOfExercise":"2015-12-09",
                                                                |"individual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"dateOfGrant":"2015-12-10",
                                                                |"numberSharesAcquired":100.0,
                                                                |"sharesPartOfLargestClass":true,
                                                                |"sharesListedOnSE":true,
                                                                |"amvPerShareAtAcquisitionDate":10.1234,
                                                                |"exerciseValuePerShare":10.1234,
                                                                |"umvPerShareAtExerciseDate":10.1234,
                                                                |"qualifyForTaxRelief":true,
                                                                |"payeOperatedApplied":true,
                                                                |"deductibleAmount":10.1234,
                                                                |"nicsElectionAgreementEnteredInto":true,
                                                                |"sharesDisposedOnSameDay":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"optionsReleasedExchangesCancelledLapsedInYear":true,
                                                                |"released":{
                                                                |"releasedEvents":[
                                                                |{
                                                                |"dateOfEvent":"2015-12-09",
                                                                |"wasMoneyOrValueGiven":true,
                                                                |"amtOrValue":10.9821,
                                                                |"releasedIndividual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"payeOperatedApplied":false
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
                                                                |"declarationPart2_8Schedule4":"schedule-4",
                                                                |"numberSharesWithNewOptionsGrantedInYear":0,
                                                                |"numberIndividualsGrantedNewOptions":0,
                                                                |"numberSharesIssuedTransferredOnExerciseOfOptionsDuringTheYear":0,
                                                                |"numberParticipantsWhoExercisedAllOptionsInTheYear":0,
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

    "create valid json for not NilReturn with participants, ammends, OptinsGranted, Released and OptionsExercised from 2 records for each sheet" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(
          List(
            SchemeData(CSOP.schemeInfo, "CSOP_OptionsGranted_V4", None, Some(ListBuffer(CSOP.buildGrantedV4("yes", "yes")))),
            SchemeData(CSOP.schemeInfo, "CSOP_OptionsGranted_V4", None, Some(ListBuffer(CSOP.buildGrantedV4("no", "no")))),
            SchemeData(CSOP.schemeInfo, "CSOP_OptionsRCL_V4", None, Some(ListBuffer(CSOP.buildOptionsRCL(true, "yes")))),
            SchemeData(CSOP.schemeInfo, "CSOP_OptionsRCL_V4", None, Some(ListBuffer(CSOP.buildOptionsRCL(true, "no")))),
            SchemeData(CSOP.schemeInfo, "CSOP_OptionsExercised_V4", None, Some(ListBuffer(CSOP.buildOptionsExercised(withAllFields = true, sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes", payeOperated = "yes")))),
            SchemeData(CSOP.schemeInfo, "CSOP_OptionsExercised_V4", None, Some(ListBuffer(CSOP.buildOptionsExercised(withAllFields = true, sharesListedOnSE = "no", marketValueAgreedHMRC = "yes", payeOperated = "yes"))))
          )
        )
      )

      val result = await(adrSubmission.generateSubmission()(request, hc, CSOP.metadataWithAllAmmends))
      result-("acknowledgementReference") shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"CSOP",
                                                                |"schemeReference":"XA1100000000000",
                                                                |"taxYear":"2015/16",
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
                                                                |"grant":{
                                                                |"grants":[
                                                                |{
                                                                |"dateOfGrant":"2015-12-09",
                                                                |"numberOfIndividuals":123456,
                                                                |"numberOfSharesGrantedOver":50.6,
                                                                |"umvPerShareUsedToDetermineTheExPrice":10.9821,
                                                                |"exercisePricePerShare":8.2587,
                                                                |"sharesListedOnSE":true,
                                                                |"employeeHoldSharesGreaterThan30K":false
                                                                |},
                                                                |{
                                                                |"dateOfGrant":"2015-12-09",
                                                                |"numberOfIndividuals":123456,
                                                                |"numberOfSharesGrantedOver":50.6,
                                                                |"umvPerShareUsedToDetermineTheExPrice":10.9821,
                                                                |"exercisePricePerShare":8.2587,
                                                                |"sharesListedOnSE":false,
                                                                |"mvAgreedHMRC":false,
                                                                |"employeeHoldSharesGreaterThan30K":false
                                                                |}
                                                                |]
                                                                |},
                                                                |"optionsExercisedInYear":true,
                                                                |"exercised":{
                                                                |"exercisedEvents":[
                                                                |{
                                                                |"dateOfExercise":"2015-12-09",
                                                                |"individual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"dateOfGrant":"2015-12-10",
                                                                |"numberSharesAcquired":100.0,
                                                                |"sharesPartOfLargestClass":true,
                                                                |"sharesListedOnSE":true,
                                                                |"amvPerShareAtAcquisitionDate":10.1234,
                                                                |"exerciseValuePerShare":10.1234,
                                                                |"umvPerShareAtExerciseDate":10.1234,
                                                                |"qualifyForTaxRelief":true,
                                                                |"payeOperatedApplied":true,
                                                                |"deductibleAmount":10.1234,
                                                                |"nicsElectionAgreementEnteredInto":true,
                                                                |"sharesDisposedOnSameDay":true
                                                                |},
                                                                |{
                                                                |"dateOfExercise":"2015-12-09",
                                                                |"individual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"dateOfGrant":"2015-12-10",
                                                                |"numberSharesAcquired":100.0,
                                                                |"sharesPartOfLargestClass":true,
                                                                |"sharesListedOnSE":false,
                                                                |"mvAgreedHMRC":true,
                                                                |"hmrcRef":"aa12345678",
                                                                |"amvPerShareAtAcquisitionDate":10.1234,
                                                                |"exerciseValuePerShare":10.1234,
                                                                |"umvPerShareAtExerciseDate":10.1234,
                                                                |"qualifyForTaxRelief":true,
                                                                |"payeOperatedApplied":true,
                                                                |"deductibleAmount":10.1234,
                                                                |"nicsElectionAgreementEnteredInto":true,
                                                                |"sharesDisposedOnSameDay":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"optionsReleasedExchangesCancelledLapsedInYear":true,
                                                                |"released":{
                                                                |"releasedEvents":[
                                                                |{
                                                                |"dateOfEvent":"2015-12-09",
                                                                |"wasMoneyOrValueGiven":true,
                                                                |"amtOrValue":10.9821,
                                                                |"releasedIndividual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"payeOperatedApplied":false
                                                                |},
                                                                |{
                                                                |"dateOfEvent":"2015-12-09",
                                                                |"wasMoneyOrValueGiven":false,
                                                                |"releasedIndividual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"payeOperatedApplied":false
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
                                                                |"declarationPart2_8Schedule4":"schedule-4",
                                                                |"numberSharesWithNewOptionsGrantedInYear":0,
                                                                |"numberIndividualsGrantedNewOptions":0,
                                                                |"numberSharesIssuedTransferredOnExerciseOfOptionsDuringTheYear":0,
                                                                |"numberParticipantsWhoExercisedAllOptionsInTheYear":0,
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

  }

  // CSOP_OptionsGranted_V4
  "calling generateJson for OptionsGranted_V4" should {

    val configData: Config = Common.loadConfiguration(CSOP.schemeType, "CSOP_OptionsGranted_V4", mockConfigUtils)

    "create valid JSON with sharesListedOnSE = (\"yes\" or \"no\"), marketValueAgreedHMRC = \"yes\"" in {

      val result = adrSubmission.buildJson(
        configData,
        ListBuffer(
          CSOP.buildGrantedV4(sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes"),
          CSOP.buildGrantedV4(sharesListedOnSE = "no", marketValueAgreedHMRC = "yes")
        )
      )

      result shouldBe Json.parse("""{
                                   |"optionsGrantedInYear":true,
                                   |"grant":{
                                   |"grants":[
                                   |{
                                   |"dateOfGrant":"2015-12-09",
                                   |"numberOfIndividuals":123456,
                                   |"numberOfSharesGrantedOver":50.6,
                                   |"umvPerShareUsedToDetermineTheExPrice":10.9821,
                                   |"exercisePricePerShare":8.2587,
                                   |"sharesListedOnSE":true,
                                   |"employeeHoldSharesGreaterThan30K":false
                                   |},
                                   |{
                                   |"dateOfGrant":"2015-12-09",
                                   |"numberOfIndividuals":123456,
                                   |"numberOfSharesGrantedOver":50.6,
                                   |"umvPerShareUsedToDetermineTheExPrice":10.9821,
                                   |"exercisePricePerShare":8.2587,
                                   |"sharesListedOnSE":false,
                                   |"mvAgreedHMRC":true,
                                   |"hmrcRef":"aa12345678",
                                   |"employeeHoldSharesGreaterThan30K":false
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON with sharesListedOnSE = \"no\", marketValueAgreedHMRC = (\"yes\" or \"no\")" in {

      val result = adrSubmission.buildJson(
        configData,
        ListBuffer(
          CSOP.buildGrantedV4(sharesListedOnSE = "no", marketValueAgreedHMRC = "yes"),
          CSOP.buildGrantedV4(sharesListedOnSE = "no", marketValueAgreedHMRC = "no")
        )
      )

      result shouldBe Json.parse("""{
                                   |"optionsGrantedInYear":true,
                                   |"grant":{
                                   |"grants":[
                                   |{
                                   |"dateOfGrant":"2015-12-09",
                                   |"numberOfIndividuals":123456,
                                   |"numberOfSharesGrantedOver":50.6,
                                   |"umvPerShareUsedToDetermineTheExPrice":10.9821,
                                   |"exercisePricePerShare":8.2587,
                                   |"sharesListedOnSE":false,
                                   |"mvAgreedHMRC":true,
                                   |"hmrcRef":"aa12345678",
                                   |"employeeHoldSharesGreaterThan30K":false
                                   |},
                                   |{
                                   |"dateOfGrant":"2015-12-09",
                                   |"numberOfIndividuals":123456,
                                   |"numberOfSharesGrantedOver":50.6,
                                   |"umvPerShareUsedToDetermineTheExPrice":10.9821,
                                   |"exercisePricePerShare":8.2587,
                                   |"sharesListedOnSE":false,
                                   |"mvAgreedHMRC":false,
                                   |"employeeHoldSharesGreaterThan30K":false
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }
  }

  // CSOP_OptionsRCL_V4
  "calling generateJson for OptionsRCL_V4" should {

    val configData: Config = Common.loadConfiguration(CSOP.schemeType, "CSOP_OptionsRCL_V4", mockConfigUtils)

    "create valid JSON with withAllFields = (true or false), moneyExchanged = \"yes\"" in {

      val result = adrSubmission.buildJson(
        configData,
        ListBuffer(
          CSOP.buildOptionsRCL(withAllFields = true, moneyExchanged = "yes"),
          CSOP.buildOptionsRCL(withAllFields = false, moneyExchanged = "yes")
        )
      )

      result shouldBe Json.parse("""{
                                   |"optionsReleasedExchangesCancelledLapsedInYear":true,
                                   |"released":{
                                   |"releasedEvents":[
                                   |{
                                   |"dateOfEvent":"2015-12-09",
                                   |"wasMoneyOrValueGiven":true,
                                   |"amtOrValue":10.9821,
                                   |"releasedIndividual":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"payeOperatedApplied":false
                                   |},
                                   |{
                                   |"dateOfEvent":"2015-12-09",
                                   |"wasMoneyOrValueGiven":true,
                                   |"amtOrValue":10.9821,
                                   |"releasedIndividual":{
                                   |"firstName":"First",
                                   |"surname":"Last",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"payeOperatedApplied":false
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON with withAllFields = true, moneyExchanged = (\"yes\" or \"no\")" in {

      val result = adrSubmission.buildJson(
        configData,
        ListBuffer(
          CSOP.buildOptionsRCL(withAllFields = true, moneyExchanged = "yes"),
          CSOP.buildOptionsRCL(withAllFields = true, moneyExchanged = "no")
        )
      )

      result shouldBe Json.parse("""{
                                   |"optionsReleasedExchangesCancelledLapsedInYear":true,
                                   |"released":{
                                   |"releasedEvents":[
                                   |{
                                   |"dateOfEvent":"2015-12-09",
                                   |"wasMoneyOrValueGiven":true,
                                   |"amtOrValue":10.9821,
                                   |"releasedIndividual":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"payeOperatedApplied":false
                                   |},
                                   |{
                                   |"dateOfEvent":"2015-12-09",
                                   |"wasMoneyOrValueGiven":false,
                                   |"releasedIndividual":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"payeOperatedApplied":false
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }
  }

  // CSOP_OptionsExercised_V4
  "calling generateJson for OptionsExercised_V4" should {

    val configData: Config = Common.loadConfiguration(CSOP.schemeType, "CSOP_OptionsExercised_V4", mockConfigUtils)

    "create valid JSON with withAllFields = (true or false), sharesListedOnSE = \"yes\", marketValueAgreedHMRC = \"yes\", payeOperated = \"yes\"" in {

      val result = adrSubmission.buildJson(
        configData,
        ListBuffer(
          CSOP.buildOptionsExercised(withAllFields = true, sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes", payeOperated = "yes"),
          CSOP.buildOptionsExercised(withAllFields = false, sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes", payeOperated = "yes")
        )
      )

      result shouldBe Json.parse("""{
                                   |"optionsExercisedInYear":true,
                                   |"exercised":{
                                   |"exercisedEvents":[
                                   |{
                                   |"dateOfExercise":"2015-12-09",
                                   |"individual":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateOfGrant":"2015-12-10",
                                   |"numberSharesAcquired":100.0,
                                   |"sharesPartOfLargestClass":true,
                                   |"sharesListedOnSE":true,
                                   |"amvPerShareAtAcquisitionDate":10.1234,
                                   |"exerciseValuePerShare":10.1234,
                                   |"umvPerShareAtExerciseDate":10.1234,
                                   |"qualifyForTaxRelief":true,
                                   |"payeOperatedApplied":true,
                                   |"deductibleAmount":10.1234,
                                   |"nicsElectionAgreementEnteredInto":true,
                                   |"sharesDisposedOnSameDay":true
                                   |},
                                   |{
                                   |"dateOfExercise":"2015-12-09",
                                   |"individual":{
                                   |"firstName":"First",
                                   |"surname":"Last",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateOfGrant":"2015-12-10",
                                   |"numberSharesAcquired":100.0,
                                   |"sharesPartOfLargestClass":true,
                                   |"sharesListedOnSE":true,
                                   |"amvPerShareAtAcquisitionDate":10.1234,
                                   |"exerciseValuePerShare":10.1234,
                                   |"umvPerShareAtExerciseDate":10.1234,
                                   |"qualifyForTaxRelief":true,
                                   |"payeOperatedApplied":true,
                                   |"deductibleAmount":10.1234,
                                   |"nicsElectionAgreementEnteredInto":true,
                                   |"sharesDisposedOnSameDay":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)

    }

    "create valid JSON with withAllFields = true, sharesListedOnSE = (\"yes\" or \"no\"), marketValueAgreedHMRC = \"yes\", payeOperated = \"yes\"" in {

      val result = adrSubmission.buildJson(
        configData,
        ListBuffer(
          CSOP.buildOptionsExercised(withAllFields = true, sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes", payeOperated = "yes"),
          CSOP.buildOptionsExercised(withAllFields = true, sharesListedOnSE = "no", marketValueAgreedHMRC = "yes", payeOperated = "yes")
        )
      )

      result shouldBe Json.parse("""{
                                   |"optionsExercisedInYear":true,
                                   |"exercised":{
                                   |"exercisedEvents":[
                                   |{
                                   |"dateOfExercise":"2015-12-09",
                                   |"individual":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateOfGrant":"2015-12-10",
                                   |"numberSharesAcquired":100.0,
                                   |"sharesPartOfLargestClass":true,
                                   |"sharesListedOnSE":true,
                                   |"amvPerShareAtAcquisitionDate":10.1234,
                                   |"exerciseValuePerShare":10.1234,
                                   |"umvPerShareAtExerciseDate":10.1234,
                                   |"qualifyForTaxRelief":true,
                                   |"payeOperatedApplied":true,
                                   |"deductibleAmount":10.1234,
                                   |"nicsElectionAgreementEnteredInto":true,
                                   |"sharesDisposedOnSameDay":true
                                   |},
                                   |{
                                   |"dateOfExercise":"2015-12-09",
                                   |"individual":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateOfGrant":"2015-12-10",
                                   |"numberSharesAcquired":100.0,
                                   |"sharesPartOfLargestClass":true,
                                   |"sharesListedOnSE":false,
                                   |"mvAgreedHMRC":true,
                                   |"hmrcRef":"aa12345678",
                                   |"amvPerShareAtAcquisitionDate":10.1234,
                                   |"exerciseValuePerShare":10.1234,
                                   |"umvPerShareAtExerciseDate":10.1234,
                                   |"qualifyForTaxRelief":true,
                                   |"payeOperatedApplied":true,
                                   |"deductibleAmount":10.1234,
                                   |"nicsElectionAgreementEnteredInto":true,
                                   |"sharesDisposedOnSameDay":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)

    }

    "create valid JSON with withAllFields = true, sharesListedOnSE = \"no\", marketValueAgreedHMRC = (\"yes\" or \"no\"), payeOperated = \"yes\"" in {

      val result = adrSubmission.buildJson(
        configData,
        ListBuffer(
          CSOP.buildOptionsExercised(withAllFields = true, sharesListedOnSE = "no", marketValueAgreedHMRC = "yes", payeOperated = "yes"),
          CSOP.buildOptionsExercised(withAllFields = true, sharesListedOnSE = "no", marketValueAgreedHMRC = "no", payeOperated = "yes")
        )
      )

      result shouldBe Json.parse("""{
                                   |"optionsExercisedInYear":true,
                                   |"exercised":{
                                   |"exercisedEvents":[
                                   |{
                                   |"dateOfExercise":"2015-12-09",
                                   |"individual":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateOfGrant":"2015-12-10",
                                   |"numberSharesAcquired":100.0,
                                   |"sharesPartOfLargestClass":true,
                                   |"sharesListedOnSE":false,
                                   |"mvAgreedHMRC":true,
                                   |"hmrcRef":"aa12345678",
                                   |"amvPerShareAtAcquisitionDate":10.1234,
                                   |"exerciseValuePerShare":10.1234,
                                   |"umvPerShareAtExerciseDate":10.1234,
                                   |"qualifyForTaxRelief":true,
                                   |"payeOperatedApplied":true,
                                   |"deductibleAmount":10.1234,
                                   |"nicsElectionAgreementEnteredInto":true,
                                   |"sharesDisposedOnSameDay":true
                                   |},
                                   |{
                                   |"dateOfExercise":"2015-12-09",
                                   |"individual":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateOfGrant":"2015-12-10",
                                   |"numberSharesAcquired":100.0,
                                   |"sharesPartOfLargestClass":true,
                                   |"sharesListedOnSE":false,
                                   |"mvAgreedHMRC":false,
                                   |"amvPerShareAtAcquisitionDate":10.1234,
                                   |"exerciseValuePerShare":10.1234,
                                   |"umvPerShareAtExerciseDate":10.1234,
                                   |"qualifyForTaxRelief":true,
                                   |"payeOperatedApplied":true,
                                   |"deductibleAmount":10.1234,
                                   |"nicsElectionAgreementEnteredInto":true,
                                   |"sharesDisposedOnSameDay":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)

    }

    "create valid JSON with withAllFields = true, sharesListedOnSE = \"no\", marketValueAgreedHMRC = \"yes\", payeOperated = (\"yes\" or \"no\")" in {

      val result = adrSubmission.buildJson(
        configData,
        ListBuffer(
          CSOP.buildOptionsExercised(withAllFields = true, sharesListedOnSE = "no", marketValueAgreedHMRC = "yes", payeOperated = "yes"),
          CSOP.buildOptionsExercised(withAllFields = true, sharesListedOnSE = "no", marketValueAgreedHMRC = "yes", payeOperated = "no")
        )
      )

      result shouldBe Json.parse("""{
                                   |"optionsExercisedInYear":true,
                                   |"exercised":{
                                   |"exercisedEvents":[
                                   |{
                                   |"dateOfExercise":"2015-12-09",
                                   |"individual":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateOfGrant":"2015-12-10",
                                   |"numberSharesAcquired":100.0,
                                   |"sharesPartOfLargestClass":true,
                                   |"sharesListedOnSE":false,
                                   |"mvAgreedHMRC":true,
                                   |"hmrcRef":"aa12345678",
                                   |"amvPerShareAtAcquisitionDate":10.1234,
                                   |"exerciseValuePerShare":10.1234,
                                   |"umvPerShareAtExerciseDate":10.1234,
                                   |"qualifyForTaxRelief":true,
                                   |"payeOperatedApplied":true,
                                   |"deductibleAmount":10.1234,
                                   |"nicsElectionAgreementEnteredInto":true,
                                   |"sharesDisposedOnSameDay":true
                                   |},
                                   |{
                                   |"dateOfExercise":"2015-12-09",
                                   |"individual":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"dateOfGrant":"2015-12-10",
                                   |"numberSharesAcquired":100.0,
                                   |"sharesPartOfLargestClass":true,
                                   |"sharesListedOnSE":false,
                                   |"mvAgreedHMRC":true,
                                   |"hmrcRef":"aa12345678",
                                   |"amvPerShareAtAcquisitionDate":10.1234,
                                   |"exerciseValuePerShare":10.1234,
                                   |"umvPerShareAtExerciseDate":10.1234,
                                   |"qualifyForTaxRelief":true,
                                   |"payeOperatedApplied":false,
                                   |"nicsElectionAgreementEnteredInto":true,
                                   |"sharesDisposedOnSameDay":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)

    }

  }
}
