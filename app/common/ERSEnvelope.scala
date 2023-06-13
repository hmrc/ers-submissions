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

package common

import cats.data.EitherT
import models.ERSError

import scala.concurrent.{ExecutionContext, Future}

object ERSEnvelope {
  type ERSEnvelope[T] = EitherT[Future, ERSError, T]

  def apply[T](t: T): EitherT[Future, ERSError, T] =
    EitherT[Future, ERSError, T](Future.successful(Right(t)))

  def apply[T](ersErrors: ERSError): ERSEnvelope[T] =
    EitherT[Future, ERSError, T](Future.successful(Left(ersErrors)))

  def apply[T](t: Future[T])(implicit ec: ExecutionContext): ERSEnvelope[T] = EitherT.right(t)

  def apply[T](eitherArg: Either[ERSError, T])(implicit ec: ExecutionContext): ERSEnvelope[T] =
    EitherT.fromEither[Future](eitherArg)

  def fromFuture[T](t: Future[T])(implicit ec: ExecutionContext): ERSEnvelope[T] = EitherT.right(t)
}
