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

package services.audit

import javax.inject.Inject
import org.joda.time.DateTime
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.ExecutionContext.Implicits.global

class AuditService @Inject()(auditConnector: AuditConnector) {
  val auditSource = "ers-submissions"

  def sendEvent(transactionName : String, details: Map[String, String])(implicit request: Request[_], hc: HeaderCarrier): Unit =
    auditConnector.sendEvent(buildEvent(transactionName, details))

  def buildEvent(transactionName: String,  details: Map[String, String])(implicit request: Request[_], hc: HeaderCarrier): DataEvent =
    DataEvent(
      auditSource = auditSource,
      auditType = transactionName,
      tags = generateTags(hc),
      detail = details
    )

  def generateTags(hc: HeaderCarrier): Map[String, String] =
    hc.headers.toMap ++
      hc.headers.toMap ++
      Map("dateTime" ->  getDateTime.toString)

  def getDateTime: DateTime = new DateTime

}
