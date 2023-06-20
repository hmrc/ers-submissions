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

package services.audit

import org.joda.time.DateTime
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.DataEvent

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuditService @Inject()(auditConnector: AuditConnector)(implicit ec: ExecutionContext) {
  val auditSource = "ers-submissions"

  def sendEvent(transactionName : String, details: Map[String, String])(implicit hc: HeaderCarrier): Future[AuditResult] =
    auditConnector.sendEvent(buildEvent(transactionName, details))

  def buildEvent(transactionName: String,  details: Map[String, String])(implicit hc: HeaderCarrier): DataEvent =
    DataEvent(
      auditSource = auditSource,
      auditType = transactionName,
      tags = generateTags(hc),
      detail = details
    )

  def generateTags(hc: HeaderCarrier): Map[String, String] =
    hc.headers(HeaderNames.explicitlyIncludedHeaders).toMap ++
      Map("dateTime" ->  getDateTime.toString)

  def getDateTime: DateTime = new DateTime
}
