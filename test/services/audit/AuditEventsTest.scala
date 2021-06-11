/*
 * Copyright 2021 HM Revenue & Customs
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
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify}
import org.mockito.internal.verification.VerificationModeFactory
import org.scalatest.BeforeAndAfterEach
import play.api.test.FakeRequest
import services.audit.{AuditEvents, AuditService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

class AuditEventsTest
  extends ERSTestHelper with BeforeAndAfterEach {

  implicit val request = FakeRequest()
  implicit var hc = new HeaderCarrier()
  val dateTime = new DateTime()
  val rsc = new ErsMetaData(new SchemeInfo("",new DateTime(),"","","",""),"",Some(""),"",Some(""),Some(""))
  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockAuditService: AuditService = mock[AuditService]
  val testAuditEvents = new AuditEvents(mockAuditService)

  override protected def beforeEach(): Unit = {
    reset(mockAuditService)
    super.beforeEach()
  }


  "its should audit runtime errors" in {
    testAuditEvents.auditRunTimeError(new RuntimeException, "some context")

    verify(mockAuditService, VerificationModeFactory.times(1))
      .sendEvent(any(), any())(any(), any())
  }

  "public to protected audit event" in {

    val result: Boolean = testAuditEvents.publicToProtectedEvent(
      schemeInfo = schemeData.schemeInfo,
      sheetName = schemeData.sheetName,
      numRows = schemeData.data.getOrElse(Seq()).length.toString)

    result shouldBe true

    verify(mockAuditService, VerificationModeFactory.times(1))
      .sendEvent(any(), any())(any(), any())
  }

  "send To Adr Event audit event" in {

   val result: Boolean = testAuditEvents.sendToAdrEvent("send To Adr Event", Fixtures.EMISummaryDate)

    result shouldBe true
    verify(mockAuditService, VerificationModeFactory.times(1))
      .sendEvent(any(), any())(any(), any())
  }

  "send resubmissionResult Event audit event" in {
    val result: Boolean = testAuditEvents.resubmissionResult(Fixtures.schemeInfo, res = true)

    result shouldBe true
    verify(mockAuditService, VerificationModeFactory.times(1))
      .sendEvent(any(), any())(any(), any())
  }

  "eventMap should return the correct map when no additional maps are added" in {

    val timestamp: DateTime = new DateTime()
      .withDate(2015,12,5).withTime(12,50,55,0).withZone(DateTimeZone.UTC)

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
