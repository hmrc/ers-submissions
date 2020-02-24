/*
 * Copyright 2020 HM Revenue & Customs
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
import models.ERSQuery
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import reactivemongo.api.DB
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class DataVerificationRepositorySpec extends UnitSpec with MockitoSugar {
  val presubmissionJson = Fixtures.schemeDataJson

  val sayeERSQuery: ERSQuery = ERSQuery (
    schemeType = Some("SAYE"),
    startDate = None,
    endDate = None,
    transferStatus = None,
    schemeRefsList = List()
  )

  def buildMongoRepository(countResult: Option[Int] = None): DataVerificationMongoRepository = new DataVerificationMongoRepository()(() => mock[DB]) {
    val mockCollection = mock[JSONCollection]
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

  /* commented out by Andrew Dowell on 23.01.18 - these are not valid unit test as they don't test the logic
  but calls to a DB. Update to Mongo means that Mockito class is final and can't therefore mock. These tests add
  value testing as part of integration and not as unit.

  "Calling getCountBySchemeTypeWithInDateRange" should {

    "Check numberOfRecords = 1" in {
      running(FakeApplication()) {
        val numberOfRecords: Int = 1
        val dataVerificationRepository = buildMongoRepository(countResult = Some(numberOfRecords))
        val result = await(dataVerificationRepository.getCountBySchemeTypeWithInDateRange(sayeERSQuery))
        result shouldBe numberOfRecords
      }
    }

    "Check numberOfRecords = 3" in {
      running(FakeApplication()) {
        val numberOfRecords: Int = 3
        val dataVerificationRepository = buildMongoRepository(countResult = Some(numberOfRecords))
        val result = await(dataVerificationRepository.getCountBySchemeTypeWithInDateRange(sayeERSQuery))
        result shouldBe numberOfRecords
      }
    }
  }
  */

}
