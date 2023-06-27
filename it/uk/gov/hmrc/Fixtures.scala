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

package uk.gov.hmrc

import _root_.play.api.libs.json._
import models._
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.joda.time.{DateTime, DateTimeZone}

import scala.collection.mutable.ListBuffer
import scala.util.Random

object Fixtures {
  val invalidPayload: JsObject = Json.obj("invalid data" -> "test")

  def schemeInfo(schemaType: String = "EMI",
                 timestamp: DateTime = DateTime.now(DateTimeZone.UTC),
                 schemeRef: String = "XA1100000000000"): SchemeInfo = SchemeInfo (
    schemeRef = schemeRef,
    timestamp = timestamp,
    schemeId = "123PA12345678",
    taxYear = "2014/15",
    schemeName = "EMI",
    schemeType = schemaType
  )
  def schemeInfoPayload(schemeInfo: SchemeInfo): JsValue = Json.toJson(schemeInfo)

  val submissionsSchemeData: SubmissionsSchemeData = SubmissionsSchemeData(
    schemeInfo(),
    "EMI40_Adjustments_V4",
    UpscanCallback("EMI40_Adjustments_V4", "http://localhost:19000/fakeDownload"),
    1
  )
  def submissionsSchemeDataJson(submissionsSchemeData: SubmissionsSchemeData): JsObject = Json.toJson(submissionsSchemeData).as[JsObject]

  def ersMetaData(schemaType: String, timestamp: DateTime, schemeRef: String): ErsMetaData = ErsMetaData(
    schemeInfo = schemeInfo(schemaType, timestamp, schemeRef),
    ipRef = "127.0.0.0",
    aoRef = Some("123PA12345678"),
    empRef = "EMI - MyScheme - XA1100000000000 - 2014/15",
    agentRef = None,
    sapNumber = Some("sap-123456")
  )

  def schemeData(timestamp: Option[DateTime] = None): SchemeData = SchemeData(
    timestamp.fold(schemeInfo())(ts => schemeInfo(timestamp = ts)),
    "EMI40_Adjustments_V4",
    None,
    Some(
      ListBuffer(
        Seq("no", "no", "yes", "3", "2015-12-09", "John", "", "Doe", "AA123456A", "123/XZ55555555", "10.1234", "100.12", "10.1234", "10.1234"),
        Seq("no", "no", "no", "", "2015-12-09", "John", "", "Doe", "AA123456A", "123/XZ55555555", "10.1234", "100.12", "10.1234", "10.1234"),
        Seq("yes", "", "", "", "2015-12-09", "John", "Middle", "Doe", "AA123456A", "123/XZ55555555", "10.1234", "100.12", "10.1234", "10.1234")
      )
    )
  )

  val schemeDataPayload: JsValue = Json.toJson(schemeData())

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

  def buildErsSummary(isNilReturn: Boolean = false,
                      transferStatus: Option[String] = Some("saved"),
                      schemaType: String = "EMI",
                      bundleRef: String = "testbundle",
                      timestamp: DateTime = DateTime.now(DateTimeZone.UTC),
                      schemeRef: String = "XA1100000000000"): ErsSummary = ErsSummary(
    bundleRef = bundleRef,
    isNilReturn = if(isNilReturn) "2" else "1",
    fileType = Some("ods"),
    confirmationDateTime = DateTime.now(DateTimeZone.UTC),
    metaData = ersMetaData(schemaType, timestamp, schemeRef),
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
    nofOfRows = None,
    transferStatus = transferStatus
  )

  def buildErsSummaryPayload(ersSummary: ErsSummary): JsValue =
    Json.toJson(ersSummary)

  def generateListOfErsSummaries(numberRecords: Int,
                                 isNilReturn: Boolean = false,
                                 transferStatus: Option[String] = Some("failed"),
                                 schemaType: String = "CSOP",
                                 bundleRef: String = "testbundle"
                                ): Seq[ErsSummary] = {
    val random = new Random()
    Seq.fill(numberRecords)(
      Fixtures.buildErsSummary(
        isNilReturn = isNilReturn,
        transferStatus = transferStatus,
        schemaType = schemaType,
        bundleRef = bundleRef,
        timestamp = DateTime.now(DateTimeZone.UTC).minus(random.nextLong())
      )
    )
  }

  def generateListOfErsSummaries(): Seq[ErsSummary] = {

    val failedJobs: Seq[ErsSummary] = generateListOfErsSummaries(
      numberRecords = 10
    )

    val passedJobs: Seq[ErsSummary] = generateListOfErsSummaries(
      numberRecords = 5,
      transferStatus = Some("passed")
    )

    failedJobs ++ passedJobs
  }

  def generatePresubmissionRecordsForMetadata(ersSummaries: Seq[ErsSummary]): Seq[SchemeData] = {
    ersSummaries.map { summary =>
      SchemeData(CSOP.schemeInfo.copy(timestamp = summary.metaData.schemeInfo.timestamp), "CSOP_OptionsRCL_V4", None, Some(ListBuffer(CSOP.buildOptionsRCL(true, "yes"))))
    }
  }

  val failedJobsWithDifferentBundleRef: Seq[ErsSummary] = generateListOfErsSummaries(numberRecords = 10)

  val formatter: DateTimeFormatter = DateTimeFormat.forPattern("dd/MM/yyyy")
  val ersSummaries: Seq[JsObject] = Seq(
    Fixtures.buildErsSummary(transferStatus = Some("passed"), schemaType = "CSOP", timestamp = DateTime.parse("30/04/2022", formatter)),
    Fixtures.buildErsSummary(transferStatus = Some("successResubmit"), schemaType = "CSOP", timestamp = DateTime.parse("30/04/2021", formatter)),
    Fixtures.buildErsSummary(transferStatus = Some("failed"), schemaType = "NOT_CSOP", timestamp = DateTime.parse("30/04/2000", formatter)),
    Fixtures.buildErsSummary(transferStatus = Some("failed"), schemaType = "CSOP", timestamp = DateTime.parse("30/04/2023", formatter)),
    Fixtures.buildErsSummary(transferStatus = Some("failed"), schemaType = "CSOP", timestamp = DateTime.parse("10/05/2023", formatter)),
    Fixtures.buildErsSummary(transferStatus = Some("failed"), schemaType = "CSOP", timestamp = DateTime.parse("20/05/2023", formatter)),
  ).map(Json.toJsObject(_))

  val schemeData: Seq[JsObject] = Seq(
    SchemeData(CSOP.schemeInfo.copy(timestamp = DateTime.parse("30/04/2023", formatter)), "CSOP_OptionsRCL_V4", None, Some(ListBuffer(CSOP.buildOptionsRCL(true, "yes")))),
    SchemeData(CSOP.schemeInfo.copy(timestamp = DateTime.parse("10/05/2023", formatter)), "CSOP_OptionsRCL_V4", None, Some(ListBuffer(CSOP.buildOptionsRCL(true, "yes")))),
    SchemeData(CSOP.schemeInfo.copy(timestamp = DateTime.parse("20/05/2023", formatter)), "CSOP_OptionsRCL_V4", None, Some(ListBuffer(CSOP.buildOptionsRCL(true, "yes"))))
  ).map(Json.toJsObject(_))
}
