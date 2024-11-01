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

package services.resubmission

import com.mongodb.client.result.UpdateResult
import common._
import models._
import fixtures.Fixtures
import helpers.ERSTestHelper
import models.{ErsMetaData, ErsSummary, SchemeInfo}
import org.bson.BsonValue
import org.mockito.ArgumentMatchers.{any, anyString, eq => mockEq}
import org.mockito.Mockito._
import org.mongodb.scala.bson.{BsonString, ObjectId}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import play.api.mvc.Request
import play.api.test.FakeRequest
import repositories.{MetadataMongoRepository, PresubmissionMongoRepository, Selectors}
import services.SubmissionService
import services.audit.AuditEvents
import uk.gov.hmrc.http.HeaderCarrier
import play.api.libs.json._

import java.time.Instant
import java.time.temporal.ChronoUnit

class ResubPresubmissionServiceSpec extends ERSTestHelper with BeforeAndAfterEach with EitherValues {
  implicit val hc: HeaderCarrier = new HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()

  val mockMetadataMongoRepository: MetadataMongoRepository = mock[MetadataMongoRepository]
  val mockPresubmissionMongoRepository: PresubmissionMongoRepository = mock[PresubmissionMongoRepository]

  val mockSubmissionService: SubmissionService = mock[SubmissionService]
  val mockAuditEvents: AuditEvents = mock[AuditEvents]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMetadataMongoRepository)
    reset(mockSubmissionService)
    reset(mockAuditEvents)
  }

  val updateResult: UpdateResult = new UpdateResult() {
    override def wasAcknowledged(): Boolean = true
    override def getMatchedCount: Long = 1
    override def getModifiedCount: Long = 1
    override def getUpsertedId: BsonValue = BsonString("123")
  }

  val failedUpdateResult: UpdateResult = UpdateResult.unacknowledged()

  val schemeInfo: SchemeInfo = SchemeInfo(
    schemeRef = "123",
    timestamp = Instant.parse("2023-10-07T10:15:30.00Z"),
    schemeId = "123",
    taxYear = "123",
    schemeName = "123",
    schemeType = "123"
  )

  val ersMetaData: ErsMetaData = ErsMetaData(
    schemeInfo = schemeInfo,
    ipRef = "123",
    aoRef = None,
    empRef = "213",
    agentRef = None,
    sapNumber = None
  )

  val ersSummary: ErsSummary = ErsSummary(
    bundleRef = "123",
    isNilReturn = "456",
    fileType = None,
    confirmationDateTime = Instant.now().truncatedTo(ChronoUnit.MILLIS),
    metaData = ersMetaData,
    altAmendsActivity = None,
    alterationAmends = None,
    groupService = None,
    schemeOrganiser = None,
    companies = None,
    trustees = None,
    nofOfRows = None,
    transferStatus  = None
  )

  val processFailedSubmissionsConfig: ProcessFailedSubmissionsConfig = ProcessFailedSubmissionsConfig(
    resubmissionLimit = 100,
    searchStatusList = List.empty[String],
    schemeRefList = None,
    resubmitScheme = None,
    dateTimeFilter = None,
    failedStatus = "failed",
    resubmitSuccessStatus = "resubmisionSucess"
  )

  "processFailedSubmissions" should {
    val resubPresubmissionService: ResubPresubmissionService = new ResubPresubmissionService(
      mockMetadataMongoRepository,
      mockPresubmissionMongoRepository,
      mockSubmissionService,
      mockAuditEvents
    )

    "return true if findAndUpdateByStatus is successful and returns a record" in {
      when(mockMetadataMongoRepository.getFailedJobs(any(), any(), any()))
        .thenReturn(ERSEnvelope(Seq(new ObjectId())))
      when(mockMetadataMongoRepository.findAndUpdateByStatus(any[List[ObjectId]](), any()))
        .thenReturn(ERSEnvelope(updateResult))
      when(mockMetadataMongoRepository.findErsSummaries(any(), any()))
        .thenReturn(ERSEnvelope(scala.Seq(ersSummary)))
      when(mockSubmissionService.callProcessData(any(), any(), any())(any(), any()))
        .thenReturn(ERSEnvelope(true))

      val result = await(resubPresubmissionService.processFailedSubmissions(processFailedSubmissionsConfig).value)
      result.value shouldBe true
    }

    "return false if findAndUpdateByStatus is successful but returns None" in {
      when(mockMetadataMongoRepository.getFailedJobs(any(), any(), any()))
        .thenReturn(ERSEnvelope(Seq(new ObjectId())))
      when(mockMetadataMongoRepository.findAndUpdateByStatus(any[List[ObjectId]](), any()))
        .thenReturn(ERSEnvelope(updateResult))
      when(mockMetadataMongoRepository.findErsSummaries(any(), any()))
        .thenReturn(ERSEnvelope(Seq(ersSummary)))
      when(mockSubmissionService.callProcessData(any(), any(), any())(any(), any()))
        .thenReturn(ERSEnvelope(false))

      val result = await(resubPresubmissionService.processFailedSubmissions(processFailedSubmissionsConfig).value)
      result.value shouldBe false
    }

    "rethrow ERSError() if such one occurs" in {
      when(mockMetadataMongoRepository.getFailedJobs(any(), any(), any()))
        .thenReturn(ERSEnvelope(Seq(new ObjectId())))
      when(mockMetadataMongoRepository.findAndUpdateByStatus(any(), any()))
        .thenReturn(ERSEnvelope(updateResult))
      when(mockMetadataMongoRepository.findErsSummaries(any(), any()))
        .thenReturn(ERSEnvelope(scala.Seq(ersSummary)))
      when(mockSubmissionService.callProcessData(any(), any(), any())(any(), any()))
        .thenReturn(ERSEnvelope(JsonFromSheetsCreationError("error occurred")))

      val result = await(resubPresubmissionService.processFailedSubmissions(processFailedSubmissionsConfig).value)

      result.swap.value shouldBe JsonFromSheetsCreationError("error occurred")
    }

    "throw ERSError if Exception occurs" in {
      when(mockMetadataMongoRepository.findAndUpdateByStatus(any(), any()))
        .thenReturn(ERSEnvelope(MongoGenericError("Something went wrong")))
      when(mockMetadataMongoRepository.getFailedJobs(any(), any(), any()))
        .thenReturn(ERSEnvelope(Seq(new ObjectId())))
      when(mockMetadataMongoRepository.findErsSummaries(any(), any()))
        .thenReturn(ERSEnvelope(scala.Seq(ersSummary)))
      when(mockSubmissionService.callProcessData(any(), any(), any())(any(), any()))
        .thenReturn(ERSEnvelope(false))

      val result = await(resubPresubmissionService.processFailedSubmissions(processFailedSubmissionsConfig).value)

      result.swap.value shouldBe MongoGenericError("Something went wrong")
    }

    Seq(
      (true, true, true),
      (false, true, false),
      (false, false, false)
    ) foreach {
      case (expectedResult, firstResult, secondResult) =>
        s"return $expectedResult if getFailedJobs returns two records where the first result returns $firstResult and second returns $secondResult " +
          s"when processed by processFailedSubmissions" in {
          val firstErsSummary = ersSummary
          val secondErsSummary = ersSummary.copy(bundleRef = "456")
          val failedJobIds = Seq(new ObjectId("6450e4f47e56c12cd1259908"), new ObjectId("6450e4f47e56c12cd1259910"))
          val ersSummaries = Seq(firstErsSummary, secondErsSummary)

          when(mockMetadataMongoRepository.getFailedJobs(any(), any(), any()))
            .thenReturn(ERSEnvelope(failedJobIds))
          when(mockMetadataMongoRepository.findAndUpdateByStatus(mockEq(failedJobIds), any()))
            .thenReturn(ERSEnvelope(updateResult))
          when(mockMetadataMongoRepository.findErsSummaries(mockEq(failedJobIds), any()))
            .thenReturn(ERSEnvelope(ersSummaries))
          when(mockSubmissionService.callProcessData(mockEq(firstErsSummary), any(), any())(any(), any()))
            .thenReturn(ERSEnvelope(firstResult))
          when(mockSubmissionService.callProcessData(mockEq(secondErsSummary), any(), any())(any(), any()))
            .thenReturn(ERSEnvelope(secondResult))

          val result = await(
            resubPresubmissionService
              .processFailedSubmissions(processFailedSubmissionsConfig).value
          ).value

          result shouldBe expectedResult
        }
    }
  }

  "startResubmission" should {
    val resubPresubmissionService: ResubPresubmissionService = new ResubPresubmissionService(
      mockMetadataMongoRepository,
      mockPresubmissionMongoRepository,
      mockSubmissionService,
      mockAuditEvents
    )

    "return the result of callProcessData if ErsSubmissions is successfully extracted" in {
      when(mockSubmissionService.callProcessData(any[ErsSummary](), anyString(), anyString())(any(), any()))
        .thenReturn(ERSEnvelope(true))

      val result = await(resubPresubmissionService.startResubmission(Fixtures.metadata, processFailedSubmissionsConfig).value).value
      result shouldBe true
    }

    "audit failed submission if callProcessData returns error" in {
      when(mockSubmissionService.callProcessData(any[ErsSummary](), anyString(), anyString())(any(), any()))
        .thenReturn(ERSEnvelope(ResubmissionError()))

      val result = await(resubPresubmissionService.startResubmission(Fixtures.metadata, processFailedSubmissionsConfig).value)

      result.swap.value shouldBe ResubmissionError()
    }

    "return ResubmissionError if ADRTransferError occurs" in {
      when(mockSubmissionService.callProcessData(any[ErsSummary](), anyString(), anyString())(any(), any()))
        .thenReturn(ERSEnvelope(ADRTransferError()))

      val result = await(resubPresubmissionService.startResubmission(Fixtures.metadata, processFailedSubmissionsConfig).value)

      result.swap.value shouldBe ADRTransferError()
    }
  }

  "getPreSubSelectedSchemeRefDetailsMessage" should {

    val resubPresubmissionService: ResubPresubmissionService = new ResubPresubmissionService(
      mockMetadataMongoRepository,
      mockPresubmissionMongoRepository,
      mockSubmissionService,
      mockAuditEvents
    )

    def getSimpleSchemaData(schemeInfo: SchemeInfo) = SchemeData(
      schemeInfo = schemeInfo,
      sheetName = "TestSheet",
      numberOfParts = None,
      data = None
    )

    "produce a log message with only the submissions returned from getStatusForSelectedSchemes" in {

      val taxYears: Seq[String] = Seq("2015/16", "2016/17", "2017/18", "2018/19", "2019/20", "2020/21", "2021/22")
      val schemeDataAsJsObject: Seq[JsObject] = taxYears
        .map(taxYear =>
          Json.toJson(getSimpleSchemaData(schemeInfo.copy(taxYear = taxYear))).as[JsObject]
        )

      def createExpectedStringFromTaxYear(taxYear: String): String =
        s"schemaRef: 123, schemaType: 123, taxYear: $taxYear, timestamp: 2023-10-07T10:15:30Z"

      val expectedOutput = s"[ResubmissionService] PreSubSelectedSchemeRefLogs - Selected scheme details: " +
        s"${taxYears.map(createExpectedStringFromTaxYear).mkString("\n", "\n", "\n")}"

      when(mockPresubmissionMongoRepository.getStatusForSelectedSchemes(anyString(), any()))
        .thenReturn(ERSEnvelope(schemeDataAsJsObject))

      val result: String = await(resubPresubmissionService.getPreSubSelectedSchemeRefDetailsMessage(processFailedSubmissionsConfig).value).value

      result shouldEqual expectedOutput
    }

    "produce a message indicating it could not find any submissions when getStatusForSelectedSchemes returns an empty seq" in {
      when(mockPresubmissionMongoRepository.getStatusForSelectedSchemes(anyString(), any()))
        .thenReturn(ERSEnvelope(Seq.empty[JsObject]))

      val expectedOutput = s"[ResubmissionService] PreSubSelectedSchemeRefLogs - Could not find any records for the selected scheme reference"

      val result: String = await(resubPresubmissionService.getPreSubSelectedSchemeRefDetailsMessage(processFailedSubmissionsConfig).value).value

      result shouldEqual expectedOutput
    }

    "produce a message indicating there are to many submissions to log out when getStatusForSelectedSchemes returns > 50 records" in {
      when(mockPresubmissionMongoRepository.getStatusForSelectedSchemes(anyString(), any()))
        .thenReturn(ERSEnvelope(Seq.fill(51)(getSimpleSchemaData(schemeInfo)).map(Json.toJson(_).as[JsObject])))

      val expectedOutput = s"[ResubmissionService] PreSubSelectedSchemeRefLogs - Selected schemes have more then 50 records (51 records selected)"

      val result: String = await(resubPresubmissionService.getPreSubSelectedSchemeRefDetailsMessage(processFailedSubmissionsConfig).value).value

      result shouldEqual expectedOutput
    }
  }

  "getMetadataSelectedSchemeRefDetailsMessage" should {

    val resubPresubmissionService: ResubPresubmissionService = new ResubPresubmissionService(
      mockMetadataMongoRepository,
      mockPresubmissionMongoRepository,
      mockSubmissionService,
      mockAuditEvents
    )

    def getSimpleErsSummary(schemeInfo: SchemeInfo, transferStatus: Option[String] = Some("saved")): ErsSummary = ersSummary
      .copy(
        metaData = ersMetaData.copy(schemeInfo = schemeInfo),
        transferStatus = transferStatus
      )

    "produce a log message with only the submissions returned from getStatusForSelectedSchemes" in {

      def createExpectedStringFromTaxYear(taxYear: String, transferStatus: Option[String]): String =
        s"schemaRef: 123, schemaType: 123, taxYear: $taxYear, transferStatus: ${transferStatus.get}, timestamp: 2023-10-07T10:15:30Z"

      val taxYearAndTransferStatus: Seq[(String, Option[String])] = Seq(
        ("2015/16", Some("saved")),
        ("2016/17", Some("saved")),
        ("2017/18", Some("saved")),
        ("2018/19", Some("saved")),
        ("2019/20", Some("saved")),
        ("2020/21", Some("saved")),
        ("2021/22", Some("saved"))
      )

      val taxYearAndTransferStatusString: String = taxYearAndTransferStatus
      .map{
        case (taxYear, transferStatus: Option[String]) =>
          createExpectedStringFromTaxYear(taxYear, transferStatus)
      }.mkString("\n", "\n", "\n")

      val ersSummaryAsJsObject: Seq[JsObject] = taxYearAndTransferStatus
        .map { case (taxYear, transferStatus) =>
          Json.toJson(getSimpleErsSummary(schemeInfo.copy(taxYear = taxYear), transferStatus = transferStatus)).as[JsObject]
        }

      val expectedOutput = s"[ResubmissionService] MetaDataSelectedSchemeRefLogs - Selected scheme details: " +
        s"$taxYearAndTransferStatusString"

      when(mockMetadataMongoRepository.getStatusForSelectedSchemes(anyString(), any()))
        .thenReturn(ERSEnvelope(ersSummaryAsJsObject))

      val result: String = await(resubPresubmissionService.getMetadataSelectedSchemeRefDetailsMessage(processFailedSubmissionsConfig).value).value

      result shouldEqual expectedOutput
    }

    "produce a log message using the default value for the transfer status when None can be found" in {

      val ersSummaryAsJsObject: Seq[JsObject] = Seq(
        Json
          .toJson(getSimpleErsSummary(schemeInfo.copy(taxYear = "2016/17"), transferStatus = None))
          .as[JsObject]
      )

      val expectedOutput = s"[ResubmissionService] MetaDataSelectedSchemeRefLogs - Selected scheme details: \n" +
        s"schemaRef: 123, schemaType: 123, taxYear: 2016/17, transferStatus: transfer status is not defined, timestamp: 2023-10-07T10:15:30Z\n"

      when(mockMetadataMongoRepository.getStatusForSelectedSchemes(anyString(), any()))
        .thenReturn(ERSEnvelope(ersSummaryAsJsObject))

      val result: String = await(resubPresubmissionService.getMetadataSelectedSchemeRefDetailsMessage(processFailedSubmissionsConfig).value).value

      result shouldEqual expectedOutput
    }

    "produce a message indicating it could not find any submissions when getStatusForSelectedSchemes returns an empty seq" in {
      when(mockMetadataMongoRepository.getStatusForSelectedSchemes(anyString(), any()))
        .thenReturn(ERSEnvelope(Seq.empty[JsObject]))

      val expectedOutput = s"[ResubmissionService] MetaDataSelectedSchemeRefLogs - Could not find any records for the selected scheme reference"

      val result: String = await(resubPresubmissionService.getMetadataSelectedSchemeRefDetailsMessage(processFailedSubmissionsConfig).value).value

      result shouldEqual expectedOutput
    }

    "produce a message indicating there are to many submissions to log out when getStatusForSelectedSchemes returns > 50 records" in {
      when(mockMetadataMongoRepository.getStatusForSelectedSchemes(anyString(), any()))
        .thenReturn(ERSEnvelope(Seq.fill(51)(getSimpleErsSummary(schemeInfo)).map(Json.toJson(_).as[JsObject])))

      val expectedOutput = s"[ResubmissionService] MetaDataSelectedSchemeRefLogs - Selected schemes have more then 50 records (51 records selected)"

      val result: String = await(resubPresubmissionService.getMetadataSelectedSchemeRefDetailsMessage(processFailedSubmissionsConfig).value).value

      result shouldEqual expectedOutput
    }
  }
}
