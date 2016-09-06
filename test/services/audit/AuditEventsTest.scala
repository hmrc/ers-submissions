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

package services

import fixtures.Fixtures
import models.{ErsMetaData, SchemeInfo}
import org.apache.commons.lang3.exception.ExceptionUtils
import org.joda.time.DateTime
import org.scalatest.{Matchers, WordSpec}
import play.api.test.FakeRequest
import services.audit.{AuditEvents, AuditService, AuditServiceConnector}
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.http.HeaderCarrier
import scala.collection.mutable.ListBuffer

class AuditEventsTest extends WordSpec with Matchers {

  implicit val request = FakeRequest()
  implicit var hc = new HeaderCarrier()
  val dateTime = new DateTime()
  val rsc = new ErsMetaData(new SchemeInfo("",new DateTime(),"","","",""),"",Some(""),"",Some(""),Some(""))


  trait ObservableAuditConnector extends AuditServiceConnector {
    val events: ListBuffer[DataEvent] = new ListBuffer[DataEvent]

    def observedEvents: ListBuffer[DataEvent] = events

    def addEvent(dataEvent: DataEvent): Unit = {
      events += dataEvent
    }

    override def auditData(dataEvent: DataEvent)(implicit hc: HeaderCarrier): Unit = {
      addEvent(dataEvent)
    }
  }

  def createObservableAuditConnector = new ObservableAuditConnector {}

  def createAuditor(observableAuditConnector: ObservableAuditConnector) = {

    val testAuditService = new AuditService {
      override def auditConnector = observableAuditConnector
    }

    new AuditEvents {
      override def auditService: AuditService = testAuditService
    }
  }

  "its should audit runtime errors" in {
    val observableAuditConnector = createObservableAuditConnector
    val auditor = createAuditor(observableAuditConnector)

    var runtimeException : Throwable = null

    try {
      var divideByZero : Int = 0/0
    } catch {
      case e: Throwable => {
        runtimeException = e
        auditor.auditRunTimeError(e, "some context info")
      }
    }

    observableAuditConnector.events.length should equal(1)

    val event = observableAuditConnector.events.head

    event.auditType should equal("ERSRunTimeError")

    event.detail("ErrorMessage") should equal(runtimeException.getMessage)
    event.detail("StackTrace") should equal(ExceptionUtils.getStackTrace(runtimeException))
    event.detail("Context") should equal("some context info")
  }

  "public to protected audit event" in {
    val observableAuditConnector = createObservableAuditConnector
    val auditor = createAuditor(observableAuditConnector)


    auditor.publicToProtectedEvent(Fixtures.schemeData.schemeInfo, Fixtures.schemeData.sheetName, Fixtures.schemeData.data.getOrElse(Seq()).length.toString)
    observableAuditConnector.events.length should equal(1)

    val event = observableAuditConnector.events.head

    event.auditType should equal("ErsFileTransfer")
  }

  "send To Adr Event audit event" in {
    val observableAuditConnector = createObservableAuditConnector
    val auditor = createAuditor(observableAuditConnector)


    auditor.sendToAdrEvent("send To Adr Event", Fixtures.EMISummaryDate)
    observableAuditConnector.events.length should equal(1)

    val event = observableAuditConnector.events.head

    event.auditType should equal("send To Adr Event")
  }

}
