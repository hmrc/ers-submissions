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

import common._
import models._
import fixtures.Fixtures
import helpers.ERSTestHelper
import models.{ErsMetaData, ErsSummary, SchemeInfo}
import org.bson.BsonValue
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{any, anyString, eq => mockEq}
import org.mockito.Mockito._
import org.mongodb.scala.bson.{BsonString, ObjectId}
import org.mongodb.scala.result.UpdateResult
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import play.api.mvc.Request
import play.api.test.FakeRequest
import repositories.MetadataMongoRepository
import services.SubmissionService
import services.audit.AuditEvents
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggingAndRexceptions.{ErsLoggingAndAuditing, ResubmissionExceptionEmitter}

class ResubPresubmissionServiceSpec extends ERSTestHelper with BeforeAndAfterEach with EitherValues {
  implicit val hc: HeaderCarrier = new HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()

  val metadataMongoRepositoryResubmission: MetadataMongoRepository = mock[MetadataMongoRepository]
  val mockSchedulerLoggingAndAuditing: ErsLoggingAndAuditing = mock[ErsLoggingAndAuditing]
  val mockSubmissionService: SubmissionService = mock[SubmissionService]
  val mockAuditEvents: AuditEvents = mock[AuditEvents]
  val mockResubmissionExceptionEmitter: ResubmissionExceptionEmitter = app.injector.instanceOf[ResubmissionExceptionEmitter]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(metadataMongoRepositoryResubmission)
    reset(mockSubmissionService)
    reset(mockSchedulerLoggingAndAuditing)
    reset(mockAuditEvents)
  }

  val updateResult: UpdateResult = new UpdateResult() {
    override def wasAcknowledged(): Boolean = true
    override def getMatchedCount: Long = 1
    override def getModifiedCount: Long = 1
    override def getUpsertedId: BsonValue = BsonString("123")
  }

  val schemeInfo: SchemeInfo = SchemeInfo(
    schemeRef = "123",
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
    confirmationDateTime = new DateTime(),
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
      metadataMongoRepositoryResubmission,
      mockSchedulerLoggingAndAuditing,
      mockSubmissionService,
      mockAuditEvents
    )

    "return true if findAndUpdateByStatus is successful and returns a record" in {
      when(metadataMongoRepositoryResubmission.getFailedJobs(any(), any(), any()))
        .thenReturn(ERSEnvelope(Seq(new ObjectId())))
      when(metadataMongoRepositoryResubmission.findAndUpdateByStatus(any[List[ObjectId]](), any()))
        .thenReturn(ERSEnvelope(updateResult))
      when(metadataMongoRepositoryResubmission.findErsSummaries(any(), any()))
        .thenReturn(ERSEnvelope(scala.Seq(ersSummary)))
      when(mockSubmissionService.callProcessData(any(), any(), any())(any(), any()))
        .thenReturn(ERSEnvelope(true))

      val result = await(resubPresubmissionService.processFailedSubmissions(processFailedSubmissionsConfig).value)
      result.value shouldBe true
    }

    "return false if findAndUpdateByStatus is successful but returns None" in {
      when(metadataMongoRepositoryResubmission.getFailedJobs(any(), any(), any()))
        .thenReturn(ERSEnvelope(Seq(new ObjectId())))
      when(metadataMongoRepositoryResubmission.findAndUpdateByStatus(any[List[ObjectId]](), any()))
        .thenReturn(ERSEnvelope(updateResult))
      when(metadataMongoRepositoryResubmission.findErsSummaries(any(), any()))
        .thenReturn(ERSEnvelope(Seq(ersSummary)))
      when(mockSubmissionService.callProcessData(any(), any(), any())(any(), any()))
        .thenReturn(ERSEnvelope(false))

      val result = await(resubPresubmissionService.processFailedSubmissions(processFailedSubmissionsConfig).value)
      result.value shouldBe false
    }

    "rethrow ERSError() if such one occurs" in {
      when(metadataMongoRepositoryResubmission.getFailedJobs(any(), any(), any()))
        .thenReturn(ERSEnvelope(Seq(new ObjectId())))
      when(metadataMongoRepositoryResubmission.findAndUpdateByStatus(any(), any()))
        .thenReturn(ERSEnvelope(updateResult))
      when(metadataMongoRepositoryResubmission.findErsSummaries(any(), any()))
        .thenReturn(ERSEnvelope(scala.Seq(ersSummary)))
      when(mockSubmissionService.callProcessData(any(), any(), any())(any(), any()))
        .thenReturn(ERSEnvelope(JsonFromSheetsCreationError("error occurred")))

      val result = await(resubPresubmissionService.processFailedSubmissions(processFailedSubmissionsConfig).value)

      result.swap.value shouldBe JsonFromSheetsCreationError("error occurred")
    }

    "throw ERSError if Exception occurs" in {
      when(metadataMongoRepositoryResubmission.findAndUpdateByStatus(any(), any()))
        .thenReturn(ERSEnvelope(MongoGenericError("Something went wrong")))
      when(metadataMongoRepositoryResubmission.getFailedJobs(any(), any(), any()))
        .thenReturn(ERSEnvelope(Seq(new ObjectId())))
      when(metadataMongoRepositoryResubmission.findErsSummaries(any(), any()))
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

          when(metadataMongoRepositoryResubmission.getFailedJobs(any(), any(), any()))
            .thenReturn(ERSEnvelope(failedJobIds))
          when(metadataMongoRepositoryResubmission.findAndUpdateByStatus(mockEq(failedJobIds), any()))
            .thenReturn(ERSEnvelope(updateResult))
          when(metadataMongoRepositoryResubmission.findErsSummaries(mockEq(failedJobIds), any()))
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
      metadataMongoRepositoryResubmission,
      mockSchedulerLoggingAndAuditing,
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
}
