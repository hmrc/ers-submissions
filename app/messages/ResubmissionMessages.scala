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

package messages

import models.{ErsSummary, SchemeData}
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
  override val message: String = s"$prefix Processing resubmission event for: "
}

case class TotalNumberSubmissionsToProcessMessage(numberOfFailedJobs: Long) extends ResubmissionMessages {
  val message: String = s"$prefix There are $numberOfFailedJobs submissions for the resubmission service to process"
}

case class NumberOfFailedToBeProcessedMessage(numberOfFailedJobs: Long) extends ResubmissionMessages {
  val message: String = s"$prefix $numberOfFailedJobs failed jobs will be processed by this job"
}

case class ResubmissionLimitMessage(resubmissionLimit: Long) extends ResubmissionMessages {
  val message: String = s"$prefix Running resubmission job with a batch size of: $resubmissionLimit"
}

case class AggregatedLogs(aggregatedLogs: Seq[AggregatedLog]) extends ResubmissionMessages {
  val message: String = s"$prefix Aggregated view of submissions:" +
    s"${aggregatedLogs.map(_.logLine).mkString("\n", "\n", "\n")}"
}

case class MetaDataSelectedSchemeRefLogs(selectedErsSummary: Seq[ErsSummary]) extends ResubmissionMessages {
  private def logLine(ersSummary: ErsSummary): String = s"schemaRef: ${ersSummary.metaData.schemeInfo.schemeRef}, " +
    s"schemaType: ${ersSummary.metaData.schemeInfo.schemeType}, " +
    s"taxYear: ${ersSummary.metaData.schemeInfo.taxYear}, " +
    s"transferStatus: ${ersSummary.transferStatus.getOrElse("transfer status is not defined")}, " +
    s"timestamp: ${ersSummary.metaData.schemeInfo.timestamp}"

  val numberSelectedErsRecords: Int = selectedErsSummary.length
  val message: String =
    if (selectedErsSummary.isEmpty) {
      s"$prefix MetaDataSelectedSchemeRefLogs - Could not find any records for the selected scheme reference"
    }
    else if (numberSelectedErsRecords > 50){
      s"$prefix MetaDataSelectedSchemeRefLogs - Selected schemes have more then 50 records ($numberSelectedErsRecords records selected)"
    } else {
      s"$prefix MetaDataSelectedSchemeRefLogs - Selected scheme details: ${selectedErsSummary.map(logLine).mkString("\n", "\n", "\n")}"
    }
}

case class PreSubSelectedSchemeRefLogs(selectedErsSummary: Seq[SchemeData]) extends ResubmissionMessages {
  private def logLine(schemeData: SchemeData): String =
    s"schemaRef: ${schemeData.schemeInfo.schemeRef}, " +
    s"schemaType: ${schemeData.schemeInfo.schemeType}, " +
    s"taxYear: ${schemeData.schemeInfo.taxYear}, " +
    s"timestamp: ${schemeData.schemeInfo.timestamp}"

  val numberSelectedErsRecords: Int = selectedErsSummary.length
  val message: String =
    if (selectedErsSummary.isEmpty) {
      s"$prefix PreSubSelectedSchemeRefLogs - Could not find any records for the selected scheme reference"
    }
    else if (numberSelectedErsRecords > 50){
      s"$prefix PreSubSelectedSchemeRefLogs - Selected schemes have more then 50 records ($numberSelectedErsRecords records selected)"
    } else {
      s"$prefix PreSubSelectedSchemeRefLogs - Selected scheme details: ${selectedErsSummary.map(logLine).mkString("\n", "\n", "\n")}"
    }
}
