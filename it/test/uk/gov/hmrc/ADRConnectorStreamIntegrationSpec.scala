/*
 * Copyright 2025 HM Revenue & Customs
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
import _root_.play.api.test.Helpers._
import connectors.ADRConnector
import models.SchemeInfo
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import repositories.{MetadataMongoRepository, PresubmissionMongoRepository}
import services.PresubmissionService
import uk.gov.hmrc.Fixtures.buildErsSummary
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext

class ADRConnectorStreamIntegrationSpec
  extends AnyWordSpecLike
    with Matchers
    with EitherValues
    with BeforeAndAfterEach
    with FakeErsStubService {

  val app: Application = new GuiceApplicationBuilder().configure(
    Map("microservice.services.ers-stub.port" -> stubServicePort,
      "auditing.enabled" -> false
    )
  ).build()

  implicit lazy val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  private lazy val presubmissionRepository = app.injector.instanceOf[PresubmissionMongoRepository]
  private lazy val metadataMongoRepository = app.injector.instanceOf[MetadataMongoRepository]
  private lazy val presubmissionService = app.injector.instanceOf[PresubmissionService]
  private lazy val adrConnector = app.injector.instanceOf[ADRConnector]

  private val timestamp: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)
  private val streamSession = "session"
  private val schemeType = "CSOP"
  private val expectedPath = s"/employment-related-securities/etmpFullSubmit/${schemeType.toLowerCase}"

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    await(
      presubmissionRepository
        .storeJson(Fixtures.schemeData(Some(timestamp)), streamSession)
        .value
    )
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    await(presubmissionRepository.collection.drop().toFuture())
    await(metadataMongoRepository.collection.drop().toFuture())
  }

  "ADRConnector.sendDataStream" should {
    "POST a streamed JSON array and return ACCEPTED" in {
      val ersSummary = buildErsSummary(timestamp = timestamp, schemaType = schemeType)
      val schemeInfo: SchemeInfo = ersSummary.metaData.schemeInfo

      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(streamSession)))

      val byteStream = await(presubmissionService.getJsonByteStringStream(schemeInfo).value).value

      val resp = await(adrConnector.sendDataStream(byteStream, schemeType).value).value
      resp.status shouldBe ACCEPTED

      verifyPostedTo(expectedPath)
    }
  }
}
