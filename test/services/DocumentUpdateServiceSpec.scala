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

package services

import com.mongodb.client.result.UpdateResult
import helpers.ERSTestHelper
import models.{UpdateRequestAcknowledged, UpdateRequestAcknowledgedNothingToUpdate, UpdateRequestNotAcknowledged}
import org.bson.types.ObjectId
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import repositories.{LockRepositoryProvider, PresubmissionMongoRepository}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.Await.result
import scala.concurrent.Future
import scala.concurrent.duration.Duration

class DocumentUpdateServiceSpec extends ERSTestHelper with BeforeAndAfterEach {

  val mockPresubmissionMongo: PresubmissionMongoRepository = mock[PresubmissionMongoRepository]
  val mockLockKeeper: LockRepositoryProvider = mock[LockRepositoryProvider]
  val mockServicesConfig: ServicesConfig = mock[ServicesConfig]

  class Setup() {
    val documentUpdateService: DefaultDocumentUpdateService = new DefaultDocumentUpdateService(
      mockPresubmissionMongo,
      mockLockKeeper,
      mockServicesConfig
    )
  }

  val successfulUpdateResult: UpdateResult = UpdateResult.acknowledged(0, 2, null)
  val successfulUpdateResultNoModified: UpdateResult = UpdateResult.acknowledged(0, 0, null)

  override def beforeEach(): Unit = {
    reset(mockPresubmissionMongo)
    reset(mockLockKeeper)
    reset(mockServicesConfig)
  }

  "updateMissingCreatedAtFields" should {
    "run the update document scheduler job and return successful update result with modified count != 0" when {
      "there are documents without createdAt fields" in new Setup {
        when(mockPresubmissionMongo.getDocumentIdsWithoutCreatedAtField(any()))
          .thenReturn(Future.successful(Seq(ObjectId.get(), ObjectId.get())))
        when(mockPresubmissionMongo.addCreatedAtField(any()))
          .thenReturn(Future.successful(Right(successfulUpdateResult)))

        val (modifiedDocuments, updateMessage) = result(documentUpdateService.updateMissingCreatedAtFields(), Duration.Inf)

        modifiedDocuments shouldBe 2
        updateMessage shouldBe UpdateRequestAcknowledged(2)

        verify(mockPresubmissionMongo, times(1)).getDocumentIdsWithoutCreatedAtField(any[Int])
        verify(mockPresubmissionMongo, times(1)).addCreatedAtField(any[Seq[ObjectId]])
      }
    }

    "run the update document scheduler job and return successful update result with modified count == 0" when {
      "there are no documents without createdAt fields" in new Setup {
        when(mockPresubmissionMongo.getDocumentIdsWithoutCreatedAtField(any()))
          .thenReturn(Future.successful(Seq()))
        when(mockPresubmissionMongo.addCreatedAtField(any()))
          .thenReturn(Future.successful(Right(successfulUpdateResultNoModified)))

        val (modifiedDocuments, updateMessage) = result(documentUpdateService.updateMissingCreatedAtFields(), Duration.Inf)

        modifiedDocuments shouldBe 0
        updateMessage shouldBe UpdateRequestAcknowledgedNothingToUpdate

        verify(mockPresubmissionMongo, times(1)).getDocumentIdsWithoutCreatedAtField(any[Int])
        verify(mockPresubmissionMongo, times(0)).addCreatedAtField(any[Seq[ObjectId]])
      }
    }

    "run the update document scheduler job and return failed result" when {
      "the update was not acknowledged" in new Setup {
        when(mockPresubmissionMongo.getDocumentIdsWithoutCreatedAtField(any()))
          .thenReturn(Future.successful(Seq(ObjectId.get())))
        when(mockPresubmissionMongo.addCreatedAtField(any()))
          .thenReturn(Future.successful(Left(UpdateRequestNotAcknowledged)))

        val (modifiedDocuments, updateMessage) = result(documentUpdateService.updateMissingCreatedAtFields(), Duration.Inf)

        modifiedDocuments shouldBe 0
        updateMessage shouldBe UpdateRequestNotAcknowledged

        verify(mockPresubmissionMongo, times(1)).getDocumentIdsWithoutCreatedAtField(any[Int])
        verify(mockPresubmissionMongo, times(1)).addCreatedAtField(any[Seq[ObjectId]])
      }
    }
  }
}
