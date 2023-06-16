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

package services.query

import config.ApplicationConfig

import javax.inject.Inject
import models.ERSMetaDataResults
import org.joda.time.DateTime
import play.api.Logging
import play.api.libs.json.{JsError, JsObject, JsSuccess}
import repositories.{DataVerificationMongoRepository, MetaDataVerificationMongoRepository, Repositories}
import scheduler.ScheduledService
import services.resubmission.SchedulerConfig

import scala.concurrent.{ExecutionContext, Future}

class MetaDataVerificationService @Inject()(val applicationConfig: ApplicationConfig,
                                            repositories: Repositories)(implicit ec: ExecutionContext)
  extends ScheduledService[Boolean]
    with Logging
    with SchedulerConfig {
  lazy val metaDataVerificationRepository: MetaDataVerificationMongoRepository = repositories.metaDataVerificationRepository
  lazy val dataVerificationRepository: DataVerificationMongoRepository = repositories.dataVerificationRepository

  override val jobName: String = "metadata-verification-service"

  override def invoke(implicit ec: ExecutionContext): Future[Boolean] = {
    logger.warn(s"[MetaDataVerificationService]: Start MetaData Verification ${DateTime.now.toString}")
    // TODO: Come back to.... should we break these services up?
    if (applicationConfig.checkMetaDataHasPresubmissionFileEnabled) {
      logAggregateMetadataMetrics
//      logWhichMetadataRecordsHavePresubmissionRecords
    }
    if (applicationConfig.getCountBySchemeTypeWithInDateRangeEnabled) {
      getCountBySchemeTypeWithInDateRange
    }
    if (applicationConfig.getBundleRefAndSchemeRefBySchemeTypeWithInDateRangeEnabled) {
      getBundleRefAndSchemeRefBySchemeTypeWithInDateRange
    }
    if (applicationConfig.getSchemeRefsInfoEnabled) {
      getSchemeRefsInfo
    }
    Future(true) // TODO: Come back to this
  }

  def logWhichMetadataRecordsHavePresubmissionRecords(): Future[Seq[Unit]] = { // TODO: Change return type to Future[Unit]
    checkMetaDataHasPresubmissionFile.map(
      _.map {
        case (matchingRecord: Boolean, schemaRef: String) =>
          if (matchingRecord) {
            logger.info(s"[MetaDataVerificationService]: Found presubmission record for: $schemaRef")
          }
          else {
            logger.info(s"[MetaDataVerificationService]: Missing presubmission record for: $schemaRef")
          }
      }
    )
  }

  def checkMetaDataHasPresubmissionFile: Future[Seq[(Boolean, String)]] = {
    for {
      metaDataWithTransferStatus: Seq[ERSMetaDataResults] <-
        metaDataVerificationRepository.getRecordsWithTransferStatus(applicationConfig.ersQuery)
      matchingPresubmissionData: Seq[(Boolean, String)] <- Future.sequence {
        metaDataWithTransferStatus.map((metaDataResult: ERSMetaDataResults) => {
          dataVerificationRepository.getPresubRecodFromMetadata(metaDataResult)
        })
      }
    } yield matchingPresubmissionData
  }

  def logAggregateMetadataMetrics: Future[Unit] = getAggregateMetadataMetrics.map(
    _.map(log =>
      logger.info(
        s"[MetaDataVerificationService]: Aggregated view of submissions: ${log.logLine}"
      )
    )
  )

  def getAggregateMetadataMetrics: Future[Seq[AggregatedLog]] =
    for {
      aggregatedRecords: Seq[JsObject] <- metaDataVerificationRepository
        .getAggregateCountOfSubmissions
      aggregatedLogs: Seq[AggregatedLog] = aggregatedRecords
        .flatMap(mapJsonToAggregatedLog)
    } yield aggregatedLogs

  def mapJsonToAggregatedLog(record: JsObject): Option[AggregatedLog] =
    record.validate[AggregatedLog] match {
      case JsSuccess(obj, _) =>
        Some(obj)
      case JsError(_) =>
        None
    }


  def getCountBySchemeTypeWithInDateRange: Future[Long] = {
    metaDataVerificationRepository.getCountBySchemeTypeWithInDateRange(applicationConfig.ersQuery).map { total =>
      logger.warn(s"The total number of ${applicationConfig.ersQuery.schemeType} Scheme Type files available in the 'ers-metadata' is => $total")
      total
    }
  }

  def getBundleRefAndSchemeRefBySchemeTypeWithInDateRange: Future[Seq[(String, String, String)]] = {
    metaDataVerificationRepository.getBundleRefAndSchemeRefBySchemeTypeWithInDateRange(applicationConfig.ersQuery)
      .map { schemeRefsList =>
        logger.warn(s"The total (BundleRefs,SchemeRefs,TransferStatus) of " +
          s"${applicationConfig.ersQuery.schemeType} Scheme Type available in the 'ers-metadata' are => $schemeRefsList")
        schemeRefsList
      }
  }

  def getSchemeRefsInfo: Future[Seq[ERSMetaDataResults]] = {
    metaDataVerificationRepository.getSchemeRefsInfo(applicationConfig.ersQuery).map { ersMetaDataResults =>
      logger.warn(s"(BundleRefs,SchemeRefs,TransferStatus,FileType,Timestamp, TaxYear) from 'ers-metadata' => $ersMetaDataResults")
      ersMetaDataResults
    }
  }

}
