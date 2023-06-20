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

package utils

import models.{ADRTransferError, ERSError}
import play.api.Logging
import play.api.http.Status.BAD_REQUEST
import play.api.libs.json.{JsError, JsPath, Json, JsonValidationError}
import play.api.mvc.Result
import play.api.mvc.Results.BadRequest
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import scala.concurrent.Future

trait ErrorHandlerHelper extends Logging {
  val className: String

  def handleError(ex: Throwable, methodName: String): ERSError = {
    logger.error(s"[$className][$methodName] Exception thrown with message ${ex.getMessage}")
    ADRTransferError()
  }

  def handleBadRequest(jsonValidationErrors: scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])]): Future[Result] = {
    val errorResponseBody = Json.toJson(ErrorResponse(BAD_REQUEST, JsError.toJson(jsonValidationErrors).toString()))
    Future.successful(BadRequest(errorResponseBody))
  }
}
