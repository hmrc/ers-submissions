/*
 * Copyright 2016 HM Revenue & Customs
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

package repositories

import fixtures.Fixtures
import models.{ErsSummary, PostSubmissionData}
import play.api.libs.json.{JsObject, Reads}
import reactivemongo.api.Cursor
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import reactivemongo.api.DB
import reactivemongo.api.collections.GenericQueryBuilder
import reactivemongo.api.commands.{UpdateWriteResult, WriteError, DefaultWriteResult, WriteResult}
import reactivemongo.bson._
import reactivemongo.json.JSONSerializationPack.Reader
import reactivemongo.json.collection.JSONCollection
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}
import scala.collection.generic.CanBuildFrom
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import org.mockito.Matchers._

class MetadataMongoRepositorySpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  "calling storeJson" should {

    def buildMetadataMongoRepository(storeJsonResult: Option[Boolean] = None): MetadataMongoRepository = new MetadataMongoRepository()(() => mock[DB]) {
      val mockCollection = mock[JSONCollection]

      val writeRes: Option[WriteResult] = storeJsonResult match {
        case Some (true) => Some(new DefaultWriteResult (storeJsonResult.getOrElse (true), 200, Seq (), None, None, None))
        case Some (false) => Some(new DefaultWriteResult (storeJsonResult.getOrElse (false), 400, Seq (new WriteError (1, 400, "Error message") ), None, None, Some ("Error message") ))
        case _ => None
      }

      when(mockCollection.insert(any[PostSubmissionData], any())(any(), any())).thenReturn(Future(writeRes.getOrElse(throw new Exception)))

      override lazy val collection = mockCollection
    }

    "return true if storage is successful" in {
      val metadataMongoRepository = buildMetadataMongoRepository(Some(true))
      val result = await(metadataMongoRepository.storeErsSummary(Fixtures.metadata))
      result shouldBe true
    }

    "return false if storage is successful" in {
      val metadataMongoRepository = buildMetadataMongoRepository(Some(false))
      val result = await(metadataMongoRepository.storeErsSummary(Fixtures.metadata))
      result shouldBe false
    }

    "rethrow exception if exception occurs" in {
      val metadataMongoRepository = buildMetadataMongoRepository(None)
      intercept[Exception] {
        await(metadataMongoRepository.storeErsSummary(Fixtures.metadata))
      }
    }

  }

  "calling buildSelector" should {

    def buildMetadataMongoRepository(storeJsonResult: Option[Boolean] = None): MetadataMongoRepository = new MetadataMongoRepository()(() => mock[DB]) {
      val mockCollection = mock[JSONCollection]

      val writeRes: Option[WriteResult] = storeJsonResult match {
        case Some (true) => Some(new DefaultWriteResult (storeJsonResult.getOrElse (true), 200, Seq (), None, None, None))
        case Some (false) => Some(new DefaultWriteResult (storeJsonResult.getOrElse (false), 400, Seq (new WriteError (1, 400, "Error message") ), None, None, Some ("Error message") ))
        case _ => None
      }

      when(mockCollection.insert(any[PostSubmissionData], any())(any(), any())).thenReturn(Future(writeRes.getOrElse(throw new Exception)))

      override lazy val collection = mockCollection
    }

    "return bson document with timestamp and schemeRef" in {
      val result = buildMetadataMongoRepository(Some(true)).buildSelector(Fixtures.schemeInfo)
      result.get("metaData.schemeInfo.schemeRef").get shouldBe BSONString("XA1100000000000")
      result.get("metaData.schemeInfo.timestamp").get shouldBe BSONLong(1449319855000L)
    }
  }

  "calling getErsSummary" should {

    val mockCollection = mock[JSONCollection]
    val genericQueryBuilder = mock[GenericQueryBuilder[mockCollection.pack.type]]

    val buildMetadataMongoRepository: MetadataMongoRepository = new MetadataMongoRepository()(() => mock[DB]) {
      override lazy val collection = mockCollection
    }

    "return related to given SchemeInfo ErsSummary" in {
      reset(mockCollection)
      reset(genericQueryBuilder)
      when(
        mockCollection.find(any())(any())
      ).thenReturn(
        genericQueryBuilder
      )
      when(
        genericQueryBuilder.one[ErsSummary](any, any)
      ).thenReturn(
        Future.successful(Some(Fixtures.EMISummaryDate))
      )

      val result = await(buildMetadataMongoRepository.getErsSummary(Fixtures.schemeInfo))
      result.get shouldBe Fixtures.EMISummaryDate
    }
  }

  "calling updateStatus" should {

    def buildMetadataMongoRepository(updateResult: Option[Boolean] = None): MetadataMongoRepository = new MetadataMongoRepository()(() => mock[DB]) {
      val mockCollection = mock[JSONCollection]

      val writeRes: Option[UpdateWriteResult] = updateResult match {
        case Some (true) => Some(new UpdateWriteResult (updateResult.getOrElse (true), 200, 1, Seq (), Seq (), None, None, None))
        case Some (false) => Some(new UpdateWriteResult (updateResult.getOrElse (false), 400, 0, Seq (), Seq(new WriteError (1, 400, "Error message") ), None, None, Some ("Error message") ))
        case _ => None
      }

      when(
        mockCollection.update(any[JsObject], any[JsObject](), any(), any(), any())(any(), any(), any())
      ).thenReturn(
        Future(writeRes.getOrElse(throw new Exception))
      )

      override lazy val collection = mockCollection
    }

    "return true if update is successful" in {
      val result = await(buildMetadataMongoRepository(Some(true)).updateStatus(Fixtures.EMISchemeInfo, "sent"))
      result shouldBe true
    }

    "return false if update fails" in {
      val result = await(buildMetadataMongoRepository(Some(false)).updateStatus(Fixtures.EMISchemeInfo, "sent"))
      result shouldBe false
    }

    "throws exception if exception occurs" in {
      intercept[Exception] {
        await(buildMetadataMongoRepository(None).updateStatus(Fixtures.EMISchemeInfo, "sent"))
      }
    }

  }

}
