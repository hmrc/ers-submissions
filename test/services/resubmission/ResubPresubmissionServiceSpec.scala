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

import org.mongodb.scala.result.UpdateResult
import fixtures.Fixtures
import helpers.ERSTestHelper
import models.{ADRTransferException, ErsMetaData, ErsSummary, ResubmissionException, SchemeInfo}
import org.bson.BsonValue
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{any, anyString, eq => mockEq}
import org.mockito.Mockito._
import org.mongodb.scala.bson.{BsonString, ObjectId}
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.Request
import play.api.test.FakeRequest
import repositories.MetadataMongoRepository
import services.SubmissionService
import services.audit.AuditEvents
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggingAndRexceptions.{ErsLoggingAndAuditing, ResubmissionExceptionEmitter}

import scala.concurrent.Future

class ResubPresubmissionServiceSpec extends ERSTestHelper with BeforeAndAfterEach {
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

  implicit val processFailedSubmissionsConfig: ProcessFailedSubmissionsConfig = ProcessFailedSubmissionsConfig(
    resubmissionLimit = 100,
    searchStatusList = List.empty[String],
    schemeRefList = None,
    resubmitScheme = None,
    dateTimeFilter = None,
    failedStatus = "failed",
    resubmitSuccessStatus = "resubmisionSucess",
    legacyRefList = Seq.empty[String]
  )

