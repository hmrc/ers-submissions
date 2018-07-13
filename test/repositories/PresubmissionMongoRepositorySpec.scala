/*
 * Copyright 2018 HM Revenue & Customs
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
import models.SchemeData
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeApplication
import play.api.test.Helpers._
import reactivemongo.api.DB
import reactivemongo.api.commands.{WriteError, DefaultWriteResult, WriteResult}
import reactivemongo.json.collection.JSONCollection
import uk.gov.hmrc.play.test.{UnitSpec}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PresubmissionMongoRepositorySpec extends UnitSpec with MockitoSugar {

  val presubmissionJson = Fixtures.schemeDataJson

  def buildMongoRepository(storeJsonResult: Option[Boolean] = None, removeResult: Option[Boolean] = None, getJsonResult: Option[Boolean] = None, countResult: Option[Int] = None): PresubmissionMongoRepository = new PresubmissionMongoRepository()(() => mock[DB]) {
    val mockCollection = mock[JSONCollection]

    val writeRes: Option[WriteResult] = storeJsonResult match {
      case Some (true) => Some(new DefaultWriteResult (storeJsonResult.getOrElse (true), 200, Seq (), None, None, None))
      case Some (false) => Some(new DefaultWriteResult (storeJsonResult.getOrElse (false), 400, Seq (new WriteError (1, 400, "Error message") ), None, None, Some ("Error message") ))
      case _ => None
    }

    when(mockCollection.insert(any[SchemeData], any())(any(), any())).thenReturn(Future(writeRes.getOrElse(throw new Exception)))

    val writeResRemove: Option[WriteResult] = removeResult match {
      case Some (true) => Some(new DefaultWriteResult (removeResult.getOrElse (true), 200, Seq (), None, None, None))
      case Some (false) => Some(new DefaultWriteResult (removeResult.getOrElse (false), 400, Seq (new WriteError (1, 400, "Error message") ), None, None, Some ("Error message") ))
      case _ => None
    }

    when(mockCollection.remove(any(), any(), any())(any(), any())).thenReturn(Future(writeResRemove.getOrElse(throw new Exception)))

    when(
      mockCollection.count(any(), anyInt(), anyInt(), any())(any(), any())
    ).thenReturn(
      countResult match {
        case Some(num) => Future.successful(num)
        case _ => Future.failed(new RuntimeException)
      }
    )

    override lazy val collection = mockCollection

  }

  "calling storeJson" should {

    "return true if storage is successful" in {
      val presubmissionMongoRepository = buildMongoRepository(Some(true))
      val result = await(presubmissionMongoRepository.storeJson(Fixtures.schemeData))
      result shouldBe true
    }

    "return false if storage is successful" in {
      val presubmissionMongoRepository = buildMongoRepository(Some(false))
      val result = await(presubmissionMongoRepository.storeJson(Fixtures.schemeData))
      result shouldBe false
    }

  }

  "calling removeJson" should {

    "return true if storage is successful" in {
      val presubmissionMongoRepository = buildMongoRepository(None, Some(true))
      val result = await(presubmissionMongoRepository.removeJson(Fixtures.EMISchemeInfo))
      result shouldBe true
    }

    "return false if storage is successful" in {
      val presubmissionMongoRepository = buildMongoRepository(None, Some(false))
      val result = await(presubmissionMongoRepository.removeJson(Fixtures.EMISchemeInfo))
      result shouldBe false
    }
  }

  "calling count" should {

    "return number of found documents" in {
      running(FakeApplication()) {
        val numberOfSheets: Int = 1
        val presubmissionMongoRepository = buildMongoRepository(countResult = Some(numberOfSheets))
        val result = await(presubmissionMongoRepository.count(Fixtures.EMISchemeInfo))
        result shouldBe numberOfSheets
      }
    }

  }
}
