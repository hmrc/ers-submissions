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

package models

sealed trait ERSError
final case class ADRTransferError(transferStatus: Option[Int] = None) extends ERSError
final case class SubmissionStatusUpdateError(adrSubmissionTransferStatus: Option[Int] = None, transferStatus: Option[String] = None) extends ERSError
final case class ResubmissionError() extends ERSError
final case class JsonFromSheetsCreationError(message: String) extends ERSError
final case class SchemeDataMappingError(message: String) extends ERSError
final case class NoData() extends ERSError

//Database errors
abstract class MongoError extends ERSError
final case class MongoUnavailableError(message: String) extends MongoError
final case class MongoGenericError(message: String) extends MongoError
