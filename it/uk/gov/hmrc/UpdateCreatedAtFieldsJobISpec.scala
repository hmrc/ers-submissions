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
import _root_.play.api.test.Helpers._
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import models.SchemeData
import org.mongodb.scala.model.Filters
import org.mongodb.scala.result.InsertOneResult
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import repositories.PresubmissionMongoRepository
import scheduler.{ScheduledJob, UpdateCreatedAtFieldsJob}

import scala.concurrent.ExecutionContext.Implicits.global

class UpdateCreatedAtFieldsJobISpec extends AnyWordSpecLike
  with Matchers
  with GuiceOneServerPerSuite
  with WiremockHelper
  with BeforeAndAfterEach
  with BeforeAndAfterAll {

  override def beforeEach(): Unit = {
    resetWiremock()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure()
    .build()

  class Setup {
    val repository: PresubmissionMongoRepository = app.injector.instanceOf[PresubmissionMongoRepository]
    await(repository.collection.drop().toFuture())
    await(repository.ensureIndexes)
    await(repository.collection.countDocuments(Filters.empty()).toFuture()) shouldBe 0

    def insert(schemeData: SchemeData): Boolean = await(repository.storeJson(schemeData))

    def insertAsJson(schemeData: SchemeData): InsertOneResult = await(
      repository.collection.insertOne(Json.toJsObject(schemeData)).toFuture())

    def count: Long = await(repository.collection.countDocuments(Filters.empty).toFuture())

    def getDocsWithCreatedAtField: Seq[JsObject] = await(repository.collection.find(Filters.and(Filters.exists("createdAt"), Filters.notEqual("createdAt", ""))).toFuture())
  }

  def getJob: ScheduledJob = app.injector.instanceOf[UpdateCreatedAtFieldsJob]

  "UpdateCreatedAtFieldsJob" should {
    "update the documents where the createdAt is missing" in new Setup {
      insert(Fixtures.schemeData) //with createdAt
      insertAsJson(Fixtures.schemeData) //without createdAt
      insertAsJson(Fixtures.schemeData) //without createdAt

      count shouldBe 3
      getDocsWithCreatedAtField.size shouldBe 1

      val updatedCount: Long = await(getJob.scheduledMessage.service.invoke.map(_.asInstanceOf[Long]))

      updatedCount shouldBe 2
      getDocsWithCreatedAtField.size shouldBe 3
    }
  }

  "return updated count 0 if all documents have createdAt field" in new Setup {
    insert(Fixtures.schemeData) //with createdAt
    insert(Fixtures.schemeData) //with createdAt
    insert(Fixtures.schemeData) //with createdAt

    count shouldBe 3
    getDocsWithCreatedAtField.size shouldBe 3

    val updatedCount: Long = await(getJob.scheduledMessage.service.invoke.map(_.asInstanceOf[Long]))

    updatedCount shouldBe 0
  }
}

trait WiremockHelper {
  val wiremockPort = 11111
  val wiremockHost = "localhost"
  val url = s"http://$wiremockHost:$wiremockPort"

  lazy val wmConfig: WireMockConfiguration = wireMockConfig().port(wiremockPort)
  lazy val wireMockServer = new WireMockServer(wmConfig)

  def startWiremock(): Unit = {
    wireMockServer.start()
    WireMock.configureFor(wiremockHost, wiremockPort)
  }

  def stopWiremock(): Unit = wireMockServer.stop()

  def resetWiremock(): Unit = WireMock.reset()
}
