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

import fixtures.Fixtures
import fixtures.Fixtures.schemeData
import helpers.ERSTestHelper
import models.{ErsMetaData, SchemeInfo}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.mockito.internal.verification.VerificationModeFactory
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import services.audit.{AuditEvents, AuditService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Disabled, Failure, Success}

import java.time.{ZoneId, ZonedDateTime}
import scala.concurrent.Future

class AuditEventsTest
  extends ERSTestHelper with BeforeAndAfterEach {

  implicit val request: FakeRequest[AnyContent] = FakeRequest()
  implicit var hc: HeaderCarrier = new HeaderCarrier()
  val rsc: ErsMetaData = ErsMetaData(SchemeInfo(schemeRef = "", schemeId = "", taxYear = "", schemeName = "", schemeType = ""),"",Some(""),"",Some(""),Some(""))
  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockAuditService: AuditService = mock[AuditService]
  val testAuditEvents = new AuditEvents(mockAuditService)

  override protected def beforeEach(): Unit = {
    reset(mockAuditService)
    super.beforeEach()
  }

for (eventResult <- Seq(Success, Failure("failed"), Disabled)) {
  s"it should audit runtime errors with result $eventResult" in {
    when(mockAuditService.sendEvent(any(), any())(any())).thenReturn(Future.successful(eventResult))

    val result = await(testAuditEvents.auditRunTimeError(new RuntimeException, "some context"))

    result shouldBe eventResult

    verify(mockAuditService, VerificationModeFactory.times(1))
      .sendEvent(any(), any())(any())
  }

  s"it should audit generic error with result $eventResult" in {
    when(mockAuditService.sendEvent(any(), any())(any())).thenReturn(Future.successful(eventResult))

    val result = await(testAuditEvents.auditError("someContext", "someMessage"))

    result shouldBe eventResult

    verify(mockAuditService, VerificationModeFactory.times(1))
      .sendEvent(any(), any())(any())
  }

  s"public to protected audit event with result $eventResult" in {
    when(mockAuditService.sendEvent(any(), any())(any())).thenReturn(Future.successful(eventResult))

    val result = await(testAuditEvents.publicToProtectedEvent(
      schemeInfo = schemeData.schemeInfo,
      sheetName = schemeData.sheetName,
      numRows = schemeData.data.getOrElse(Seq()).length.toString))

    result shouldBe eventResult

    verify(mockAuditService, VerificationModeFactory.times(1))
      .sendEvent(any(), any())(any())
  }

  s"send To Adr Event audit event with result $eventResult" in {
    when(mockAuditService.sendEvent(any(), any())(any())).thenReturn(Future.successful(eventResult))

    val result = await(testAuditEvents.sendToAdrEvent("send To Adr Event", Fixtures.EMISummaryDate))

    result shouldBe eventResult

    verify(mockAuditService, VerificationModeFactory.times(1))
      .sendEvent(any(), any())(any())
  }

  s"send resubmissionResult Event audit event with result $eventResult" in {
    when(mockAuditService.sendEvent(any(), any())(any())).thenReturn(Future.successful(eventResult))

    val result = await(testAuditEvents.resubmissionResult(Fixtures.schemeInfo, res = true))

    result shouldBe eventResult

    verify(mockAuditService, VerificationModeFactory.times(1))
      .sendEvent(any(), any())(any())
  }
}

  "eventMap should return the correct map when no additional maps are added" in {
    val timestamp = ZonedDateTime.of(2015,12,5,12,50,55,0, ZoneId.of("UTC")).toInstant

    val testSchemeInfo: Map[String, String] = Map(
      "schemeRef" -> "XA1100000000000",
      "schemeId" -> "123PA12345678",
      "schemeType" -> "EMI",
      "schemeName" -> "My scheme",
      "timestamp" -> timestamp.toString,
      "taxYear" -> "2014/15"
    )

  val result: Map[String, String] = testAuditEvents.eventMap(Fixtures.schemeInfo)

    result shouldBe testSchemeInfo
  }

}
