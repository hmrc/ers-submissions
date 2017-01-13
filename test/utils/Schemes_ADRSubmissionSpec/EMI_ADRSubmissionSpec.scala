/*
 * Copyright 2017 HM Revenue & Customs
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
import fixtures.{Fixtures, Common, EMI}
import models.{SchemeInfo, SchemeData}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import services.PresubmissionService
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}
import utils.{ConfigUtils, SubmissionCommon, ADRSubmission}
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class EMI_ADRSubmissionSpec extends UnitSpec with MockitoSugar with BeforeAndAfter with WithFakeApplication  {

  implicit val hc: HeaderCarrier = new HeaderCarrier()
  implicit val request = FakeRequest().withBody(Fixtures.metadataJson)

  val mockPresubmissionService: PresubmissionService = mock[PresubmissionService]

  val adrSubmission: ADRSubmission = new ADRSubmission {
    override val presubmissionService: PresubmissionService = mockPresubmissionService
    override val submissionCommon: SubmissionCommon = SubmissionCommon
    override val configUtils: ConfigUtils = ConfigUtils
  }

  def before(fun : => scala.Any) = {
    super.before(())
    reset(mockPresubmissionService)
  }

  "calling generateSubmissionReturn" should {

    "create valid json for NilReturn" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(List())
      )

      val result = await(adrSubmission.generateSubmission()(request, hc, EMI.metadataNilReturn))
      result-("acknowledgementReference") shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"EMI",
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
                                                                |"adjustmentOfOptionsFollowingVariation":false,
                                                                |"optionsReleasedLapsedCancelled":false,
                                                                |"submitANilReturn":true,
                                                                |"groupPlan":true,
                                                                |"numberOfPeopleHoldingOptionsAtYearEnd":0,
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

      val result = await(adrSubmission.generateSubmission()(request, hc, EMI.metadata))
      result-("acknowledgementReference") shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"EMI",
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
                                                                |"adjustmentOfOptionsFollowingVariation":false,
                                                                |"optionsReleasedLapsedCancelled":false,
                                                                |"submitANilReturn":false,
                                                                |"groupPlan":true,
                                                                |"numberOfPeopleHoldingOptionsAtYearEnd":0,
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

    "create valid json for not NilReturn with all sheets and participants" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(
          List(
            SchemeData(EMI.schemeInfo, "EMI40_Adjustments_V3", None, Some(ListBuffer(EMI.buildAdjustmentsV3(true, "yes")))),
            SchemeData(EMI.schemeInfo, "EMI40_Replaced_V3", None, Some(ListBuffer(EMI.buildReplacedV3(true)))),
            SchemeData(EMI.schemeInfo, "EMI40_RLC_V3", None, Some(ListBuffer(EMI.buildRLCV3(true, "yes", "yes")))),
            SchemeData(EMI.schemeInfo, "EMI40_NonTaxable_V3", None, Some(ListBuffer(EMI.buildNonTaxableV3(true, "yes", "yes")))),
            SchemeData(EMI.schemeInfo, "EMI40_Taxable_V3", None, Some(ListBuffer(EMI.buildTaxableV3(true, "yes", "yes", "yes"))))
          )
        )
      )

      val result = await(adrSubmission.generateSubmission()(request, hc, EMI.metadata))
      result-("acknowledgementReference") shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"EMI",
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
                                                                |"adjustmentOfOptionsFollowingVariation":true,
                                                                |"changeInDescriptionOfShares":true,
                                                                |"disqualifyingEvent":true,
                                                                |"natureOfDisqualifyingEvent":"4",
                                                                |"adjustment":{
                                                                |"adjustmentEvents":[
                                                                |{
                                                                |"dateOptionAdjusted":"2011-10-13",
                                                                |"individual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"exercisePricePerSUOPBeforeAdjustment":10.1234,
                                                                |"numberOfSUOPAfterAdjustment":10.14,
                                                                |"exercisePricePerSUOPAfterAdjustment":10.1324,
                                                                |"actualMarketValueOfShareAtGrantDate":10.1244
                                                                |}
                                                                |]
                                                                |},
                                                                |"replaced":{
                                                                |"replacementEvents":[
                                                                |{
                                                                |"grantDateOfOldOption":"2014-12-10",
                                                                |"grantDateOfNewOption":"2014-12-10",
                                                                |"individualReleased":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"actualMarketValuePerShareReplacementAtDate":10.1234,
                                                                |"snopCompany":{
                                                                |"companyName":"company",
                                                                |"companyAddress":{
                                                                |"addressLine1":"1 Beth Street",
                                                                |"addressLine2":"Bucknall",
                                                                |"addressLine3":"Stoke",
                                                                |"addressLine4":"Staffordshire",
                                                                |"country":"UK",
                                                                |"postcode":"SE1 2AB"
                                                                |},
                                                                |"companyCRN":"XT123456",
                                                                |"companyCTRef":"1234567899"
                                                                |}
                                                                |}
                                                                |]
                                                                |},
                                                                |"optionsReleasedLapsedCancelled":true,
                                                                |"released":{
                                                                |"releasedEvents":[
                                                                |{
                                                                |"dateOfEvent":"2014-12-10",
                                                                |"disqualifyingEvent":true,
                                                                |"natureOfDisqualifyingEvent":"1",
                                                                |"individualRelLapsedCanc":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfSharesWhichCanNoLongerBeExercised":10.12,
                                                                |"moneyValueReceived":true,
                                                                |"receivedAmount":123.1234,
                                                                |"payeOperatedApplied":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"ntExercise":{
                                                                |"ntExerciseEvents":[
                                                                |{
                                                                |"exerciseDate":"2015-03-03",
                                                                |"individualNTExercise":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfSharesAcquired":100.0,
                                                                |"actualMarketValueAtGrantDate":10.1234,
                                                                |"exercisePrice":10.1234,
                                                                |"actualMarketValuePerShareAtExerciseDate":10.1234,
                                                                |"sharesListedOnSE":true,
                                                                |"totalAmountPaidToAcquireShares":10.1234,
                                                                |"sharesDisposedOnSameDay":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"taxExercise":{
                                                                |"taxExercise":[
                                                                |{
                                                                |"exerciseDate":"2015-06-04",
                                                                |"disqualifyingEvent":true,
                                                                |"natureOfDisqualifyingEvent":"3",
                                                                |"individualTaxExercise":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfSharesAcquired":100.0,
                                                                |"actualMarketValueAtGrantDate":10.1234,
                                                                |"exercisePricePaidToAcquireAShare":10.1234,
                                                                |"actualMarketValuePerShareAtExerciseDate":10.1234,
                                                                |"unrestrictedMarketValuePerShareAtExerciseDate":10.1234,
                                                                |"totalAmountPaidToAcquireTheShares":10.1234,
                                                                |"sharesListedOnSE":true,
                                                                |"electionMadeUnderSection431":true,
                                                                |"nicsElectionAgreementEnteredInto":true,
                                                                |"amountSubjectToPAYE":10.1234
                                                                |}
                                                                |]
                                                                |},
                                                                |"submitANilReturn":false,
                                                                |"groupPlan":true,
                                                                |"numberOfPeopleHoldingOptionsAtYearEnd":0,
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

    "create valid json for not NilReturn with all sheets and participants from 2 records of sheet data" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(
          List(
            SchemeData(EMI.schemeInfo, "EMI40_Adjustments_V3", None, Some(ListBuffer(EMI.buildAdjustmentsV3(true, "yes")))),
            SchemeData(EMI.schemeInfo, "EMI40_Adjustments_V3", None, Some(ListBuffer(EMI.buildAdjustmentsV3(true, "yes")))),
            SchemeData(EMI.schemeInfo, "EMI40_Replaced_V3", None, Some(ListBuffer(EMI.buildReplacedV3(true)))),
            SchemeData(EMI.schemeInfo, "EMI40_Replaced_V3", None, Some(ListBuffer(EMI.buildReplacedV3(true)))),
            SchemeData(EMI.schemeInfo, "EMI40_RLC_V3", None, Some(ListBuffer(EMI.buildRLCV3(true, "yes", "yes")))),
            SchemeData(EMI.schemeInfo, "EMI40_RLC_V3", None, Some(ListBuffer(EMI.buildRLCV3(true, "yes", "yes")))),
            SchemeData(EMI.schemeInfo, "EMI40_NonTaxable_V3", None, Some(ListBuffer(EMI.buildNonTaxableV3(true, "yes", "yes")))),
            SchemeData(EMI.schemeInfo, "EMI40_NonTaxable_V3", None, Some(ListBuffer(EMI.buildNonTaxableV3(true, "yes", "yes")))),
            SchemeData(EMI.schemeInfo, "EMI40_Taxable_V3", None, Some(ListBuffer(EMI.buildTaxableV3(true, "yes", "yes", "yes")))),
            SchemeData(EMI.schemeInfo, "EMI40_Taxable_V3", None, Some(ListBuffer(EMI.buildTaxableV3(true, "yes", "yes", "yes"))))
          )
        )
      )

      val result = await(adrSubmission.generateSubmission()(request, hc, EMI.metadata))
      result-("acknowledgementReference") shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"EMI",
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
                                                                |"adjustmentOfOptionsFollowingVariation":true,
                                                                |"changeInDescriptionOfShares":true,
                                                                |"disqualifyingEvent":true,
                                                                |"natureOfDisqualifyingEvent":"4",
                                                                |"adjustment":{
                                                                |"adjustmentEvents":[
                                                                |{
                                                                |"dateOptionAdjusted":"2011-10-13",
                                                                |"individual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"exercisePricePerSUOPBeforeAdjustment":10.1234,
                                                                |"numberOfSUOPAfterAdjustment":10.14,
                                                                |"exercisePricePerSUOPAfterAdjustment":10.1324,
                                                                |"actualMarketValueOfShareAtGrantDate":10.1244
                                                                |},
                                                                |{
                                                                |"dateOptionAdjusted":"2011-10-13",
                                                                |"individual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"exercisePricePerSUOPBeforeAdjustment":10.1234,
                                                                |"numberOfSUOPAfterAdjustment":10.14,
                                                                |"exercisePricePerSUOPAfterAdjustment":10.1324,
                                                                |"actualMarketValueOfShareAtGrantDate":10.1244
                                                                |}
                                                                |]
                                                                |},
                                                                |"replaced":{
                                                                |"replacementEvents":[
                                                                |{
                                                                |"grantDateOfOldOption":"2014-12-10",
                                                                |"grantDateOfNewOption":"2014-12-10",
                                                                |"individualReleased":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"actualMarketValuePerShareReplacementAtDate":10.1234,
                                                                |"snopCompany":{
                                                                |"companyName":"company",
                                                                |"companyAddress":{
                                                                |"addressLine1":"1 Beth Street",
                                                                |"addressLine2":"Bucknall",
                                                                |"addressLine3":"Stoke",
                                                                |"addressLine4":"Staffordshire",
                                                                |"country":"UK",
                                                                |"postcode":"SE1 2AB"
                                                                |},
                                                                |"companyCRN":"XT123456",
                                                                |"companyCTRef":"1234567899"
                                                                |}
                                                                |},
                                                                |{
                                                                |"grantDateOfOldOption":"2014-12-10",
                                                                |"grantDateOfNewOption":"2014-12-10",
                                                                |"individualReleased":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"actualMarketValuePerShareReplacementAtDate":10.1234,
                                                                |"snopCompany":{
                                                                |"companyName":"company",
                                                                |"companyAddress":{
                                                                |"addressLine1":"1 Beth Street",
                                                                |"addressLine2":"Bucknall",
                                                                |"addressLine3":"Stoke",
                                                                |"addressLine4":"Staffordshire",
                                                                |"country":"UK",
                                                                |"postcode":"SE1 2AB"
                                                                |},
                                                                |"companyCRN":"XT123456",
                                                                |"companyCTRef":"1234567899"
                                                                |}
                                                                |}
                                                                |]
                                                                |},
                                                                |"optionsReleasedLapsedCancelled":true,
                                                                |"released":{
                                                                |"releasedEvents":[
                                                                |{
                                                                |"dateOfEvent":"2014-12-10",
                                                                |"disqualifyingEvent":true,
                                                                |"natureOfDisqualifyingEvent":"1",
                                                                |"individualRelLapsedCanc":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfSharesWhichCanNoLongerBeExercised":10.12,
                                                                |"moneyValueReceived":true,
                                                                |"receivedAmount":123.1234,
                                                                |"payeOperatedApplied":true
                                                                |},
                                                                |{
                                                                |"dateOfEvent":"2014-12-10",
                                                                |"disqualifyingEvent":true,
                                                                |"natureOfDisqualifyingEvent":"1",
                                                                |"individualRelLapsedCanc":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfSharesWhichCanNoLongerBeExercised":10.12,
                                                                |"moneyValueReceived":true,
                                                                |"receivedAmount":123.1234,
                                                                |"payeOperatedApplied":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"ntExercise":{
                                                                |"ntExerciseEvents":[
                                                                |{
                                                                |"exerciseDate":"2015-03-03",
                                                                |"individualNTExercise":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfSharesAcquired":100.0,
                                                                |"actualMarketValueAtGrantDate":10.1234,
                                                                |"exercisePrice":10.1234,
                                                                |"actualMarketValuePerShareAtExerciseDate":10.1234,
                                                                |"sharesListedOnSE":true,
                                                                |"totalAmountPaidToAcquireShares":10.1234,
                                                                |"sharesDisposedOnSameDay":true
                                                                |},
                                                                |{
                                                                |"exerciseDate":"2015-03-03",
                                                                |"individualNTExercise":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfSharesAcquired":100.0,
                                                                |"actualMarketValueAtGrantDate":10.1234,
                                                                |"exercisePrice":10.1234,
                                                                |"actualMarketValuePerShareAtExerciseDate":10.1234,
                                                                |"sharesListedOnSE":true,
                                                                |"totalAmountPaidToAcquireShares":10.1234,
                                                                |"sharesDisposedOnSameDay":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"taxExercise":{
                                                                |"taxExercise":[
                                                                |{
                                                                |"exerciseDate":"2015-06-04",
                                                                |"disqualifyingEvent":true,
                                                                |"natureOfDisqualifyingEvent":"3",
                                                                |"individualTaxExercise":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfSharesAcquired":100.0,
                                                                |"actualMarketValueAtGrantDate":10.1234,
                                                                |"exercisePricePaidToAcquireAShare":10.1234,
                                                                |"actualMarketValuePerShareAtExerciseDate":10.1234,
                                                                |"unrestrictedMarketValuePerShareAtExerciseDate":10.1234,
                                                                |"totalAmountPaidToAcquireTheShares":10.1234,
                                                                |"sharesListedOnSE":true,
                                                                |"electionMadeUnderSection431":true,
                                                                |"nicsElectionAgreementEnteredInto":true,
                                                                |"amountSubjectToPAYE":10.1234
                                                                |},
                                                                |{
                                                                |"exerciseDate":"2015-06-04",
                                                                |"disqualifyingEvent":true,
                                                                |"natureOfDisqualifyingEvent":"3",
                                                                |"individualTaxExercise":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfSharesAcquired":100.0,
                                                                |"actualMarketValueAtGrantDate":10.1234,
                                                                |"exercisePricePaidToAcquireAShare":10.1234,
                                                                |"actualMarketValuePerShareAtExerciseDate":10.1234,
                                                                |"unrestrictedMarketValuePerShareAtExerciseDate":10.1234,
                                                                |"totalAmountPaidToAcquireTheShares":10.1234,
                                                                |"sharesListedOnSE":true,
                                                                |"electionMadeUnderSection431":true,
                                                                |"nicsElectionAgreementEnteredInto":true,
                                                                |"amountSubjectToPAYE":10.1234
                                                                |}
                                                                |]
                                                                |},
                                                                |"submitANilReturn":false,
                                                                |"groupPlan":true,
                                                                |"numberOfPeopleHoldingOptionsAtYearEnd":0,
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

    "create valid json for not NilReturn with Adjustments and participants" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(
          List(
            SchemeData(EMI.schemeInfo, "EMI40_Adjustments_V3", None, Some(ListBuffer(EMI.buildAdjustmentsV3(true, "yes"))))
          )
        )
      )

      val result = await(adrSubmission.generateSubmission()(request, hc, EMI.metadata))
      result-("acknowledgementReference") shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"EMI",
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
                                                                |"adjustmentOfOptionsFollowingVariation":true,
                                                                |"changeInDescriptionOfShares":true,
                                                                |"disqualifyingEvent":true,
                                                                |"natureOfDisqualifyingEvent":"4",
                                                                |"adjustment":{
                                                                |"adjustmentEvents":[
                                                                |{
                                                                |"dateOptionAdjusted":"2011-10-13",
                                                                |"individual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"exercisePricePerSUOPBeforeAdjustment":10.1234,
                                                                |"numberOfSUOPAfterAdjustment":10.14,
                                                                |"exercisePricePerSUOPAfterAdjustment":10.1324,
                                                                |"actualMarketValueOfShareAtGrantDate":10.1244
                                                                |}
                                                                |]
                                                                |},
                                                                |"optionsReleasedLapsedCancelled":false,
                                                                |"submitANilReturn":false,
                                                                |"groupPlan":true,
                                                                |"numberOfPeopleHoldingOptionsAtYearEnd":0,
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

    "create valid json for not NilReturn with Adjustments and participants from 2 records of sheet data" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(
          List(
            SchemeData(EMI.schemeInfo, "EMI40_Adjustments_V3", None, Some(ListBuffer(EMI.buildAdjustmentsV3(true, "yes")))),
            SchemeData(EMI.schemeInfo, "EMI40_Adjustments_V3", None, Some(ListBuffer(EMI.buildAdjustmentsV3(true, "yes"))))
          )
        )
      )

      val result = await(adrSubmission.generateSubmission()(request, hc, EMI.metadata))
      result-("acknowledgementReference") shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"EMI",
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
                                                                |"adjustmentOfOptionsFollowingVariation":true,
                                                                |"changeInDescriptionOfShares":true,
                                                                |"disqualifyingEvent":true,
                                                                |"natureOfDisqualifyingEvent":"4",
                                                                |"adjustment":{
                                                                |"adjustmentEvents":[
                                                                |{
                                                                |"dateOptionAdjusted":"2011-10-13",
                                                                |"individual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"exercisePricePerSUOPBeforeAdjustment":10.1234,
                                                                |"numberOfSUOPAfterAdjustment":10.14,
                                                                |"exercisePricePerSUOPAfterAdjustment":10.1324,
                                                                |"actualMarketValueOfShareAtGrantDate":10.1244
                                                                |},
                                                                |{
                                                                |"dateOptionAdjusted":"2011-10-13",
                                                                |"individual":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"exercisePricePerSUOPBeforeAdjustment":10.1234,
                                                                |"numberOfSUOPAfterAdjustment":10.14,
                                                                |"exercisePricePerSUOPAfterAdjustment":10.1324,
                                                                |"actualMarketValueOfShareAtGrantDate":10.1244
                                                                |}
                                                                |]
                                                                |},
                                                                |"optionsReleasedLapsedCancelled":false,
                                                                |"submitANilReturn":false,
                                                                |"groupPlan":true,
                                                                |"numberOfPeopleHoldingOptionsAtYearEnd":0,
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

    "create valid json for not NilReturn with Released and participants" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(
          List(
            SchemeData(EMI.schemeInfo, "EMI40_RLC_V3", None, Some(ListBuffer(EMI.buildRLCV3(true, "yes", "yes"))))
          )
        )
      )

      val result = await(adrSubmission.generateSubmission()(request, hc, EMI.metadata))
      result-("acknowledgementReference") shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"EMI",
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
                                                                |"adjustmentOfOptionsFollowingVariation":false,
                                                                |"optionsReleasedLapsedCancelled":true,
                                                                |"released":{
                                                                |"releasedEvents":[
                                                                |{
                                                                |"dateOfEvent":"2014-12-10",
                                                                |"disqualifyingEvent":true,
                                                                |"natureOfDisqualifyingEvent":"1",
                                                                |"individualRelLapsedCanc":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfSharesWhichCanNoLongerBeExercised":10.12,
                                                                |"moneyValueReceived":true,
                                                                |"receivedAmount":123.1234,
                                                                |"payeOperatedApplied":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"submitANilReturn":false,
                                                                |"groupPlan":true,
                                                                |"numberOfPeopleHoldingOptionsAtYearEnd":0,
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

    "create valid json for not NilReturn with Released and participants from 2 records of sheet data" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(
          List(
            SchemeData(EMI.schemeInfo, "EMI40_RLC_V3", None, Some(ListBuffer(EMI.buildRLCV3(true, "yes", "yes")))),
            SchemeData(EMI.schemeInfo, "EMI40_RLC_V3", None, Some(ListBuffer(EMI.buildRLCV3(true, "yes", "yes"))))
          )
        )
      )

      val result = await(adrSubmission.generateSubmission()(request, hc, EMI.metadata))
      result-("acknowledgementReference") shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"EMI",
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
                                                                |"adjustmentOfOptionsFollowingVariation":false,
                                                                |"optionsReleasedLapsedCancelled":true,
                                                                |"released":{
                                                                |"releasedEvents":[
                                                                |{
                                                                |"dateOfEvent":"2014-12-10",
                                                                |"disqualifyingEvent":true,
                                                                |"natureOfDisqualifyingEvent":"1",
                                                                |"individualRelLapsedCanc":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfSharesWhichCanNoLongerBeExercised":10.12,
                                                                |"moneyValueReceived":true,
                                                                |"receivedAmount":123.1234,
                                                                |"payeOperatedApplied":true
                                                                |},
                                                                |{
                                                                |"dateOfEvent":"2014-12-10",
                                                                |"disqualifyingEvent":true,
                                                                |"natureOfDisqualifyingEvent":"1",
                                                                |"individualRelLapsedCanc":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfSharesWhichCanNoLongerBeExercised":10.12,
                                                                |"moneyValueReceived":true,
                                                                |"receivedAmount":123.1234,
                                                                |"payeOperatedApplied":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"submitANilReturn":false,
                                                                |"groupPlan":true,
                                                                |"numberOfPeopleHoldingOptionsAtYearEnd":0,
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

    "create valid json for not NilReturn with RLC and participants" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(
          List(
            SchemeData(EMI.schemeInfo, "EMI40_RLC_V3", None, Some(ListBuffer(EMI.buildRLCV3(true, "yes", "yes"))))
          )
        )
      )

      val result = await(adrSubmission.generateSubmission()(request, hc, EMI.metadata))
      result-("acknowledgementReference") shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"EMI",
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
                                                                |"adjustmentOfOptionsFollowingVariation":false,
                                                                |"optionsReleasedLapsedCancelled":true,
                                                                |"released":{
                                                                |"releasedEvents":[
                                                                |{
                                                                |"dateOfEvent":"2014-12-10",
                                                                |"disqualifyingEvent":true,
                                                                |"natureOfDisqualifyingEvent":"1",
                                                                |"individualRelLapsedCanc":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfSharesWhichCanNoLongerBeExercised":10.12,
                                                                |"moneyValueReceived":true,
                                                                |"receivedAmount":123.1234,
                                                                |"payeOperatedApplied":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"submitANilReturn":false,
                                                                |"groupPlan":true,
                                                                |"numberOfPeopleHoldingOptionsAtYearEnd":0,
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

    "create valid json for not NilReturn with RLC and participants from 2 records of sheet data" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(
          List(
            SchemeData(EMI.schemeInfo, "EMI40_RLC_V3", None, Some(ListBuffer(EMI.buildRLCV3(true, "yes", "yes")))),
            SchemeData(EMI.schemeInfo, "EMI40_RLC_V3", None, Some(ListBuffer(EMI.buildRLCV3(true, "yes", "yes"))))
          )
        )
      )

      val result = await(adrSubmission.generateSubmission()(request, hc, EMI.metadata))
      result-("acknowledgementReference") shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"EMI",
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
                                                                |"adjustmentOfOptionsFollowingVariation":false,
                                                                |"optionsReleasedLapsedCancelled":true,
                                                                |"released":{
                                                                |"releasedEvents":[
                                                                |{
                                                                |"dateOfEvent":"2014-12-10",
                                                                |"disqualifyingEvent":true,
                                                                |"natureOfDisqualifyingEvent":"1",
                                                                |"individualRelLapsedCanc":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfSharesWhichCanNoLongerBeExercised":10.12,
                                                                |"moneyValueReceived":true,
                                                                |"receivedAmount":123.1234,
                                                                |"payeOperatedApplied":true
                                                                |},
                                                                |{
                                                                |"dateOfEvent":"2014-12-10",
                                                                |"disqualifyingEvent":true,
                                                                |"natureOfDisqualifyingEvent":"1",
                                                                |"individualRelLapsedCanc":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfSharesWhichCanNoLongerBeExercised":10.12,
                                                                |"moneyValueReceived":true,
                                                                |"receivedAmount":123.1234,
                                                                |"payeOperatedApplied":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"submitANilReturn":false,
                                                                |"groupPlan":true,
                                                                |"numberOfPeopleHoldingOptionsAtYearEnd":0,
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

    "create valid json for not NilReturn with NonTaxable and participants" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(
          List(
            SchemeData(EMI.schemeInfo, "EMI40_NonTaxable_V3", None, Some(ListBuffer(EMI.buildNonTaxableV3(true, "yes", "yes"))))
          )
        )
      )

      val result = await(adrSubmission.generateSubmission()(request, hc, EMI.metadata))
      result-("acknowledgementReference") shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"EMI",
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
                                                                |"adjustmentOfOptionsFollowingVariation":false,
                                                                |"optionsReleasedLapsedCancelled":false,
                                                                |"ntExercise":{
                                                                |"ntExerciseEvents":[
                                                                |{
                                                                |"exerciseDate":"2015-03-03",
                                                                |"individualNTExercise":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfSharesAcquired":100.0,
                                                                |"actualMarketValueAtGrantDate":10.1234,
                                                                |"exercisePrice":10.1234,
                                                                |"actualMarketValuePerShareAtExerciseDate":10.1234,
                                                                |"sharesListedOnSE":true,
                                                                |"totalAmountPaidToAcquireShares":10.1234,
                                                                |"sharesDisposedOnSameDay":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"submitANilReturn":false,
                                                                |"groupPlan":true,
                                                                |"numberOfPeopleHoldingOptionsAtYearEnd":0,
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

    "create valid json for not NilReturn with NonTaxable and participants from 2 records of sheet data" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(
          List(
            SchemeData(EMI.schemeInfo, "EMI40_NonTaxable_V3", None, Some(ListBuffer(EMI.buildNonTaxableV3(true, "yes", "yes")))),
            SchemeData(EMI.schemeInfo, "EMI40_NonTaxable_V3", None, Some(ListBuffer(EMI.buildNonTaxableV3(true, "yes", "yes"))))
          )
        )
      )

      val result = await(adrSubmission.generateSubmission()(request, hc, EMI.metadata))
      result-("acknowledgementReference") shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"EMI",
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
                                                                |"adjustmentOfOptionsFollowingVariation":false,
                                                                |"optionsReleasedLapsedCancelled":false,
                                                                |"ntExercise":{
                                                                |"ntExerciseEvents":[
                                                                |{
                                                                |"exerciseDate":"2015-03-03",
                                                                |"individualNTExercise":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfSharesAcquired":100.0,
                                                                |"actualMarketValueAtGrantDate":10.1234,
                                                                |"exercisePrice":10.1234,
                                                                |"actualMarketValuePerShareAtExerciseDate":10.1234,
                                                                |"sharesListedOnSE":true,
                                                                |"totalAmountPaidToAcquireShares":10.1234,
                                                                |"sharesDisposedOnSameDay":true
                                                                |},
                                                                |{
                                                                |"exerciseDate":"2015-03-03",
                                                                |"individualNTExercise":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfSharesAcquired":100.0,
                                                                |"actualMarketValueAtGrantDate":10.1234,
                                                                |"exercisePrice":10.1234,
                                                                |"actualMarketValuePerShareAtExerciseDate":10.1234,
                                                                |"sharesListedOnSE":true,
                                                                |"totalAmountPaidToAcquireShares":10.1234,
                                                                |"sharesDisposedOnSameDay":true
                                                                |}
                                                                |]
                                                                |},
                                                                |"submitANilReturn":false,
                                                                |"groupPlan":true,
                                                                |"numberOfPeopleHoldingOptionsAtYearEnd":0,
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

    "create valid json for not NilReturn with Taxable and participants" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(
          List(
            SchemeData(EMI.schemeInfo, "EMI40_Taxable_V3", None, Some(ListBuffer(EMI.buildTaxableV3(true, "yes", "yes", "yes"))))
          )
        )
      )

      val result = await(adrSubmission.generateSubmission()(request, hc, EMI.metadata))
      result-("acknowledgementReference") shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"EMI",
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
                                                                |"adjustmentOfOptionsFollowingVariation":false,
                                                                |"optionsReleasedLapsedCancelled":false,
                                                                |"taxExercise":{
                                                                |"taxExercise":[
                                                                |{
                                                                |"exerciseDate":"2015-06-04",
                                                                |"disqualifyingEvent":true,
                                                                |"natureOfDisqualifyingEvent":"3",
                                                                |"individualTaxExercise":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfSharesAcquired":100.0,
                                                                |"actualMarketValueAtGrantDate":10.1234,
                                                                |"exercisePricePaidToAcquireAShare":10.1234,
                                                                |"actualMarketValuePerShareAtExerciseDate":10.1234,
                                                                |"unrestrictedMarketValuePerShareAtExerciseDate":10.1234,
                                                                |"totalAmountPaidToAcquireTheShares":10.1234,
                                                                |"sharesListedOnSE":true,
                                                                |"electionMadeUnderSection431":true,
                                                                |"nicsElectionAgreementEnteredInto":true,
                                                                |"amountSubjectToPAYE":10.1234
                                                                |}
                                                                |]
                                                                |},
                                                                |"submitANilReturn":false,
                                                                |"groupPlan":true,
                                                                |"numberOfPeopleHoldingOptionsAtYearEnd":0,
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

    "create valid json for not NilReturn with Taxable and participants from 2 records of sheet data" in {

      when(
        mockPresubmissionService.getJson(any[SchemeInfo]())
      ).thenReturn(
        Future.successful(
          List(
            SchemeData(EMI.schemeInfo, "EMI40_Taxable_V3", None, Some(ListBuffer(EMI.buildTaxableV3(true, "yes", "yes", "yes")))),
            SchemeData(EMI.schemeInfo, "EMI40_Taxable_V3", None, Some(ListBuffer(EMI.buildTaxableV3(true, "yes", "yes", "yes"))))
          )
        )
      )

      val result = await(adrSubmission.generateSubmission()(request, hc, EMI.metadata))
      result-("acknowledgementReference") shouldBe Json.parse("""{
                                                                |"regime":"ERS",
                                                                |"schemeType":"EMI",
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
                                                                |"adjustmentOfOptionsFollowingVariation":false,
                                                                |"optionsReleasedLapsedCancelled":false,
                                                                |"taxExercise":{
                                                                |"taxExercise":[
                                                                |{
                                                                |"exerciseDate":"2015-06-04",
                                                                |"disqualifyingEvent":true,
                                                                |"natureOfDisqualifyingEvent":"3",
                                                                |"individualTaxExercise":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfSharesAcquired":100.0,
                                                                |"actualMarketValueAtGrantDate":10.1234,
                                                                |"exercisePricePaidToAcquireAShare":10.1234,
                                                                |"actualMarketValuePerShareAtExerciseDate":10.1234,
                                                                |"unrestrictedMarketValuePerShareAtExerciseDate":10.1234,
                                                                |"totalAmountPaidToAcquireTheShares":10.1234,
                                                                |"sharesListedOnSE":true,
                                                                |"electionMadeUnderSection431":true,
                                                                |"nicsElectionAgreementEnteredInto":true,
                                                                |"amountSubjectToPAYE":10.1234
                                                                |},
                                                                |{
                                                                |"exerciseDate":"2015-06-04",
                                                                |"disqualifyingEvent":true,
                                                                |"natureOfDisqualifyingEvent":"3",
                                                                |"individualTaxExercise":{
                                                                |"firstName":"First",
                                                                |"secondName":"Second",
                                                                |"surname":"Last",
                                                                |"nino":"NINO",
                                                                |"payeReference":"123/XZ55555555"
                                                                |},
                                                                |"numberOfSharesAcquired":100.0,
                                                                |"actualMarketValueAtGrantDate":10.1234,
                                                                |"exercisePricePaidToAcquireAShare":10.1234,
                                                                |"actualMarketValuePerShareAtExerciseDate":10.1234,
                                                                |"unrestrictedMarketValuePerShareAtExerciseDate":10.1234,
                                                                |"totalAmountPaidToAcquireTheShares":10.1234,
                                                                |"sharesListedOnSE":true,
                                                                |"electionMadeUnderSection431":true,
                                                                |"nicsElectionAgreementEnteredInto":true,
                                                                |"amountSubjectToPAYE":10.1234
                                                                |}
                                                                |]
                                                                |},
                                                                |"submitANilReturn":false,
                                                                |"groupPlan":true,
                                                                |"numberOfPeopleHoldingOptionsAtYearEnd":0,
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

  "calling generateJson for Adjustments V3" should {

    val configData: Config = Common.loadConfiguration(EMI.schemeType, "EMI40_Adjustments_V3")

    "create valid JSON with disqualifyingEvent = \"yes\"" in {

      val result = ADRSubmission.buildJson(
        configData,
        ListBuffer(
          EMI.buildAdjustmentsV3(withAllFields = true, disqualifyingEvent = "yes"),
          EMI.buildAdjustmentsV3(withAllFields = false, disqualifyingEvent = "yes")
        )
      )

      result shouldBe Json.parse("""{
                                  |"adjustmentOfOptionsFollowingVariation":true,
                                  |"changeInDescriptionOfShares":true,
                                  |"disqualifyingEvent":true,
                                  |"natureOfDisqualifyingEvent":"4",
                                  |"adjustment":{
                                  |"adjustmentEvents":[
                                  |{
                                  |"dateOptionAdjusted":"2011-10-13",
                                  |"individual":{
                                  |"firstName":"First",
                                  |"secondName":"Second",
                                  |"surname":"Last",
                                  |"nino":"NINO",
                                  |"payeReference":"123/XZ55555555"
                                  |},
                                  |"exercisePricePerSUOPBeforeAdjustment":10.1234,
                                  |"numberOfSUOPAfterAdjustment":10.14,
                                  |"exercisePricePerSUOPAfterAdjustment":10.1324,
                                  |"actualMarketValueOfShareAtGrantDate":10.1244
                                  |},
                                  |{
                                  |"dateOptionAdjusted":"2011-10-13",
                                  |"individual":{
                                  |"firstName":"First",
                                  |"surname":"Last",
                                  |"payeReference":"123/XZ55555555"
                                  |},
                                  |"exercisePricePerSUOPBeforeAdjustment":10.1234,
                                  |"numberOfSUOPAfterAdjustment":10.14,
                                  |"exercisePricePerSUOPAfterAdjustment":10.1324,
                                  |"actualMarketValueOfShareAtGrantDate":10.1244
                                  |}
                                  |]
                                  |}
                                  |}""".stripMargin)
    }

    "create valid JSON with disqualifyingEvent = \"no\"" in {

      val result = ADRSubmission.buildJson(
        configData,
        ListBuffer(
          EMI.buildAdjustmentsV3(withAllFields = true, disqualifyingEvent = "no"),
          EMI.buildAdjustmentsV3(withAllFields = false, disqualifyingEvent = "no")
        )
      )

      result shouldBe Json.parse("""{
                                   |"adjustmentOfOptionsFollowingVariation":true,
                                   |"changeInDescriptionOfShares":true,
                                   |"disqualifyingEvent":false,
                                   |"adjustment":{
                                   |"adjustmentEvents":[
                                   |{
                                   |"dateOptionAdjusted":"2011-10-13",
                                   |"individual":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"exercisePricePerSUOPBeforeAdjustment":10.1234,
                                   |"numberOfSUOPAfterAdjustment":10.14,
                                   |"exercisePricePerSUOPAfterAdjustment":10.1324,
                                   |"actualMarketValueOfShareAtGrantDate":10.1244
                                   |},
                                   |{
                                   |"dateOptionAdjusted":"2011-10-13",
                                   |"individual":{
                                   |"firstName":"First",
                                   |"surname":"Last",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"exercisePricePerSUOPBeforeAdjustment":10.1234,
                                   |"numberOfSUOPAfterAdjustment":10.14,
                                   |"exercisePricePerSUOPAfterAdjustment":10.1324,
                                   |"actualMarketValueOfShareAtGrantDate":10.1244
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }
  }

  "calling generateJson for Replaced V3" should {

    val configData: Config = Common.loadConfiguration(EMI.schemeType, "EMI40_Replaced_V3")

    "create valid JSON" in {

      val result = ADRSubmission.buildJson(
        configData,
        ListBuffer(
          EMI.buildReplacedV3(withAllFields = true),
          EMI.buildReplacedV3(withAllFields = false)
        )
      )

      result shouldBe Json.parse("""{
                                   |"replaced":{
                                   |"replacementEvents":[
                                   |{
                                   |"grantDateOfOldOption":"2014-12-10",
                                   |"grantDateOfNewOption":"2014-12-10",
                                   |"individualReleased":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"actualMarketValuePerShareReplacementAtDate":10.1234,
                                   |"snopCompany":{
                                   |"companyName":"company",
                                   |"companyAddress":{
                                   |"addressLine1":"1 Beth Street",
                                   |"addressLine2":"Bucknall",
                                   |"addressLine3":"Stoke",
                                   |"addressLine4":"Staffordshire",
                                   |"country":"UK",
                                   |"postcode":"SE1 2AB"
                                   |},
                                   |"companyCRN":"XT123456",
                                   |"companyCTRef":"1234567899"
                                   |}
                                   |},
                                   |{
                                   |"grantDateOfOldOption":"2014-12-10",
                                   |"grantDateOfNewOption":"2014-12-10",
                                   |"individualReleased":{
                                   |"firstName":"First",
                                   |"surname":"Last",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"actualMarketValuePerShareReplacementAtDate":10.1234,
                                   |"snopCompany":{
                                   |"companyName":"company",
                                   |"companyAddress":{
                                   |"addressLine1":"1 Beth Street",
                                   |"country":"UK",
                                   |"postcode":"SE1 2AB"
                                   |},
                                   |"companyCRN":"XT123456",
                                   |"companyCTRef":"1234567899"
                                   |}
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

  }

  "calling generateJson for RLC V3" should {

    val configData: Config = Common.loadConfiguration(EMI.schemeType, "EMI40_RLC_V3")

    "create valid JSON for withAllFields = (true or false), disqualifyingEvent = \"yes\" and moneyValueReceived = \"yes\"" in {

      val result = ADRSubmission.buildJson(
        configData,
        ListBuffer(
          EMI.buildRLCV3(withAllFields = true, disqualifyingEvent = "yes", moneyValueReceived = "yes"),
          EMI.buildRLCV3(withAllFields = false, disqualifyingEvent = "yes", moneyValueReceived = "yes")
        )
      )

      result shouldBe Json.parse("""{
                                   |"optionsReleasedLapsedCancelled":true,
                                   |"released":{
                                   |"releasedEvents":[
                                   |{
                                   |"dateOfEvent":"2014-12-10",
                                   |"disqualifyingEvent":true,
                                   |"natureOfDisqualifyingEvent":"1",
                                   |"individualRelLapsedCanc":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfSharesWhichCanNoLongerBeExercised":10.12,
                                   |"moneyValueReceived":true,
                                   |"receivedAmount":123.1234,
                                   |"payeOperatedApplied":true
                                   |},
                                   |{
                                   |"dateOfEvent":"2014-12-10",
                                   |"disqualifyingEvent":true,
                                   |"natureOfDisqualifyingEvent":"1",
                                   |"individualRelLapsedCanc":{
                                   |"firstName":"First",
                                   |"surname":"Last",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfSharesWhichCanNoLongerBeExercised":10.12,
                                   |"moneyValueReceived":true,
                                   |"receivedAmount":123.1234,
                                   |"payeOperatedApplied":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON for withAllFields = true, disqualifyingEvent = (\"yes\" or \"no\") and moneyValueReceived = \"yes\"" in {

      val result = ADRSubmission.buildJson(
        configData,
        ListBuffer(
          EMI.buildRLCV3(withAllFields = true, disqualifyingEvent = "yes", moneyValueReceived = "yes"),
          EMI.buildRLCV3(withAllFields = true, disqualifyingEvent = "no", moneyValueReceived = "yes")
        )
      )

      result shouldBe Json.parse("""{
                                   |"optionsReleasedLapsedCancelled":true,
                                   |"released":{
                                   |"releasedEvents":[
                                   |{
                                   |"dateOfEvent":"2014-12-10",
                                   |"disqualifyingEvent":true,
                                   |"natureOfDisqualifyingEvent":"1",
                                   |"individualRelLapsedCanc":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfSharesWhichCanNoLongerBeExercised":10.12,
                                   |"moneyValueReceived":true,
                                   |"receivedAmount":123.1234,
                                   |"payeOperatedApplied":true
                                   |},
                                   |{
                                   |"dateOfEvent":"2014-12-10",
                                   |"disqualifyingEvent":false,
                                   |"individualRelLapsedCanc":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfSharesWhichCanNoLongerBeExercised":10.12,
                                   |"moneyValueReceived":true,
                                   |"receivedAmount":123.1234,
                                   |"payeOperatedApplied":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON for withAllFields = true, disqualifyingEvent = \"yes\" and moneyValueReceived = (\"yes\" or \"no\")" in {

      val result = ADRSubmission.buildJson(
        configData,
        ListBuffer(
          EMI.buildRLCV3(withAllFields = true, disqualifyingEvent = "yes", moneyValueReceived = "yes"),
          EMI.buildRLCV3(withAllFields = true, disqualifyingEvent = "yes", moneyValueReceived = "no")
        )
      )

      result shouldBe Json.parse("""{
                                   |"optionsReleasedLapsedCancelled":true,
                                   |"released":{
                                   |"releasedEvents":[
                                   |{
                                   |"dateOfEvent":"2014-12-10",
                                   |"disqualifyingEvent":true,
                                   |"natureOfDisqualifyingEvent":"1",
                                   |"individualRelLapsedCanc":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfSharesWhichCanNoLongerBeExercised":10.12,
                                   |"moneyValueReceived":true,
                                   |"receivedAmount":123.1234,
                                   |"payeOperatedApplied":true
                                   |},
                                   |{
                                   |"dateOfEvent":"2014-12-10",
                                   |"disqualifyingEvent":true,
                                   |"natureOfDisqualifyingEvent":"1",
                                   |"individualRelLapsedCanc":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfSharesWhichCanNoLongerBeExercised":10.12,
                                   |"moneyValueReceived":false
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

  }

  "calling generateJson for NonTaxable V3" should {

    val configData: Config = Common.loadConfiguration(EMI.schemeType, "EMI40_NonTaxable_V3")

    "create valid JSON for withAllFields = (true or false), sharesListedOnSE = \"yes\" and marketValueAgreedHMRC = \"yes\"" in {

      val result = ADRSubmission.buildJson(
        configData,
        ListBuffer(
          EMI.buildNonTaxableV3(withAllFields = true, sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes"),
          EMI.buildNonTaxableV3(withAllFields = false, sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes")
        )
      )

      result shouldBe Json.parse("""{
                                   |"ntExercise":{
                                   |"ntExerciseEvents":[
                                   |{
                                   |"exerciseDate":"2015-03-03",
                                   |"individualNTExercise":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfSharesAcquired":100.0,
                                   |"actualMarketValueAtGrantDate":10.1234,
                                   |"exercisePrice":10.1234,
                                   |"actualMarketValuePerShareAtExerciseDate":10.1234,
                                   |"sharesListedOnSE":true,
                                   |"totalAmountPaidToAcquireShares":10.1234,
                                   |"sharesDisposedOnSameDay":true
                                   |},
                                   |{
                                   |"exerciseDate":"2015-03-03",
                                   |"individualNTExercise":{
                                   |"firstName":"First",
                                   |"surname":"Last",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfSharesAcquired":100.0,
                                   |"actualMarketValueAtGrantDate":10.1234,
                                   |"exercisePrice":10.1234,
                                   |"actualMarketValuePerShareAtExerciseDate":10.1234,
                                   |"sharesListedOnSE":true,
                                   |"totalAmountPaidToAcquireShares":10.1234,
                                   |"sharesDisposedOnSameDay":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON for withAllFields = true, sharesListedOnSE = (\"yes\" or \"no\") and marketValueAgreedHMRC = \"yes\"" in {

      val result = ADRSubmission.buildJson(
        configData,
        ListBuffer(
          EMI.buildNonTaxableV3(withAllFields = true, sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes"),
          EMI.buildNonTaxableV3(withAllFields = true, sharesListedOnSE = "no", marketValueAgreedHMRC = "yes")
        )
      )

      result shouldBe Json.parse("""{
                                   |"ntExercise":{
                                   |"ntExerciseEvents":[
                                   |{
                                   |"exerciseDate":"2015-03-03",
                                   |"individualNTExercise":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfSharesAcquired":100.0,
                                   |"actualMarketValueAtGrantDate":10.1234,
                                   |"exercisePrice":10.1234,
                                   |"actualMarketValuePerShareAtExerciseDate":10.1234,
                                   |"sharesListedOnSE":true,
                                   |"totalAmountPaidToAcquireShares":10.1234,
                                   |"sharesDisposedOnSameDay":true
                                   |},
                                   |{
                                   |"exerciseDate":"2015-03-03",
                                   |"individualNTExercise":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfSharesAcquired":100.0,
                                   |"actualMarketValueAtGrantDate":10.1234,
                                   |"exercisePrice":10.1234,
                                   |"actualMarketValuePerShareAtExerciseDate":10.1234,
                                   |"sharesListedOnSE":false,
                                   |"marketValueAgreedHMRC": true,
                                   |"hmrcRef": "aa12345678",
                                   |"totalAmountPaidToAcquireShares":10.1234,
                                   |"sharesDisposedOnSameDay":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON for withAllFields = true, sharesListedOnSE = \"no\" and marketValueAgreedHMRC = (\"yes\" or \"no\")" in {

      val result = ADRSubmission.buildJson(
        configData,
        ListBuffer(
          EMI.buildNonTaxableV3(withAllFields = true, sharesListedOnSE = "no", marketValueAgreedHMRC = "yes"),
          EMI.buildNonTaxableV3(withAllFields = true, sharesListedOnSE = "no", marketValueAgreedHMRC = "no")
        )
      )

      result shouldBe Json.parse("""{
                                   |"ntExercise":{
                                   |"ntExerciseEvents":[
                                   |{
                                   |"exerciseDate":"2015-03-03",
                                   |"individualNTExercise":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfSharesAcquired":100.0,
                                   |"actualMarketValueAtGrantDate":10.1234,
                                   |"exercisePrice":10.1234,
                                   |"actualMarketValuePerShareAtExerciseDate":10.1234,
                                   |"sharesListedOnSE":false,
                                   |"marketValueAgreedHMRC": true,
                                   |"hmrcRef": "aa12345678",
                                   |"totalAmountPaidToAcquireShares":10.1234,
                                   |"sharesDisposedOnSameDay":true
                                   |},
                                   |{
                                   |"exerciseDate":"2015-03-03",
                                   |"individualNTExercise":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfSharesAcquired":100.0,
                                   |"actualMarketValueAtGrantDate":10.1234,
                                   |"exercisePrice":10.1234,
                                   |"actualMarketValuePerShareAtExerciseDate":10.1234,
                                   |"sharesListedOnSE":false,
                                   |"marketValueAgreedHMRC": false,
                                   |"totalAmountPaidToAcquireShares":10.1234,
                                   |"sharesDisposedOnSameDay":true
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }
  }

  "calling generateJson for Taxable V3" should {

    val configData: Config = Common.loadConfiguration(EMI.schemeType, "EMI40_Taxable_V3")

    "create valid JSON for withAllFields = (true or false), disqualifyingEvent = \"yes\", sharesListedOnSE = \"yes\" and marketValueAgreedHMRC = \"yes\"" in {

      val result = ADRSubmission.buildJson(
        configData,
        ListBuffer(
          EMI.buildTaxableV3(withAllFields = true, disqualifyingEvent = "yes", sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes"),
          EMI.buildTaxableV3(withAllFields = false, disqualifyingEvent = "yes", sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes")
        )
      )

      result shouldBe Json.parse("""{
                                   |"taxExercise":{
                                   |"taxExercise":[
                                   |{
                                   |"exerciseDate":"2015-06-04",
                                   |"disqualifyingEvent":true,
                                   |"natureOfDisqualifyingEvent":"3",
                                   |"individualTaxExercise":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfSharesAcquired":100.0,
                                   |"actualMarketValueAtGrantDate":10.1234,
                                   |"exercisePricePaidToAcquireAShare":10.1234,
                                   |"actualMarketValuePerShareAtExerciseDate":10.1234,
                                   |"unrestrictedMarketValuePerShareAtExerciseDate":10.1234,
                                   |"totalAmountPaidToAcquireTheShares":10.1234,
                                   |"sharesListedOnSE":true,
                                   |"electionMadeUnderSection431":true,
                                   |"nicsElectionAgreementEnteredInto":true,
                                   |"amountSubjectToPAYE":10.1234
                                   |},
                                   |{
                                   |"exerciseDate":"2015-06-04",
                                   |"disqualifyingEvent":true,
                                   |"natureOfDisqualifyingEvent":"3",
                                   |"individualTaxExercise":{
                                   |"firstName":"First",
                                   |"surname":"Last",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfSharesAcquired":100.0,
                                   |"actualMarketValueAtGrantDate":10.1234,
                                   |"exercisePricePaidToAcquireAShare":10.1234,
                                   |"actualMarketValuePerShareAtExerciseDate":10.1234,
                                   |"unrestrictedMarketValuePerShareAtExerciseDate":10.1234,
                                   |"totalAmountPaidToAcquireTheShares":10.1234,
                                   |"sharesListedOnSE":true,
                                   |"electionMadeUnderSection431":true,
                                   |"nicsElectionAgreementEnteredInto":true,
                                   |"amountSubjectToPAYE":10.1234
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON for withAllFields = true, disqualifyingEvent = (\"yes\" or \"no\"), sharesListedOnSE = \"yes\" and marketValueAgreedHMRC = \"yes\"" in {

      val result = ADRSubmission.buildJson(
        configData,
        ListBuffer(
          EMI.buildTaxableV3(withAllFields = true, disqualifyingEvent = "yes", sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes"),
          EMI.buildTaxableV3(withAllFields = true, disqualifyingEvent = "no", sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes")
        )
      )

      result shouldBe Json.parse("""{
                                   |"taxExercise":{
                                   |"taxExercise":[
                                   |{
                                   |"exerciseDate":"2015-06-04",
                                   |"disqualifyingEvent":true,
                                   |"natureOfDisqualifyingEvent":"3",
                                   |"individualTaxExercise":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfSharesAcquired":100.0,
                                   |"actualMarketValueAtGrantDate":10.1234,
                                   |"exercisePricePaidToAcquireAShare":10.1234,
                                   |"actualMarketValuePerShareAtExerciseDate":10.1234,
                                   |"unrestrictedMarketValuePerShareAtExerciseDate":10.1234,
                                   |"totalAmountPaidToAcquireTheShares":10.1234,
                                   |"sharesListedOnSE":true,
                                   |"electionMadeUnderSection431":true,
                                   |"nicsElectionAgreementEnteredInto":true,
                                   |"amountSubjectToPAYE":10.1234
                                   |},
                                   |{
                                   |"exerciseDate":"2015-06-04",
                                   |"disqualifyingEvent":false,
                                   |"individualTaxExercise":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfSharesAcquired":100.0,
                                   |"actualMarketValueAtGrantDate":10.1234,
                                   |"exercisePricePaidToAcquireAShare":10.1234,
                                   |"actualMarketValuePerShareAtExerciseDate":10.1234,
                                   |"unrestrictedMarketValuePerShareAtExerciseDate":10.1234,
                                   |"totalAmountPaidToAcquireTheShares":10.1234,
                                   |"sharesListedOnSE":true,
                                   |"electionMadeUnderSection431":true,
                                   |"nicsElectionAgreementEnteredInto":true,
                                   |"amountSubjectToPAYE":10.1234
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON for withAllFields = true, disqualifyingEvent = \"yes\", sharesListedOnSE = (\"yes\" or \"no\") and marketValueAgreedHMRC = \"yes\"" in {

      val result = ADRSubmission.buildJson(
        configData,
        ListBuffer(
          EMI.buildTaxableV3(withAllFields = true, disqualifyingEvent = "yes", sharesListedOnSE = "yes", marketValueAgreedHMRC = "yes"),
          EMI.buildTaxableV3(withAllFields = true, disqualifyingEvent = "yes", sharesListedOnSE = "no", marketValueAgreedHMRC = "yes")
        )
      )

      result shouldBe Json.parse("""{
                                   |"taxExercise":{
                                   |"taxExercise":[
                                   |{
                                   |"exerciseDate":"2015-06-04",
                                   |"disqualifyingEvent":true,
                                   |"natureOfDisqualifyingEvent":"3",
                                   |"individualTaxExercise":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfSharesAcquired":100.0,
                                   |"actualMarketValueAtGrantDate":10.1234,
                                   |"exercisePricePaidToAcquireAShare":10.1234,
                                   |"actualMarketValuePerShareAtExerciseDate":10.1234,
                                   |"unrestrictedMarketValuePerShareAtExerciseDate":10.1234,
                                   |"totalAmountPaidToAcquireTheShares":10.1234,
                                   |"sharesListedOnSE":true,
                                   |"electionMadeUnderSection431":true,
                                   |"nicsElectionAgreementEnteredInto":true,
                                   |"amountSubjectToPAYE":10.1234
                                   |},
                                   |{
                                   |"exerciseDate":"2015-06-04",
                                   |"disqualifyingEvent":true,
                                   |"natureOfDisqualifyingEvent":"3",
                                   |"individualTaxExercise":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfSharesAcquired":100.0,
                                   |"actualMarketValueAtGrantDate":10.1234,
                                   |"exercisePricePaidToAcquireAShare":10.1234,
                                   |"actualMarketValuePerShareAtExerciseDate":10.1234,
                                   |"unrestrictedMarketValuePerShareAtExerciseDate":10.1234,
                                   |"totalAmountPaidToAcquireTheShares":10.1234,
                                   |"sharesListedOnSE":false,
                                   |"marketValueAgreedHMRC": true,
                                   |"hmrcRef": "aa12345678",
                                   |"electionMadeUnderSection431":true,
                                   |"nicsElectionAgreementEnteredInto":true,
                                   |"amountSubjectToPAYE":10.1234
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }

    "create valid JSON for withAllFields = true, disqualifyingEvent = \"yes\", sharesListedOnSE = \"no\" and marketValueAgreedHMRC = (\"yes\" or \"no\")" in {

      val result = ADRSubmission.buildJson(
        configData,
        ListBuffer(
          EMI.buildTaxableV3(withAllFields = true, disqualifyingEvent = "yes", sharesListedOnSE = "no", marketValueAgreedHMRC = "yes"),
          EMI.buildTaxableV3(withAllFields = true, disqualifyingEvent = "yes", sharesListedOnSE = "no", marketValueAgreedHMRC = "no")
        )
      )

      result shouldBe Json.parse("""{
                                   |"taxExercise":{
                                   |"taxExercise":[
                                   |{
                                   |"exerciseDate":"2015-06-04",
                                   |"disqualifyingEvent":true,
                                   |"natureOfDisqualifyingEvent":"3",
                                   |"individualTaxExercise":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfSharesAcquired":100.0,
                                   |"actualMarketValueAtGrantDate":10.1234,
                                   |"exercisePricePaidToAcquireAShare":10.1234,
                                   |"actualMarketValuePerShareAtExerciseDate":10.1234,
                                   |"unrestrictedMarketValuePerShareAtExerciseDate":10.1234,
                                   |"totalAmountPaidToAcquireTheShares":10.1234,
                                   |"sharesListedOnSE":false,
                                   |"marketValueAgreedHMRC": true,
                                   |"hmrcRef": "aa12345678",
                                   |"electionMadeUnderSection431":true,
                                   |"nicsElectionAgreementEnteredInto":true,
                                   |"amountSubjectToPAYE":10.1234
                                   |},
                                   |{
                                   |"exerciseDate":"2015-06-04",
                                   |"disqualifyingEvent":true,
                                   |"natureOfDisqualifyingEvent":"3",
                                   |"individualTaxExercise":{
                                   |"firstName":"First",
                                   |"secondName":"Second",
                                   |"surname":"Last",
                                   |"nino":"NINO",
                                   |"payeReference":"123/XZ55555555"
                                   |},
                                   |"numberOfSharesAcquired":100.0,
                                   |"actualMarketValueAtGrantDate":10.1234,
                                   |"exercisePricePaidToAcquireAShare":10.1234,
                                   |"actualMarketValuePerShareAtExerciseDate":10.1234,
                                   |"unrestrictedMarketValuePerShareAtExerciseDate":10.1234,
                                   |"totalAmountPaidToAcquireTheShares":10.1234,
                                   |"sharesListedOnSE":false,
                                   |"marketValueAgreedHMRC": false,
                                   |"electionMadeUnderSection431":true,
                                   |"nicsElectionAgreementEnteredInto":true,
                                   |"amountSubjectToPAYE":10.1234
                                   |}
                                   |]
                                   |}
                                   |}""".stripMargin)
    }
  }

}