  "processFailedSubmissions" should {

    val resubPresubmissionService: ResubPresubmissionService = new ResubPresubmissionService(
      metadataMongoRepositoryResubmission,
      mockSchedulerLoggingAndAuditing,
      mockSubmissionService,
      mockAuditEvents,
      mockResubmissionExceptionEmitter
    )

    "return the result of startResubmission if findAndUpdateByStatus is successful and returns a record" in {
      when(metadataMongoRepositoryResubmission.findAndUpdateByStatus(any[List[ObjectId]]()))
        .thenReturn(Future.successful(updateResult))
      when(metadataMongoRepositoryResubmission.getFailedJobs(any())(any()))
        .thenReturn(Future.successful(Seq(new ObjectId())))
      when(metadataMongoRepositoryResubmission.findErsSummaries(any()))
        .thenReturn(Future.successful(Seq(ersSummary)))
      when(mockSubmissionService.callProcessData(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(false))

      val result = await(resubPresubmissionService.processFailedSubmissions())
      result shouldBe false
    }

    "return None if findAndUpdateByStatus is successful but returns None" in {
      when(metadataMongoRepositoryResubmission.findAndUpdateByStatus(any[List[ObjectId]]()))
        .thenReturn(Future.successful(updateResult))
      when(metadataMongoRepositoryResubmission.getFailedJobs(any())(any()))
        .thenReturn(Future.successful(Seq(new ObjectId())))
      when(metadataMongoRepositoryResubmission.findErsSummaries(any()))
        .thenReturn(Future.successful(Seq(ersSummary)))
      when(mockSubmissionService.callProcessData(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(false))

      val result = await(resubPresubmissionService.processFailedSubmissions())
      result shouldBe false
    }

    "rethrow ResubmissionException if such one occurs" in {
      when(metadataMongoRepositoryResubmission.findAndUpdateByStatus(any()))
        .thenReturn(Future.failed(ResubmissionException("test message", "test context", Some(Fixtures.schemeInfo))))
      when(metadataMongoRepositoryResubmission.getFailedJobs(any())(any()))
        .thenReturn(Future.successful(Seq(new ObjectId())))
      when(metadataMongoRepositoryResubmission.findErsSummaries(any()))
        .thenReturn(Future.successful(Seq(ersSummary)))
      when(mockSubmissionService.callProcessData(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(false))

      val result = intercept[ResubmissionException] {
        await(resubPresubmissionService.processFailedSubmissions())
      }
      result.message shouldBe "test message"
      result.context shouldBe "test context"
      result.schemeInfo.get shouldBe Fixtures.schemeInfo
    }

    "throw ResubmissionException if Exception occurs" in {
      when(metadataMongoRepositoryResubmission.findAndUpdateByStatus(any()))
        .thenReturn(Future.failed(new Exception("test message")))
      when(metadataMongoRepositoryResubmission.getFailedJobs(any())(any()))
        .thenReturn(Future.successful(Seq(new ObjectId())))
      when(metadataMongoRepositoryResubmission.findErsSummaries(any()))
        .thenReturn(Future.successful(Seq(ersSummary)))
      when(mockSubmissionService.callProcessData(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(false))

      val result = intercept[ResubmissionException] {
        await(resubPresubmissionService.processFailedSubmissions())
      }
      result.message shouldBe "Searching for data to be resubmitted"
      result.context shouldBe "ResubPresubmissionService.processFailedSubmissions.findAndUpdateByStatus"
      result.schemeInfo shouldBe None
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
          when(metadataMongoRepositoryResubmission.getFailedJobs(any())(any()))
            .thenReturn(Future.successful(failedJobIds))
          when(metadataMongoRepositoryResubmission.findAndUpdateByStatus(failedJobIds))
            .thenReturn(Future.successful(updateResult))
          when(metadataMongoRepositoryResubmission.findErsSummaries(failedJobIds))
            .thenReturn(Future.successful(ersSummaries))
          when(mockSubmissionService.callProcessData(mockEq(firstErsSummary), any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(firstResult))
          when(mockSubmissionService.callProcessData(mockEq(secondErsSummary), any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(secondResult))

          val result = await(
            resubPresubmissionService
              .processFailedSubmissions()
          )
          result shouldBe expectedResult
        }

    }
  }

  "startResubmission" should {
    val resubPresubmissionService: ResubPresubmissionService = new ResubPresubmissionService(
      metadataMongoRepositoryResubmission,
      mockSchedulerLoggingAndAuditing,
      mockSubmissionService,
      mockAuditEvents,
      mockResubmissionExceptionEmitter
    )

    "return the result of callProcessData if ErsSubmissions is successfully extracted" in {
      when(mockSubmissionService.callProcessData(any[ErsSummary](), anyString(), anyString(), any())(any(), any()))
        .thenReturn(Future.successful(true))

      val result = await(resubPresubmissionService.startResubmission(Fixtures.metadata))
      result shouldBe true
    }

    "audit failed submission if callProcessData throws exception" in {
      when(mockSubmissionService.callProcessData(any[ErsSummary](), anyString(), anyString(), any())(any(), any()))
        .thenReturn(Future.failed(new RuntimeException("test message")))

      val result = intercept[ResubmissionException] {
        await(resubPresubmissionService.startResubmission(Fixtures.metadata))
      }
      result.message shouldBe "Resubmitting data to ADR - Exception: test message"
      result.context shouldBe "ResubPresubmissionService.startResubmission.callProcessData"
      result.schemeInfo.get shouldBe Fixtures.metadata.metaData.schemeInfo
    }

    "throw ResubmissionException if ADRTransferException occurs" in {
      when(mockSubmissionService.callProcessData(any[ErsSummary](), anyString(), anyString(), any())(any(), any()))
        .thenReturn(Future.failed(ADRTransferException(Fixtures.EMIMetaData, "test message", "test context")))

      val result = intercept[ResubmissionException] {
        await(resubPresubmissionService.startResubmission(Fixtures.metadata))
      }
      result.message shouldBe "Resubmitting data to ADR - ADRTransferException: test message"
      result.context shouldBe "test context"
      result.schemeInfo.get shouldBe Fixtures.metadata.metaData.schemeInfo
    }

    "throw ResubmissionException if Exception occurs" in {
      when(mockSubmissionService.callProcessData(any[ErsSummary](), anyString(), anyString(), any())(any(), any()))
        .thenReturn(Future.failed(new Exception("test message")))

      val result = intercept[ResubmissionException] {
        await(resubPresubmissionService.startResubmission(Fixtures.metadata))
      }
      result.message shouldBe "Resubmitting data to ADR - Exception: test message"
      result.context shouldBe "ResubPresubmissionService.startResubmission.callProcessData"
      result.schemeInfo.get shouldBe Fixtures.metadata.metaData.schemeInfo
    }
  }
}
