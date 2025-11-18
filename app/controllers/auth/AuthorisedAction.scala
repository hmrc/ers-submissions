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

package controllers.auth

import play.api.mvc.Results.Unauthorized
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.LoggingAndExceptions.ErsLogger

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

case class AuthorisedAction(slashSeperatedRef: String, authConnector: AuthConnector, bodyParser: PlayBodyParsers)
                           (implicit val executionContext: ExecutionContext)
  extends AuthAction {
  override val optionalEmpRef: Option[EmpRef] = Try(EmpRef.fromIdentifiers(slashSeperatedRef)).toOption
  val parser: BodyParser[AnyContent] = bodyParser.default
}

trait AuthAction extends AuthorisedFunctions with ActionBuilder[Request, AnyContent] with ActionFunction[Request, Request] with ErsLogger {

  val optionalEmpRef: Option[EmpRef]
  implicit val executionContext: ExecutionContext

  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    optionalEmpRef.map(empRef =>
      authorised(
        ConfidenceLevel.L50 and Enrolment("IR-PAYE")
          .withIdentifier("TaxOfficeNumber", empRef.taxOfficeNumber)
          .withIdentifier("TaxOfficeReference", empRef.taxOfficeReference)
          .withDelegatedAuthRule("ers-auth")
      ) {
        block(request)
      } recover {
        case ex: AuthorisationException =>
          logWarn(s"[AuthAction][invokeBlock] user is unauthorised for ${request.uri} with exception  ${ex.reason}", ex)
          Unauthorized
      }
    ).getOrElse {
      logWarn(s"[AuthAction][invokeBlock] empRef is invalid ${request.uri}")
      Future.successful(Unauthorized)
    }
  }
}
