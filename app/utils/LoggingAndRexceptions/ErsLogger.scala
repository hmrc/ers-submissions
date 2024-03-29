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

package utils.LoggingAndRexceptions

import play.api.Logging

trait ErsLogger extends ErsDataMessages with ErsExceptionMessages with Logging {

  def buildMessage(message: String, data: Option[Object]): String = {
    data match {
      case Some(_) => message + " for " + buildDataMessage(data.get)
      case None => message
    }
  }

  def logException(data: Object, ex: Exception, context: Option[String] = None): Unit = {
    var errorMessage: String = buildExceptionMesssage(ex) + ",\n" + buildDataMessage(data)
    if(context.isDefined) {
      errorMessage += s",\nContext: $context"
    }
    logger.error(errorMessage)
  }

  def logIfEnabled(logEnabled: Boolean)(block: => Unit): Unit = {
    Option(logEnabled)
      .filter(identity)
      .foreach( _ => block)
  }
}
