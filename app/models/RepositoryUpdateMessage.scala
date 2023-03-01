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

trait RepositoryUpdateMessage {
  val message: String
}

case object UpdateRequestNotAcknowledged extends RepositoryUpdateMessage {
  override val message: String = "Request to update presubmission repository was not acknowledged. Data was not updated."
}

case object UpdateRequestNothingToUpdate extends RepositoryUpdateMessage {
  override val message: String = "There is no more documents to update. Consider disabling scheduler."
}

case object FailedToLockRepositoryForUpdate extends RepositoryUpdateMessage {
  override val message: String = "Failed to acquire lock. Update job not completed."
}

case class UpdateRequestAcknowledged(count: Long) extends RepositoryUpdateMessage {
  override val message: String = s"Updated $count document(s) with createdAt field."
}

case class UpdateRepositoryError(error: String) extends RepositoryUpdateMessage {
  override val message: String = s"Exception occured when tried to perform update. Exception: $error."
}
