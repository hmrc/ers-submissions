/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.resubmission

import models._
import play.api.libs.json.{JsObject, JsValue, Json}

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, ZoneId, ZonedDateTime}
import scala.collection.mutable.ListBuffer
import scala.util.Random

object Fixtures {
  val invalidPayload: JsObject = Json.obj("invalid data" -> "test")

  def schemeInfo(schemaType: String = "EMI",
                 timestamp: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS),
                 schemeRef: String = "XA1100000000000"): SchemeInfo = SchemeInfo(
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

  def ersMetaData(schemaType: String, timestamp: Instant, schemeRef: String): ErsMetaData = ErsMetaData(
    schemeInfo = schemeInfo(schemaType, timestamp, schemeRef),
    ipRef = "127.0.0.0",
    aoRef = Some("123PA12345678"),
    empRef = "EMI - MyScheme - XA1100000000000 - 2014/15",
    agentRef = None,
    sapNumber = Some("sap-123456")
  )

  def schemeData(timestamp: Option[Instant] = None): SchemeData = SchemeData(
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
                      timestamp: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS),
                      schemeRef: String = "XA1100000000000"): ErsSummary = ErsSummary(
    bundleRef = bundleRef,
    isNilReturn = if (isNilReturn) "2" else "1",
    fileType = Some("ods"),
    confirmationDateTime = timestamp,
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
        timestamp = ZonedDateTime.now().toInstant.minus(random.nextLong(), ChronoUnit.MILLIS)
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

  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  def instantFromDate(date: String): Instant = LocalDate.parse(date, formatter).atStartOfDay(ZoneId.of("UTC")).toInstant

  val ersSummaries: Seq[JsObject] = Seq(
    Fixtures.buildErsSummary(transferStatus = Some("passed"), schemaType = "CSOP", timestamp = instantFromDate("30/04/2022")),
    Fixtures.buildErsSummary(transferStatus = Some("successResubmit"), schemaType = "CSOP", timestamp = instantFromDate("30/04/2021")),
    Fixtures.buildErsSummary(transferStatus = Some("failed"), schemaType = "NOT_CSOP", timestamp = instantFromDate("30/04/2000")),
    Fixtures.buildErsSummary(transferStatus = Some("failed"), schemaType = "CSOP", timestamp = instantFromDate("30/04/2023")),
    Fixtures.buildErsSummary(transferStatus = Some("failed"), schemaType = "CSOP", timestamp = instantFromDate("10/05/2023")),
    Fixtures.buildErsSummary(transferStatus = Some("failed"), schemaType = "CSOP", timestamp = instantFromDate("20/05/2023"))
  ).map(Json.toJsObject(_))

  val schemeData: Seq[JsObject] = Seq(
    SchemeData(CSOP.schemeInfo.copy(timestamp = instantFromDate("30/04/2023")), "CSOP_OptionsRCL_V4", None, Some(ListBuffer(CSOP.buildOptionsRCL(true, "yes")))),
    SchemeData(CSOP.schemeInfo.copy(timestamp = instantFromDate("10/05/2023")), "CSOP_OptionsRCL_V4", None, Some(ListBuffer(CSOP.buildOptionsRCL(true, "yes")))),
    SchemeData(CSOP.schemeInfo.copy(timestamp = instantFromDate("20/05/2023")), "CSOP_OptionsRCL_V4", None, Some(ListBuffer(CSOP.buildOptionsRCL(true, "yes"))))
  ).map(Json.toJsObject(_))
}
