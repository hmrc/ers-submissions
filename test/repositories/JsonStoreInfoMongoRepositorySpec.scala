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
import models.{ErsJsonStoreInfo, PostSubmissionData}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.JsObject
import play.api.test.FakeApplication
import play.api.test.Helpers._
import reactivemongo.api.DB
import reactivemongo.api.commands._
import reactivemongo.json.collection.JSONCollection
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class JsonStoreInfoMongoRepositorySpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  "calling createErsJsonStoreInfo" should {

    def buildPostsubmissionRepository(storeJsonResult: Option[Boolean] = None): JsonStoreInfoMongoRepository = new JsonStoreInfoMongoRepository()(() => mock[DB]) {
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
      val postsubmissionMongoRepository = buildPostsubmissionRepository(Some(true))
      val result = await(postsubmissionMongoRepository.createErsJsonStoreInfo(Fixtures.ersJsonStoreInfo))
      result shouldBe true
    }

    "return false if storage is successful" in {
      val postsubmissionMongoRepository = buildPostsubmissionRepository(Some(false))
      val result = await(postsubmissionMongoRepository.createErsJsonStoreInfo(Fixtures.ersJsonStoreInfo))
      result shouldBe false
    }

    "rethrows exception if exception occurs" in {
      val postsubmissionMongoRepository = buildPostsubmissionRepository(None)
      intercept[Exception] {
        await(postsubmissionMongoRepository.createErsJsonStoreInfo(Fixtures.ersJsonStoreInfo))
      }
    }

  }

  "calling updateStatus" should {

    def buildPostsubmissionRepository(updateResult: Option[Boolean] = None): JsonStoreInfoMongoRepository = new JsonStoreInfoMongoRepository()(() => mock[DB]) {
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
      val postsubmissionMongoRepository = buildPostsubmissionRepository(Some(true))
      val result = await(postsubmissionMongoRepository.updateStatus("sent", Fixtures.EMISchemeInfo))
      result shouldBe true
    }

    "return false if update fails" in {
      val postsubmissionMongoRepository = buildPostsubmissionRepository(Some(false))
      val result = await(postsubmissionMongoRepository.updateStatus("sent", Fixtures.EMISchemeInfo))
      result shouldBe false
    }

    "throws exception if exception occurs" in {
      val postsubmissionMongoRepository = buildPostsubmissionRepository(None)
      intercept[Exception] {
        await(postsubmissionMongoRepository.updateStatus("sent", Fixtures.EMISchemeInfo))
      }
    }

  }

}
