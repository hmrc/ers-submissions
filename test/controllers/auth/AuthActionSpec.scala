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

package controllers.auth

import helpers.ERSTestHelper
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.auth.core.{AuthConnector, BearerTokenExpired, ConfidenceLevel, Enrolment, InsufficientConfidenceLevel}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import play.api.http.Status
import play.api.mvc.{AnyContent, BodyParser, PlayBodyParsers, Request, Result, Results}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.domain.EmpRef

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AuthActionSpec extends ERSTestHelper with BeforeAndAfterEach {

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockBodyParser: PlayBodyParsers = mock[PlayBodyParsers]

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthConnector)
  }

  def authAction(empRef: String): AuthAction = new AuthAction {
    override val optionalEmpRef: Option[EmpRef] = Try(EmpRef.fromIdentifiers(empRef)).toOption
    override def authConnector: AuthConnector = mockAuthConnector
    override implicit val executionContext: ExecutionContext = ExecutionContext.global
    override def parser: BodyParser[AnyContent] = mockBodyParser.default
  }

  def defaultAsyncBody: Request[_] => Result = _ => Results.Ok("Successful")

  def getEnrolmentPredicate(taxOfficeNumber: String, taxOfficeReference: String): Predicate =
    ConfidenceLevel.L50 and Enrolment("IR-PAYE")
      .withIdentifier("TaxOfficeNumber", taxOfficeNumber)
      .withIdentifier("TaxOfficeReference", taxOfficeReference)
      .withDelegatedAuthRule("ers-auth")


  "AuthAction" should {
    "perform the action if the user is authorised " in {
      when(
        mockAuthConnector
          .authorise(
            eqTo(getEnrolmentPredicate("123", "2343234")),
            eqTo(EmptyRetrieval)
          )(
            any(), any()
          )
      ).thenReturn(Future.successful(()))

      val result: Future[Result] = authAction("123/2343234")(defaultAsyncBody)(FakeRequest())
      status(result) shouldBe Status.OK
      await(
        bodyOf(result).map(
          _ shouldBe "Successful"
        )
      )
    }

    "return a 401 if an Authorisation Exception is experienced" in {
      when(
        mockAuthConnector
          .authorise(
            eqTo(getEnrolmentPredicate("123", "2343234")),
            eqTo(EmptyRetrieval)
          )(
            any(), any()
          )
      ).thenReturn(Future.failed(InsufficientConfidenceLevel("failed")))

      val result: Future[Result] = authAction("123/2343234")(defaultAsyncBody)(FakeRequest())
      status(result) shouldBe Status.UNAUTHORIZED
    }

    "return a 401 if an NoActiveSession Exception is experienced" in {
      when(
        mockAuthConnector
          .authorise(
            eqTo(getEnrolmentPredicate("123", "2343234")),
            eqTo(EmptyRetrieval)
          )(
            any(), any()
          )
      ).thenReturn(Future.failed(BearerTokenExpired("failed")))

      val result: Future[Result] = authAction("123/2343234")(defaultAsyncBody)(FakeRequest())
      status(result) shouldBe Status.UNAUTHORIZED
    }

    "return a 401 if an InsufficientConfidenceLevel Exception is experienced" in {
      when(
        mockAuthConnector
          .authorise(
            eqTo(getEnrolmentPredicate("123", "2343234")),
            eqTo(EmptyRetrieval)
          )(
            any(), any()
          )
      ).thenReturn(Future.failed(InsufficientConfidenceLevel("failed")))

      val result: Future[Result] = authAction("123/2343234")(defaultAsyncBody)(FakeRequest())
      status(result) shouldBe Status.UNAUTHORIZED
    }

    "return a 401 if an invalid empref is passed in" in {
      val result: Future[Result] = authAction("12343234")(defaultAsyncBody)(FakeRequest())
      status(result) shouldBe Status.UNAUTHORIZED
    }
  }

}