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

import uk.gov.hmrc.mongo.lock.LockService

trait ResubmissionMessages {
  val prefix: String = "[ResubmissionService]"
  val message: String
}

case class LockMessage(lockService: LockService) extends ResubmissionMessages {
  override val message: String = s"$prefix Searching for records to resubmit " +
    s"using database lock with id: ${lockService.lockId} " +
    s"(duration: ${lockService.ttl} and repository: ${lockService.lockRepository}"
}

case object FinishedResubmissionJob extends ResubmissionMessages {
  override val message: String = s"$prefix Finished resubmission job"
}

case object ResubmissionSuccessMessage extends ResubmissionMessages {
  override val message: String = s"$prefix Resubmission was successful"
}

case object ResubmissionFailedMessage extends ResubmissionMessages {
  override val message: String = s"$prefix Resubmission failed"
}

case object NoDataToResubmitMessage extends ResubmissionMessages {
  override val message: String = s"$prefix No data found for resubmission"
}

case object ProcessingResubmitMessage extends ResubmissionMessages {
  override val message: String = s"$prefix Processing resubmission event"
}

case class TotalNumberOfFailedJobsMessage(numberOfFailedJobs: Long, failedStatuses: List[String]) extends ResubmissionMessages {
  val message: String = s"$prefix There are $numberOfFailedJobs failed jobs in total with statuses ${failedStatuses.mkString(", ")}"
}

case class NumberOfFailedToBeProcessedMessage(numberOfFailedJobs: Long) extends ResubmissionMessages {
  val message: String = s"$prefix $numberOfFailedJobs failed jobs will be processed by this job"
}