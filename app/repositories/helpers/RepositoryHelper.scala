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

package repositories.helpers

import models.{MongoError, MongoGenericError, MongoUnavailableError}
import org.mongodb.scala.MongoSocketWriteException
import play.api.Logging

trait RepositoryHelper extends Logging {

  def mongoRecover[T](repository: String,
                      method: String,
                      message: String,
                      sessionId: String,
                      optSchemaRefs: Option[Seq[String]] = None): PartialFunction[Throwable, Either[MongoError, T]] = new PartialFunction[Throwable, Either[MongoError, T]] {

    val genericMessage = s"[$repository][$method][SessionId: $sessionId] $message."

    private def logMessage: String = optSchemaRefs.fold(genericMessage){ schemaRefs =>
      s"$genericMessage SchemaRefs: ${schemaRefs.map(_.mkString(","))}"
    }

    override def isDefinedAt(x: Throwable): Boolean = x.isInstanceOf[Exception]

    override def apply(e: Throwable): Either[MongoError, T] = e match {
      case unavailable: MongoSocketWriteException =>
        logger.error(s"$logMessage MongoDB is unavailable.", unavailable)
        Left(MongoUnavailableError(unavailable.getMessage))
      case other: Throwable =>
        logger.error(s"$logMessage Error: ${e.getMessage}.", other)
        Left(MongoGenericError(other.getMessage))
    }
  }
}
