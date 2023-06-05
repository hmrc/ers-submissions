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
import fixtures.{Common, Fixtures, SIP}
import helpers.ERSTestHelper
import models.{SchemeData, SchemeInfo}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import play.api.libs.json.Json
import play.api.test.FakeRequest
import services.PresubmissionService
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggingAndRexceptions.ADRExceptionEmitter
import utils.{ADRSubmission, ConfigUtils, SubmissionCommon}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class SIP_ADRSubmissionSpec extends ERSTestHelper with BeforeAndAfter {

  implicit val hc: HeaderCarrier = new HeaderCarrier()
  implicit val request = FakeRequest().withBody(Fixtures.metadataJson)

  val mockSubmissionCommon: SubmissionCommon = app.injector.instanceOf[SubmissionCommon]
  val mockPresubmissionService: PresubmissionService = mock[PresubmissionService]
  val mockAdrExceptionEmitter: ADRExceptionEmitter = mock[ADRExceptionEmitter]
  val mockConfigUtils: ConfigUtils = app.injector.instanceOf[ConfigUtils]

  val mockAdrSubmission: ADRSubmission = new ADRSubmission(
    mockSubmissionCommon,
    mockPresubmissionService,
    mockAdrExceptionEmitter,
    mockConfigUtils
  )
  def before(fun : => scala.Any): Unit = {
    super.before(())
    reset(mockPresubmissionService)
  }

  "calling generateSubmissionReturn" should {

    "create valid NilReturn" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())(any())
      ).thenReturn(
        Future.successful(List())
      )

      val result = await(mockAdrSubmission.generateSubmission()(request, hc, SIP.metadataNilReturn))
      result - "acknowledgementReference" shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"SIP",
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
                                                                |"sharesAcquiredOrAwardedInYear":false,
                                                                |"sharesComeOutOfThePlanInYear":false,
                                                                |"alterationsAmendmentsMadeInYear":false,
                                                                |"declarationAmendmentsMadeSchedule2Continue":" ",
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

    "create valid NilReturn with some ammends" in {

      when(mockPresubmissionService.getJson(any[SchemeInfo]())(any())).thenReturn(Future.successful(List()))

      val result = await(mockAdrSubmission.generateSubmission()(request, hc, SIP.metadataNilReturnWithSomeAltAmmends))
      result - "acknowledgementReference" shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"SIP",
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
                                                                |"sharesAcquiredOrAwardedInYear":false,
                                                                |"sharesComeOutOfThePlanInYear":false,
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
                                                                |"declarationAmendmentsMadeSchedule2Continue":" ",
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

    "create valid NilReturn with all ammends" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())(any())
      ).thenReturn(
        Future.successful(List())
      )

      val result = await(mockAdrSubmission.generateSubmission()(request, hc, SIP.metadataNilReturnWithAllAltAmmends))
      result - "acknowledgementReference" shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"SIP",
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
                                                                |"sharesAcquiredOrAwardedInYear":false,
                                                                |"sharesComeOutOfThePlanInYear":false,
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
                                                                |"declarationAmendmentsMadeSchedule2Continue":" ",
                                                                |"declaration":"declaration"
                                                                |}
                                                                |}""".stripMargin)
    }

    "create valid not NilReturn with participants and trustees without data" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())(any())
      ).thenReturn(
        Future.successful(List())
      )

      val result = await(mockAdrSubmission.generateSubmission()(request, hc, SIP.metadata))
      result - "acknowledgementReference" shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"SIP",
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
                                                                |"sharesAcquiredOrAwardedInYear":false,
                                                                |"sharesComeOutOfThePlanInYear":false,
                                                                |"alterationsAmendmentsMadeInYear":false,
                                                                |"declarationAmendmentsMadeSchedule2Continue":" ",
                                                                |"declaration":"declaration",
                                                                |"trustee":{
                                                                |"trustees":[
                                                                |{
                                                                |"trusteeName":"testCompany",
                                                                |"trusteeAddress":{
                                                                |"addressLine1":"testAddress1",
                                                                |"addressLine2":"testAddress2",
                                                                |"addressLine3":"testAddress3",
                                                                |"addressLine4":"testAddress4",
                                                                |"country":"UK",
                                                                |"postCode":"NE1 1AA"
                                                                |}
                                                                |},
                                                                |{
                                                                |"trusteeName":"testCompany",
                                                                |"trusteeAddress":{
                                                                |"addressLine1":"testAddress1"
                                                                |}
                                                                |}
                                                                |]
                                                                |}
                                                                |}
                                                                |}""".stripMargin)

    }

    "create valid not NilReturn with Awards_V4, participants and trustees without data" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())(any())
      ).thenReturn(
        Future.successful(
          List(
            SchemeData(SIP.schemeInfo, "SIP_Awards_V4", None, Some(ListBuffer(SIP.buildAwards(true, "yes", "yes"))))
          )
        )
      )

      val result = await(mockAdrSubmission.generateSubmission()(request, hc, SIP.metadata))
      result - "acknowledgementReference" shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"SIP",
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
                                                                |"sharesAcquiredOrAwardedInYear":true,
                                                                |"award":{
                                                                |"awards":[
                                                                |{
                                                                |"dateOfEvent":"2015-12-09",
                                                                |"numberOfIndividualsAwardedShares":1000,
                                                                |"awarded":{
                                                                |"awardedEvents":[
                                                                |{
                                                                |"typeOfAward":"2",
                                                                |"freePerformanceConditions":false,
                                                                |"matchingRatio":"2/1",
                                                                |"marketValuePerShareOnAcquisitionOrAward":10.1234,
                                                                |"totalNumberOfSharesAwarded":100.0,
                                                                |"totalValueOfSharesAwarded":10.1234,
                                                                |"totalFreeAwardsPerEmployeeGreaterThan3600":1000.0,
                                                                |"totalFreeAwardsPerEmployeeAtLimitOf3000":1000.0,
                                                                |"totalPartnershipAwardsPerEmployeeGreaterThan1800":1000.0,
                                                                |"totalPartnershipAwardsPerEmployeeAtLimitOf1500":1000.0,
                                                                |"totalMatchingAwardsPerEmployeeGreaterThan3600":1000.0,
                                                                |"totalMatchingAwardsPerEmployeeAtLimitOf3000":100.0,
                                                                |"sharesListedOnSE":true
                                                                |}
                                                                |]
                                                                |}
                                                                |}
                                                                |]
                                                                |},
                                                                |"sharesComeOutOfThePlanInYear":false,
                                                                |"alterationsAmendmentsMadeInYear":false,
                                                                |"declarationAmendmentsMadeSchedule2Continue":" ",
                                                                |"declaration":"declaration",
                                                                |"trustee":{
                                                                |"trustees":[
                                                                |{
                                                                |"trusteeName":"testCompany",
                                                                |"trusteeAddress":{
                                                                |"addressLine1":"testAddress1",
                                                                |"addressLine2":"testAddress2",
                                                                |"addressLine3":"testAddress3",
                                                                |"addressLine4":"testAddress4",
                                                                |"country":"UK",
                                                                |"postCode":"NE1 1AA"
                                                                |}
                                                                |},
                                                                |{
                                                                |"trusteeName":"testCompany",
                                                                |"trusteeAddress":{
                                                                |"addressLine1":"testAddress1"
                                                                |}
                                                                |}
                                                                |]
                                                                |}
                                                                |}
                                                                |}""".stripMargin)

    }

    "create valid not NilReturn with Awards_V4, participants and trustees without data from 2 records of sheet data" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())(any())
      ).thenReturn(
        Future.successful(
          List(
            SchemeData(SIP.schemeInfo, "SIP_Awards_V4", None, Some(ListBuffer(SIP.buildAwards(true, "yes", "yes")))),
            SchemeData(SIP.schemeInfo, "SIP_Awards_V4", None, Some(ListBuffer(SIP.buildAwards(true, "yes", "yes"))))
          )
        )
      )

      val result = await(mockAdrSubmission.generateSubmission()(request, hc, SIP.metadata))
      result - "acknowledgementReference" shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"SIP",
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
                                                                |"sharesAcquiredOrAwardedInYear":true,
                                                                |"award":{
                                                                |"awards":[
                                                                |{
                                                                |"dateOfEvent":"2015-12-09",
                                                                |"numberOfIndividualsAwardedShares":1000,
                                                                |"awarded":{
                                                                |"awardedEvents":[
                                                                |{
                                                                |"typeOfAward":"2",
                                                                |"freePerformanceConditions":false,
                                                                |"matchingRatio":"2/1",
                                                                |"marketValuePerShareOnAcquisitionOrAward":10.1234,
                                                                |"totalNumberOfSharesAwarded":100.0,
                                                                |"totalValueOfSharesAwarded":10.1234,
                                                                |"totalFreeAwardsPerEmployeeGreaterThan3600":1000.0,
                                                                |"totalFreeAwardsPerEmployeeAtLimitOf3000":1000.0,
                                                                |"totalPartnershipAwardsPerEmployeeGreaterThan1800":1000.0,
                                                                |"totalPartnershipAwardsPerEmployeeAtLimitOf1500":1000.0,
                                                                |"totalMatchingAwardsPerEmployeeGreaterThan3600":1000.0,
                                                                |"totalMatchingAwardsPerEmployeeAtLimitOf3000":100.0,
                                                                |"sharesListedOnSE":true
                                                                |}
                                                                |]
                                                                |}
                                                                |},
                                                                |{
                                                                |"dateOfEvent":"2015-12-09",
                                                                |"numberOfIndividualsAwardedShares":1000,
                                                                |"awarded":{
                                                                |"awardedEvents":[
                                                                |{
                                                                |"typeOfAward":"2",
                                                                |"freePerformanceConditions":false,
                                                                |"matchingRatio":"2/1",
                                                                |"marketValuePerShareOnAcquisitionOrAward":10.1234,
                                                                |"totalNumberOfSharesAwarded":100.0,
                                                                |"totalValueOfSharesAwarded":10.1234,
                                                                |"totalFreeAwardsPerEmployeeGreaterThan3600":1000.0,
                                                                |"totalFreeAwardsPerEmployeeAtLimitOf3000":1000.0,
                                                                |"totalPartnershipAwardsPerEmployeeGreaterThan1800":1000.0,
                                                                |"totalPartnershipAwardsPerEmployeeAtLimitOf1500":1000.0,
                                                                |"totalMatchingAwardsPerEmployeeGreaterThan3600":1000.0,
                                                                |"totalMatchingAwardsPerEmployeeAtLimitOf3000":100.0,
                                                                |"sharesListedOnSE":true
                                                                |}
                                                                |]
                                                                |}
                                                                |}
                                                                |]
                                                                |},
                                                                |"sharesComeOutOfThePlanInYear":false,
                                                                |"alterationsAmendmentsMadeInYear":false,
                                                                |"declarationAmendmentsMadeSchedule2Continue":" ",
                                                                |"declaration":"declaration",
                                                                |"trustee":{
                                                                |"trustees":[
                                                                |{
                                                                |"trusteeName":"testCompany",
                                                                |"trusteeAddress":{
                                                                |"addressLine1":"testAddress1",
                                                                |"addressLine2":"testAddress2",
                                                                |"addressLine3":"testAddress3",
                                                                |"addressLine4":"testAddress4",
                                                                |"country":"UK",
                                                                |"postCode":"NE1 1AA"
                                                                |}
                                                                |},
                                                                |{
                                                                |"trusteeName":"testCompany",
                                                                |"trusteeAddress":{
                                                                |"addressLine1":"testAddress1"
                                                                |}
                                                                |}
                                                                |]
                                                                |}
                                                                |}
                                                                |}""".stripMargin)

    }

    "create valid not NilReturn with Out_V4, participants and trustees without data" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())(any())
      ).thenReturn(
        Future.successful(
          List(
            SchemeData(SIP.schemeInfo, "SIP_Out_V4", None, Some(ListBuffer(SIP.buildOutOfPlan(true, "yes", "yes"))))
          )
        )
      )

      val result = await(mockAdrSubmission.generateSubmission()(request, hc, SIP.metadata))
      result - "acknowledgementReference" shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"SIP",
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
                                                                |"sharesAcquiredOrAwardedInYear":false,
                                                                |"sharesComeOutOfThePlanInYear":true,
                                                                |"outOfPlan":{
                                                                |"outOfPlanEvents":[
                                                                |{
                                                                |"dateOfEvent":"2011-10-13",
                                                                |"individual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfFreeSharesOutOfPlan":100.0,
                                                                |"numberOfPartnershipSharesOutOfPlan":100.0,
                                                                |"numberOfMatchingSharesOutOfPlan":100.0,
                                                                |"numberOfDividendSharesOutOfPlan":100.0,
                                                                |"marketValuePerFreeShare":10.1234,
                                                                |"marketValuePerPartnershipShare":10.1234,
                                                                |"marketValuePerMatchingShare":10.1234,
                                                                |"marketValuePerDividendShare":10.1234,
                                                                |"sharesHeldInPlanForMoreThan5Years":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"alterationsAmendmentsMadeInYear":false,
                                                                |"declarationAmendmentsMadeSchedule2Continue":" ",
                                                                |"declaration":"declaration",
                                                                |"trustee":{
                                                                |"trustees":[
                                                                |{
                                                                |"trusteeName":"testCompany",
                                                                |"trusteeAddress":{
                                                                |"addressLine1":"testAddress1",
                                                                |"addressLine2":"testAddress2",
                                                                |"addressLine3":"testAddress3",
                                                                |"addressLine4":"testAddress4",
                                                                |"country":"UK",
                                                                |"postCode":"NE1 1AA"
                                                                |}
                                                                |},
                                                                |{
                                                                |"trusteeName":"testCompany",
                                                                |"trusteeAddress":{
                                                                |"addressLine1":"testAddress1"
                                                                |}
                                                                |}
                                                                |]
                                                                |}
                                                                |}
                                                                |}""".stripMargin)

    }

    "create valid not NilReturn with Out_V4, participants and trustees without data from 2 records of sheet data" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())(any())
      ).thenReturn(
        Future.successful(
          List(
            SchemeData(SIP.schemeInfo, "SIP_Out_V4", None, Some(ListBuffer(SIP.buildOutOfPlan(true, "yes", "yes")))),
            SchemeData(SIP.schemeInfo, "SIP_Out_V4", None, Some(ListBuffer(SIP.buildOutOfPlan(true, "yes", "yes"))))
          )
        )
      )

      val result = await(mockAdrSubmission.generateSubmission()(request, hc, SIP.metadata))
      result - "acknowledgementReference" shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"SIP",
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
                                                                |"sharesAcquiredOrAwardedInYear":false,
                                                                |"sharesComeOutOfThePlanInYear":true,
                                                                |"outOfPlan":{
                                                                |"outOfPlanEvents":[
                                                                |{
                                                                |"dateOfEvent":"2011-10-13",
                                                                |"individual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfFreeSharesOutOfPlan":100.0,
                                                                |"numberOfPartnershipSharesOutOfPlan":100.0,
                                                                |"numberOfMatchingSharesOutOfPlan":100.0,
                                                                |"numberOfDividendSharesOutOfPlan":100.0,
                                                                |"marketValuePerFreeShare":10.1234,
                                                                |"marketValuePerPartnershipShare":10.1234,
                                                                |"marketValuePerMatchingShare":10.1234,
                                                                |"marketValuePerDividendShare":10.1234,
                                                                |"sharesHeldInPlanForMoreThan5Years":true
                                                                |},
                                                                |{
                                                                |"dateOfEvent":"2011-10-13",
                                                                |"individual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfFreeSharesOutOfPlan":100.0,
                                                                |"numberOfPartnershipSharesOutOfPlan":100.0,
                                                                |"numberOfMatchingSharesOutOfPlan":100.0,
                                                                |"numberOfDividendSharesOutOfPlan":100.0,
                                                                |"marketValuePerFreeShare":10.1234,
                                                                |"marketValuePerPartnershipShare":10.1234,
                                                                |"marketValuePerMatchingShare":10.1234,
                                                                |"marketValuePerDividendShare":10.1234,
                                                                |"sharesHeldInPlanForMoreThan5Years":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"alterationsAmendmentsMadeInYear":false,
                                                                |"declarationAmendmentsMadeSchedule2Continue":" ",
                                                                |"declaration":"declaration",
                                                                |"trustee":{
                                                                |"trustees":[
                                                                |{
                                                                |"trusteeName":"testCompany",
                                                                |"trusteeAddress":{
                                                                |"addressLine1":"testAddress1",
                                                                |"addressLine2":"testAddress2",
                                                                |"addressLine3":"testAddress3",
                                                                |"addressLine4":"testAddress4",
                                                                |"country":"UK",
                                                                |"postCode":"NE1 1AA"
                                                                |}
                                                                |},
                                                                |{
                                                                |"trusteeName":"testCompany",
                                                                |"trusteeAddress":{
                                                                |"addressLine1":"testAddress1"
                                                                |}
                                                                |}
                                                                |]
                                                                |}
                                                                |}
                                                                |}""".stripMargin)

    }

    "create valid not NilReturn with Awards_V4, Out_V4, participants and trustees without data" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())(any())
      ).thenReturn(
        Future.successful(
          List(
            SchemeData(SIP.schemeInfo, "SIP_Awards_V4", None, Some(ListBuffer(SIP.buildAwards(true, "yes", "yes")))),
            SchemeData(SIP.schemeInfo, "SIP_Out_V4", None, Some(ListBuffer(SIP.buildOutOfPlan(true, "yes", "yes"))))
          )
        )
      )

      val result = await(mockAdrSubmission.generateSubmission()(request, hc, SIP.metadata))
      result - "acknowledgementReference" shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"SIP",
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
                                                                |"sharesAcquiredOrAwardedInYear":true,
                                                                |"award":{
                                                                |"awards":[
                                                                |{
                                                                |"dateOfEvent":"2015-12-09",
                                                                |"numberOfIndividualsAwardedShares":1000,
                                                                |"awarded":{
                                                                |"awardedEvents":[
                                                                |{
                                                                |"typeOfAward":"2",
                                                                |"freePerformanceConditions":false,
                                                                |"matchingRatio":"2/1",
                                                                |"marketValuePerShareOnAcquisitionOrAward":10.1234,
                                                                |"totalNumberOfSharesAwarded":100.0,
                                                                |"totalValueOfSharesAwarded":10.1234,
                                                                |"totalFreeAwardsPerEmployeeGreaterThan3600":1000.0,
                                                                |"totalFreeAwardsPerEmployeeAtLimitOf3000":1000.0,
                                                                |"totalPartnershipAwardsPerEmployeeGreaterThan1800":1000.0,
                                                                |"totalPartnershipAwardsPerEmployeeAtLimitOf1500":1000.0,
                                                                |"totalMatchingAwardsPerEmployeeGreaterThan3600":1000.0,
                                                                |"totalMatchingAwardsPerEmployeeAtLimitOf3000":100.0,
                                                                |"sharesListedOnSE":true
                                                                |}
                                                                |]
                                                                |}
                                                                |}
                                                                |]
                                                                |},
                                                                |"sharesComeOutOfThePlanInYear":true,
                                                                |"outOfPlan":{
                                                                |"outOfPlanEvents":[
                                                                |{
                                                                |"dateOfEvent":"2011-10-13",
                                                                |"individual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfFreeSharesOutOfPlan":100.0,
                                                                |"numberOfPartnershipSharesOutOfPlan":100.0,
                                                                |"numberOfMatchingSharesOutOfPlan":100.0,
                                                                |"numberOfDividendSharesOutOfPlan":100.0,
                                                                |"marketValuePerFreeShare":10.1234,
                                                                |"marketValuePerPartnershipShare":10.1234,
                                                                |"marketValuePerMatchingShare":10.1234,
                                                                |"marketValuePerDividendShare":10.1234,
                                                                |"sharesHeldInPlanForMoreThan5Years":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"alterationsAmendmentsMadeInYear":false,
                                                                |"declarationAmendmentsMadeSchedule2Continue":" ",
                                                                |"declaration":"declaration",
                                                                |"trustee":{
                                                                |"trustees":[
                                                                |{
                                                                |"trusteeName":"testCompany",
                                                                |"trusteeAddress":{
                                                                |"addressLine1":"testAddress1",
                                                                |"addressLine2":"testAddress2",
                                                                |"addressLine3":"testAddress3",
                                                                |"addressLine4":"testAddress4",
                                                                |"country":"UK",
                                                                |"postCode":"NE1 1AA"
                                                                |}
                                                                |},
                                                                |{
                                                                |"trusteeName":"testCompany",
                                                                |"trusteeAddress":{
                                                                |"addressLine1":"testAddress1"
                                                                |}
                                                                |}
                                                                |]
                                                                |}
                                                                |}
                                                                |}""".stripMargin)

    }

    "create valid not NilReturn with Awards_V4, Out_V4, participants and trustees without data from 2 records of sheet data" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())(any())
      ).thenReturn(
        Future.successful(
          List(
            SchemeData(SIP.schemeInfo, "SIP_Awards_V4", None, Some(ListBuffer(SIP.buildAwards(true, "yes", "yes")))),
            SchemeData(SIP.schemeInfo, "SIP_Awards_V4", None, Some(ListBuffer(SIP.buildAwards(true, "yes", "yes")))),
            SchemeData(SIP.schemeInfo, "SIP_Out_V4", None, Some(ListBuffer(SIP.buildOutOfPlan(true, "yes", "yes")))),
            SchemeData(SIP.schemeInfo, "SIP_Out_V4", None, Some(ListBuffer(SIP.buildOutOfPlan(true, "yes", "yes"))))
          )
        )
      )

      val result = await(mockAdrSubmission.generateSubmission()(request, hc, SIP.metadata))
      result - "acknowledgementReference" shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"SIP",
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
                                                                |"sharesAcquiredOrAwardedInYear":true,
                                                                |"award":{
                                                                |"awards":[
                                                                |{
                                                                |"dateOfEvent":"2015-12-09",
                                                                |"numberOfIndividualsAwardedShares":1000,
                                                                |"awarded":{
                                                                |"awardedEvents":[
                                                                |{
                                                                |"typeOfAward":"2",
                                                                |"freePerformanceConditions":false,
                                                                |"matchingRatio":"2/1",
                                                                |"marketValuePerShareOnAcquisitionOrAward":10.1234,
                                                                |"totalNumberOfSharesAwarded":100.0,
                                                                |"totalValueOfSharesAwarded":10.1234,
                                                                |"totalFreeAwardsPerEmployeeGreaterThan3600":1000.0,
                                                                |"totalFreeAwardsPerEmployeeAtLimitOf3000":1000.0,
                                                                |"totalPartnershipAwardsPerEmployeeGreaterThan1800":1000.0,
                                                                |"totalPartnershipAwardsPerEmployeeAtLimitOf1500":1000.0,
                                                                |"totalMatchingAwardsPerEmployeeGreaterThan3600":1000.0,
                                                                |"totalMatchingAwardsPerEmployeeAtLimitOf3000":100.0,
                                                                |"sharesListedOnSE":true
                                                                |}
                                                                |]
                                                                |}
                                                                |},
                                                                |{
                                                                |"dateOfEvent":"2015-12-09",
                                                                |"numberOfIndividualsAwardedShares":1000,
                                                                |"awarded":{
                                                                |"awardedEvents":[
                                                                |{
                                                                |"typeOfAward":"2",
                                                                |"freePerformanceConditions":false,
                                                                |"matchingRatio":"2/1",
                                                                |"marketValuePerShareOnAcquisitionOrAward":10.1234,
                                                                |"totalNumberOfSharesAwarded":100.0,
                                                                |"totalValueOfSharesAwarded":10.1234,
                                                                |"totalFreeAwardsPerEmployeeGreaterThan3600":1000.0,
                                                                |"totalFreeAwardsPerEmployeeAtLimitOf3000":1000.0,
                                                                |"totalPartnershipAwardsPerEmployeeGreaterThan1800":1000.0,
                                                                |"totalPartnershipAwardsPerEmployeeAtLimitOf1500":1000.0,
                                                                |"totalMatchingAwardsPerEmployeeGreaterThan3600":1000.0,
                                                                |"totalMatchingAwardsPerEmployeeAtLimitOf3000":100.0,
                                                                |"sharesListedOnSE":true
                                                                |}
                                                                |]
                                                                |}
                                                                |}
                                                                |]
                                                                |},
                                                                |"sharesComeOutOfThePlanInYear":true,
                                                                |"outOfPlan":{
                                                                |"outOfPlanEvents":[
                                                                |{
                                                                |"dateOfEvent":"2011-10-13",
                                                                |"individual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfFreeSharesOutOfPlan":100.0,
                                                                |"numberOfPartnershipSharesOutOfPlan":100.0,
                                                                |"numberOfMatchingSharesOutOfPlan":100.0,
                                                                |"numberOfDividendSharesOutOfPlan":100.0,
                                                                |"marketValuePerFreeShare":10.1234,
                                                                |"marketValuePerPartnershipShare":10.1234,
                                                                |"marketValuePerMatchingShare":10.1234,
                                                                |"marketValuePerDividendShare":10.1234,
                                                                |"sharesHeldInPlanForMoreThan5Years":true
                                                                |},
                                                                |{
                                                                |"dateOfEvent":"2011-10-13",
                                                                |"individual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfFreeSharesOutOfPlan":100.0,
                                                                |"numberOfPartnershipSharesOutOfPlan":100.0,
                                                                |"numberOfMatchingSharesOutOfPlan":100.0,
                                                                |"numberOfDividendSharesOutOfPlan":100.0,
                                                                |"marketValuePerFreeShare":10.1234,
                                                                |"marketValuePerPartnershipShare":10.1234,
                                                                |"marketValuePerMatchingShare":10.1234,
                                                                |"marketValuePerDividendShare":10.1234,
                                                                |"sharesHeldInPlanForMoreThan5Years":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"alterationsAmendmentsMadeInYear":false,
                                                                |"declarationAmendmentsMadeSchedule2Continue":" ",
                                                                |"declaration":"declaration",
                                                                |"trustee":{
                                                                |"trustees":[
                                                                |{
                                                                |"trusteeName":"testCompany",
                                                                |"trusteeAddress":{
                                                                |"addressLine1":"testAddress1",
                                                                |"addressLine2":"testAddress2",
                                                                |"addressLine3":"testAddress3",
                                                                |"addressLine4":"testAddress4",
                                                                |"country":"UK",
                                                                |"postCode":"NE1 1AA"
                                                                |}
                                                                |},
                                                                |{
                                                                |"trusteeName":"testCompany",
                                                                |"trusteeAddress":{
                                                                |"addressLine1":"testAddress1"
                                                                |}
                                                                |}
                                                                |]
                                                                |}
                                                                |}
                                                                |}""".stripMargin)

    }

  }

  // SIP_Awards_V4
  "calling generateJson for Awards_V4" should {
    val configData: Config = Common.loadConfiguration(SIP.schemeType, "SIP_Awards_V4", mockConfigUtils)

    "create valid JSON with withAllFields = (true or false), sharesListedOnSE = \"yes\", marketValueAgreedHMRC = \"yes\"" in {

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          SIP.buildAwards(withAllFields = true, sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes"),
          SIP.buildAwards(withAllFields = false, sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes")
        )
      )

      result shouldBe Json.parse("""{
                                   |"sharesAcquiredOrAwardedInYear":true,
                                   |"award":{
                                   |"awards":[
                                   |{
                                   |"dateOfEvent":"2015-12-09",
                                   |"numberOfIndividualsAwardedShares":1000,
                                   |"awarded":{
                                   |"awardedEvents":[
                                   |{
                                   |"typeOfAward":"2",
                                   |"freePerformanceConditions":false,
                                   |"matchingRatio":"2/1",
                                   |"marketValuePerShareOnAcquisitionOrAward":10.1234,
                                   |"totalNumberOfSharesAwarded":100.0,
                                   |"totalValueOfSharesAwarded":10.1234,
                                   |"totalFreeAwardsPerEmployeeGreaterThan3600":1000.0,
                                   |"totalFreeAwardsPerEmployeeAtLimitOf3000":1000.0,
                                   |"totalPartnershipAwardsPerEmployeeGreaterThan1800":1000.0,
                                   |"totalPartnershipAwardsPerEmployeeAtLimitOf1500":1000.0,
                                   |"totalMatchingAwardsPerEmployeeGreaterThan3600":1000.0,
                                   |"totalMatchingAwardsPerEmployeeAtLimitOf3000":100.0,
                                   |"sharesListedOnSE":true
                                   |}
                                   |]
                                   |}
                                   |},
                                   |{
                                   |"dateOfEvent":"2015-12-09",
                                   |"numberOfIndividualsAwardedShares":1000,
                                   |"awarded":{
                                   |"awardedEvents":[
                                   |{
                                   |"typeOfAward":"2",
                                   |"totalNumberOfSharesAwarded":100.0,
                                   |"totalValueOfSharesAwarded":10.1234,
                                   |"totalFreeAwardsPerEmployeeGreaterThan3600":1000.0,
                                   |"totalFreeAwardsPerEmployeeAtLimitOf3000":1000.0,
                                   |"totalPartnershipAwardsPerEmployeeGreaterThan1800":1000.0,
                                   |"totalPartnershipAwardsPerEmployeeAtLimitOf1500":1000.0,
                                   |"totalMatchingAwardsPerEmployeeGreaterThan3600":1000.0,
                                   |"totalMatchingAwardsPerEmployeeAtLimitOf3000":100.0,
                                   |"sharesListedOnSE":true
                                   |}
                                   |]
                                   |}
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON with withAllFields = true, sharesListedOnSE = (\"yes\" or \"no\"), marketValueAgreedHMRC = \"yes\"" in {

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          SIP.buildAwards(withAllFields = true, sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes"),
          SIP.buildAwards(withAllFields = true, sharesListedOnSE = "no", marketValueAgreedHMRC = "yes")
        )
      )

      result shouldBe Json.parse("""{
                                   |"sharesAcquiredOrAwardedInYear":true,
                                   |"award":{
                                   |"awards":[
                                   |{
                                   |"dateOfEvent":"2015-12-09",
                                   |"numberOfIndividualsAwardedShares":1000,
                                   |"awarded":{
                                   |"awardedEvents":[
                                   |{
                                   |"typeOfAward":"2",
                                   |"freePerformanceConditions":false,
                                   |"matchingRatio":"2/1",
                                   |"marketValuePerShareOnAcquisitionOrAward":10.1234,
                                   |"totalNumberOfSharesAwarded":100.0,
                                   |"totalValueOfSharesAwarded":10.1234,
                                   |"totalFreeAwardsPerEmployeeGreaterThan3600":1000.0,
                                   |"totalFreeAwardsPerEmployeeAtLimitOf3000":1000.0,
                                   |"totalPartnershipAwardsPerEmployeeGreaterThan1800":1000.0,
                                   |"totalPartnershipAwardsPerEmployeeAtLimitOf1500":1000.0,
                                   |"totalMatchingAwardsPerEmployeeGreaterThan3600":1000.0,
                                   |"totalMatchingAwardsPerEmployeeAtLimitOf3000":100.0,
                                   |"sharesListedOnSE":true
                                   |}
                                   |]
                                   |}
                                   |},
                                   |{
                                   |"dateOfEvent":"2015-12-09",
                                   |"numberOfIndividualsAwardedShares":1000,
                                   |"awarded":{
                                   |"awardedEvents":[
                                   |{
                                   |"typeOfAward":"2",
                                   |"freePerformanceConditions":false,
                                   |"matchingRatio":"2/1",
                                   |"marketValuePerShareOnAcquisitionOrAward":10.1234,
                                   |"totalNumberOfSharesAwarded":100.0,
                                   |"totalValueOfSharesAwarded":10.1234,
                                   |"totalFreeAwardsPerEmployeeGreaterThan3600":1000.0,
                                   |"totalFreeAwardsPerEmployeeAtLimitOf3000":1000.0,
                                   |"totalPartnershipAwardsPerEmployeeGreaterThan1800":1000.0,
                                   |"totalPartnershipAwardsPerEmployeeAtLimitOf1500":1000.0,
                                   |"totalMatchingAwardsPerEmployeeGreaterThan3600":1000.0,
                                   |"totalMatchingAwardsPerEmployeeAtLimitOf3000":100.0,
                                   |"sharesListedOnSE":false,
                                   |"marketValueAgreedHMRC":true,
                                   |"hmrcRef":"aa12345678"
                                   |}
                                   |]
                                   |}
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON with withAllFields = true, sharesListedOnSE = \"no\", marketValueAgreedHMRC = (\"yes\" or \"no\")" in {

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          SIP.buildAwards(withAllFields = true, sharesListedOnSE = "no", marketValueAgreedHMRC = "yes"),
          SIP.buildAwards(withAllFields = true, sharesListedOnSE = "no", marketValueAgreedHMRC = "no")
        )
      )

      result shouldBe Json.parse("""{
                                   |"sharesAcquiredOrAwardedInYear":true,
                                   |"award":{
                                   |"awards":[
                                   |{
                                   |"dateOfEvent":"2015-12-09",
                                   |"numberOfIndividualsAwardedShares":1000,
                                   |"awarded":{
                                   |"awardedEvents":[
                                   |{
                                   |"typeOfAward":"2",
                                   |"freePerformanceConditions":false,
                                   |"matchingRatio":"2/1",
                                   |"marketValuePerShareOnAcquisitionOrAward":10.1234,
                                   |"totalNumberOfSharesAwarded":100.0,
                                   |"totalValueOfSharesAwarded":10.1234,
                                   |"totalFreeAwardsPerEmployeeGreaterThan3600":1000.0,
                                   |"totalFreeAwardsPerEmployeeAtLimitOf3000":1000.0,
                                   |"totalPartnershipAwardsPerEmployeeGreaterThan1800":1000.0,
                                   |"totalPartnershipAwardsPerEmployeeAtLimitOf1500":1000.0,
                                   |"totalMatchingAwardsPerEmployeeGreaterThan3600":1000.0,
                                   |"totalMatchingAwardsPerEmployeeAtLimitOf3000":100.0,
                                   |"sharesListedOnSE":false,
                                   |"marketValueAgreedHMRC":true,
                                   |"hmrcRef":"aa12345678"
                                   |}
                                   |]
                                   |}
                                   |},
                                   |{
                                   |"dateOfEvent":"2015-12-09",
                                   |"numberOfIndividualsAwardedShares":1000,
                                   |"awarded":{
                                   |"awardedEvents":[
                                   |{
                                   |"typeOfAward":"2",
                                   |"freePerformanceConditions":false,
                                   |"matchingRatio":"2/1",
                                   |"marketValuePerShareOnAcquisitionOrAward":10.1234,
                                   |"totalNumberOfSharesAwarded":100.0,
                                   |"totalValueOfSharesAwarded":10.1234,
                                   |"totalFreeAwardsPerEmployeeGreaterThan3600":1000.0,
                                   |"totalFreeAwardsPerEmployeeAtLimitOf3000":1000.0,
                                   |"totalPartnershipAwardsPerEmployeeGreaterThan1800":1000.0,
                                   |"totalPartnershipAwardsPerEmployeeAtLimitOf1500":1000.0,
                                   |"totalMatchingAwardsPerEmployeeGreaterThan3600":1000.0,
                                   |"totalMatchingAwardsPerEmployeeAtLimitOf3000":100.0,
                                   |"sharesListedOnSE":false,
                                   |"marketValueAgreedHMRC":false
                                   |}
                                   |]
                                   |}
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

  }

  // SIP_Out_V4
  "calling generateJson for Out_V4" should {
    val configData: Config = Common.loadConfiguration(SIP.schemeType, "SIP_Out_V4", mockConfigUtils)

    "create valid JSON with withAllFields = (true or false), sharesHeld = \"yes\", payeApplied = \"yes\"" in {

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          SIP.buildOutOfPlan(withAllFields = true, sharesHeld = "yes", payeApplied = "yes"),
          SIP.buildOutOfPlan(withAllFields = false, sharesHeld = "yes", payeApplied = "yes")
        )
      )

      result shouldBe Json.parse("""
                                   |{
                                   |"sharesComeOutOfThePlanInYear":true,
                                   |"outOfPlan":{
                                   |"outOfPlanEvents":[
                                   |{
                                   |"dateOfEvent":"2011-10-13",
                                   |"individual":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfFreeSharesOutOfPlan":100.0,
                                   |"numberOfPartnershipSharesOutOfPlan":100.0,
                                   |"numberOfMatchingSharesOutOfPlan":100.0,
                                   |"numberOfDividendSharesOutOfPlan":100.0,
                                   |"marketValuePerFreeShare":10.1234,
                                   |"marketValuePerPartnershipShare":10.1234,
                                   |"marketValuePerMatchingShare":10.1234,
                                   |"marketValuePerDividendShare":10.1234,
                                   |"sharesHeldInPlanForMoreThan5Years":true
                                   |},
                                   |{
                                   |"dateOfEvent":"2011-10-13",
                                   |"individual":{
                                   |"firstName":"First",
                                   |"surname":"Last",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfFreeSharesOutOfPlan":100.0,
                                   |"numberOfPartnershipSharesOutOfPlan":100.0,
                                   |"numberOfMatchingSharesOutOfPlan":100.0,
                                   |"numberOfDividendSharesOutOfPlan":100.0,
                                   |"marketValuePerFreeShare":10.1234,
                                   |"marketValuePerPartnershipShare":10.1234,
                                   |"marketValuePerMatchingShare":10.1234,
                                   |"marketValuePerDividendShare":10.1234,
                                   |"sharesHeldInPlanForMoreThan5Years":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON with withAllFields = true, sharesHeld = (\"yes\" or \"no\"), payeApplied = \"yes\"" in {

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          SIP.buildOutOfPlan(withAllFields = true, sharesHeld = "yes", payeApplied = "yes"),
          SIP.buildOutOfPlan(withAllFields = true, sharesHeld = "no", payeApplied = "yes")
        )
      )

      result shouldBe Json.parse("""
                                   |{
                                   |"sharesComeOutOfThePlanInYear":true,
                                   |"outOfPlan":{
                                   |"outOfPlanEvents":[
                                   |{
                                   |"dateOfEvent":"2011-10-13",
                                   |"individual":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfFreeSharesOutOfPlan":100.0,
                                   |"numberOfPartnershipSharesOutOfPlan":100.0,
                                   |"numberOfMatchingSharesOutOfPlan":100.0,
                                   |"numberOfDividendSharesOutOfPlan":100.0,
                                   |"marketValuePerFreeShare":10.1234,
                                   |"marketValuePerPartnershipShare":10.1234,
                                   |"marketValuePerMatchingShare":10.1234,
                                   |"marketValuePerDividendShare":10.1234,
                                   |"sharesHeldInPlanForMoreThan5Years":true
                                   |},
                                   |{
                                   |"dateOfEvent":"2011-10-13",
                                   |"individual":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfFreeSharesOutOfPlan":100.0,
                                   |"numberOfPartnershipSharesOutOfPlan":100.0,
                                   |"numberOfMatchingSharesOutOfPlan":100.0,
                                   |"numberOfDividendSharesOutOfPlan":100.0,
                                   |"marketValuePerFreeShare":10.1234,
                                   |"marketValuePerPartnershipShare":10.1234,
                                   |"marketValuePerMatchingShare":10.1234,
                                   |"marketValuePerDividendShare":10.1234,
                                   |"sharesHeldInPlanForMoreThan5Years":false,
                                   |"payeOperatedApplied":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON with withAllFields = true, sharesHeld = \"no\", payeApplied = (\"yes\" or \"no\")" in {

      val result = mockAdrSubmission.buildJson(
        configData,
        ListBuffer(
          SIP.buildOutOfPlan(withAllFields = true, sharesHeld = "no", payeApplied = "yes"),
          SIP.buildOutOfPlan(withAllFields = true, sharesHeld = "no", payeApplied = "no")
        )
      )

      result shouldBe Json.parse("""
                                   |{
                                   |"sharesComeOutOfThePlanInYear":true,
                                   |"outOfPlan":{
                                   |"outOfPlanEvents":[
                                   |{
                                   |"dateOfEvent":"2011-10-13",
                                   |"individual":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfFreeSharesOutOfPlan":100.0,
                                   |"numberOfPartnershipSharesOutOfPlan":100.0,
                                   |"numberOfMatchingSharesOutOfPlan":100.0,
                                   |"numberOfDividendSharesOutOfPlan":100.0,
                                   |"marketValuePerFreeShare":10.1234,
                                   |"marketValuePerPartnershipShare":10.1234,
                                   |"marketValuePerMatchingShare":10.1234,
                                   |"marketValuePerDividendShare":10.1234,
                                   |"sharesHeldInPlanForMoreThan5Years":false,
                                   |"payeOperatedApplied":true
                                   |},
                                   |{
                                   |"dateOfEvent":"2011-10-13",
                                   |"individual":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfFreeSharesOutOfPlan":100.0,
                                   |"numberOfPartnershipSharesOutOfPlan":100.0,
                                   |"numberOfMatchingSharesOutOfPlan":100.0,
                                   |"numberOfDividendSharesOutOfPlan":100.0,
                                   |"marketValuePerFreeShare":10.1234,
                                   |"marketValuePerPartnershipShare":10.1234,
                                   |"marketValuePerMatchingShare":10.1234,
                                   |"marketValuePerDividendShare":10.1234,
                                   |"sharesHeldInPlanForMoreThan5Years":false,
                                   |"payeOperatedApplied":false,
                                   |"qualifyForTaxRelief":false
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

  }
}
