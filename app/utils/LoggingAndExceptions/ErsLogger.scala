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

package utils.LoggingAndExceptions

import play.api.Logging

trait ErsLogger extends ErsDataMessages with ErsExceptionMessages with Logging {

  def buildMessage(message: String, data: Option[Object]): String = {
    data match {
      case Some(data) => message + " for " + buildDataMessage(data)
      case None => message
    }
  }

  def logException(data: Object, ex: Exception, context: Option[String] = None): Unit = {
    val errorMessage: Seq[String] = Seq(buildExceptionMesssage(ex), buildDataMessage(data))

    val finalErrorMessage: String =
      (if (context.isDefined) {
        errorMessage :+ s"Context: $context"
      } else {
        errorMessage
      }).mkString("\n")

    logError(finalErrorMessage)
  }

  def logIfEnabled(logEnabled: Boolean)(block: => Unit): Unit = {
    Option(logEnabled)
      .filter(identity)
      .foreach(_ => block)
  }

  // methods to help with testing
  def logInfo(message: String): Unit = logger.info(message)

  def logError(message: String): Unit = logger.error(message)

  def logError(message: String, e: Throwable): Unit = logger.error(message, e)

  def logWarn(message: String): Unit = logger.warn(message)

  def logWarn(message: String, e: Throwable): Unit = logger.warn(message, e)
}
