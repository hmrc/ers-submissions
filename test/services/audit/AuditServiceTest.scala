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

import helpers.ERSTestHelper
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.verify
import org.mockito.internal.verification.VerificationModeFactory
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import services.audit.AuditService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.ExecutionContext

class AuditServiceTest extends ERSTestHelper {

  implicit val request: FakeRequest[AnyContent] = FakeRequest()
  implicit val hc: HeaderCarrier = new HeaderCarrier

  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val auditTest: AuditService = new AuditService(mockAuditConnector) {
    override def generateTags(hc: HeaderCarrier): Map[String, String] = Map("tags" -> "someTags")
  }

  "auditer should send message" in {
    auditTest.sendEvent("source",  Map("details1" -> "randomDetail"))

    verify(mockAuditConnector, VerificationModeFactory.times(1))
      .sendEvent(any())(any(), any())
  }

  "buildEvent should build a data event" in {
    val testDataEvent = DataEvent(
      auditSource = "ers-submissions",
      auditType = "source",
      detail = Map("details1" -> "randomDetail"),
      tags = Map("tags" -> "someTags")
    )

    val result = auditTest.buildEvent("source", Map("details1" -> "randomDetail"))

    result.auditSource shouldBe testDataEvent.auditSource
    result.auditType   shouldBe testDataEvent.auditType
    result.detail      shouldBe testDataEvent.detail
    result.tags        shouldBe testDataEvent.tags
  }
}
