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
import _root_.play.api.inject.bind
import _root_.play.api.inject.guice.GuiceApplicationBuilder
import _root_.play.api.libs.json.{JsObject, Json}
import _root_.play.api.libs.ws.WSClient
import _root_.play.api.test.Helpers._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import repositories.PresubmissionMongoRepository
import scheduler.UpdateCreatedAtFieldsJobImpl
import services.DocumentUpdateService

import java.util.concurrent.TimeUnit
import scala.collection.mutable.ListBuffer

class PresubmissionMongoRepositorySpec extends AnyWordSpecLike with Matchers with BeforeAndAfterEach {

  lazy val app: Application = new GuiceApplicationBuilder().configure(
    Map(
      "microservice.services.auth.port" -> "18500",
      "settings.presubmission-collection-ttl-days" -> 365)
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
      val schemeData = Fixtures.schemeData
      val schemeInfo = schemeData.schemeInfo
      await(presubmissionRepository.storeJson(schemeData)) shouldBe  true
      await(presubmissionRepository.count(schemeInfo)) shouldBe 1
    }
  }

  "storeJson2" should {
    "successfully insert the scheme data" in {
      val schemeData = Fixtures.schemeData
      val schemeInfo = schemeData.schemeInfo
      await(presubmissionRepository.storeJsonV2(schemeInfo.toString, schemeData)) shouldBe true
      await(presubmissionRepository.count(schemeInfo)) shouldBe 1
    }
  }

  "getJson" should {
    "successfully return the scheme data" in {
      val schemeData = Fixtures.schemeData
      val schemeInfo = schemeData.schemeInfo
      await(presubmissionRepository.storeJson(schemeData))

      val result = await(presubmissionRepository.getJson(schemeInfo)).head

      (result \ "schemeInfo").as[JsObject] shouldBe Json.toJsObject(schemeInfo)
      (result \ "sheetName").as[String] shouldBe schemeData.sheetName
      (result \ "numberOfParts").asOpt[Int] shouldBe schemeData.numberOfParts
      (result \ "data").asOpt[ListBuffer[Seq[String]]] shouldBe schemeData.data
    }
  }

  "count" should {
    "successfully return number of documents for given scheme info" in {
      val schemeData = Fixtures.schemeData
      val schemeInfo = schemeData.schemeInfo
      await(presubmissionRepository.storeJson(schemeData))
      await(presubmissionRepository.count(schemeInfo)) shouldBe 1
    }
  }

  "removeJson" should {
    "successfully remove the documents for given scheme info" in {
      val schemeData = Fixtures.schemeData
      val schemeInfo = schemeData.schemeInfo
      await(presubmissionRepository.storeJson(schemeData))
      await(presubmissionRepository.count(schemeInfo)) shouldBe 1
      await(presubmissionRepository.removeJson(schemeInfo)) shouldBe true
      await(presubmissionRepository.count(schemeInfo)) shouldBe 0
    }
  }

  "getDocumentIdsWithoutCreatedAtField" should {
    "return list of document ids without createdAt field" in {
      val schemeData = Fixtures.schemeData
      await(presubmissionRepository.storeJson(schemeData)) //document with createdAt

      val testDocumentId = await(
        presubmissionRepository.collection.insertOne(Json.toJsObject(schemeData)).toFuture()
      ).getInsertedId.asObjectId().getValue //document without createdAt

      await(presubmissionRepository.getDocumentIdsWithoutCreatedAtField(2)).head shouldBe testDocumentId
    }

    "return empty list of document ids if createdAt exists for every record" in {
      val schemeData = Fixtures.schemeData
      await(presubmissionRepository.storeJson(schemeData)) //document with createdAt
      await(presubmissionRepository.storeJson(schemeData)) //document with createdAt

      await(presubmissionRepository.getDocumentIdsWithoutCreatedAtField(2)) shouldBe Seq.empty
    }
  }

  "addCreatedAtField" should {
    "add createdAt field to documents without this field" in {
      val schemeData = Fixtures.schemeData
      await(presubmissionRepository.storeJson(schemeData)) //document with createdAt

      val testDocumentId = await(
        presubmissionRepository.collection.insertOne(Json.toJsObject(schemeData)).toFuture()
      ).getInsertedId.asObjectId().getValue //document without createdAt

      await(presubmissionRepository.addCreatedAtField(Seq(testDocumentId))) shouldBe 1
    }
  }
}
