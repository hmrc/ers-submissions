/*
 * Copyright 2020 HM Revenue & Customs
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

package fixtures

import controllers.auth.AuthAction
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.{Answer, OngoingStubbing}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, BodyParser, BodyParsers, Request, Result}

import scala.concurrent.{ExecutionContext, Future}

trait WithMockedAuthActions {

  val mockAuthAction: AuthAction

  def mockJsValueAuthAction: OngoingStubbing[Action[JsValue]] =
    when(mockAuthAction.async(any[BodyParser[JsValue]])(any[Request[JsValue] => Future[Result]]()))
      .thenAnswer(new Answer[Action[JsValue]] {
        override def answer(invocation: InvocationOnMock): Action[JsValue] = {
          val passedInBodyParser = invocation.getArguments()(0).asInstanceOf[BodyParser[JsValue]]
          val passedInBlock = invocation.getArguments()(1).asInstanceOf[Request[JsValue] => Future[Result]]
          new Action[JsValue]{
            override def parser: BodyParser[JsValue] = passedInBodyParser
            override def apply(request: Request[JsValue]): Future[Result] = passedInBlock(request)
            override def executionContext: ExecutionContext = ExecutionContext.global
          }
        }
      })
}
