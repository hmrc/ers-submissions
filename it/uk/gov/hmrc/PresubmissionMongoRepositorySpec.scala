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

import _root_.play.api.Application
import _root_.play.api.inject.guice.GuiceApplicationBuilder
import _root_.play.api.libs.json.{JsObject, Json}
import _root_.play.api.libs.ws.WSClient
import _root_.play.api.test.Helpers._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import repositories.PresubmissionMongoRepository
import uk.gov.hmrc.play.http.ws.WSRequest

import java.util.concurrent.TimeUnit
import scala.collection.mutable.ListBuffer

class PresubmissionMongoRepositorySpec extends AnyWordSpecLike with Matchers
  with BeforeAndAfterEach with WSRequest with FakeAuthService {

  lazy val app: Application = new GuiceApplicationBuilder().configure(
    Map(
      "microservice.services.auth.port" -> "18500",
    "settings.presubmission-collection-ttl" -> 365)
  ).build()

  def wsClient: WSClient = app.injector.instanceOf[WSClient]

  private lazy val presubmissionRepository = app.injector.instanceOf[PresubmissionMongoRepository]

  override protected def afterEach: Unit = {
    super.afterEach
    await(presubmissionRepository.collection.drop.toFuture)
  }

  "presubmissionRepository" should {
    "have TTL index on createdAt field" in {
      presubmissionRepository.indexes.last.getOptions.getExpireAfter(TimeUnit.DAYS) shouldBe 365
    }
  }

  "storeJson" should {
    "successfully insert the scheme data" in {
      await(presubmissionRepository.storeJson(Fixtures.schemeData)) shouldBe  true
      await(presubmissionRepository.count(Fixtures.schemeInfo)) shouldBe 1
    }
  }

  "storeJson2" should {
    "successfully insert the scheme data" in {
      await(presubmissionRepository.storeJsonV2(Fixtures.schemeInfo.toString, Fixtures.schemeData)) shouldBe true
      await(presubmissionRepository.count(Fixtures.schemeInfo)) shouldBe 1
    }
  }

  "getJson" should {
    "successfully return the scheme data" in {
      await(presubmissionRepository.storeJson(Fixtures.schemeData))

      val result = await(presubmissionRepository.getJson(Fixtures.schemeInfo)).head

      (result \ "schemeInfo").as[JsObject] shouldBe Json.toJsObject(Fixtures.schemeInfo)
      (result \ "sheetName").as[String] shouldBe Fixtures.schemeData.sheetName
      (result \ "numberOfParts").asOpt[Int] shouldBe Fixtures.schemeData.numberOfParts
      (result \ "data").asOpt[ListBuffer[Seq[String]]] shouldBe Fixtures.schemeData.data
    }
  }

  "count" should {
    "successfully return number of documents for given scheme info" in {
      await(presubmissionRepository.storeJson(Fixtures.schemeData))
      await(presubmissionRepository.count(Fixtures.schemeData.schemeInfo)) shouldBe 1
    }
  }

  "removeJson" should {
    "successfully remove the documents for given scheme info" in {
      await(presubmissionRepository.storeJson(Fixtures.schemeData))
      await(presubmissionRepository.count(Fixtures.schemeData.schemeInfo)) shouldBe 1
      await(presubmissionRepository.removeJson(Fixtures.schemeData.schemeInfo)) shouldBe true
      await(presubmissionRepository.count(Fixtures.schemeData.schemeInfo)) shouldBe 0
    }
  }
}