/*
 * Copyright 2016 HM Revenue & Customs
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

package services

import models.{Statuses, ErsJsonStoreInfo}
import org.joda.time.DateTime
import repositories.{PresubmissionRepository, MetadataRepository, Repositories, JsonStoreInfoRepository}
import utils.LoggingAndRexceptions.ErsLogger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object MonitoringService extends MonitoringService {
  override val jsonStoreInfoRepository: JsonStoreInfoRepository = Repositories.postsubmissionRepository
  override val presubmissionRepository: PresubmissionRepository = Repositories.presubmissionRepository
  override val metadataRepository: MetadataRepository = Repositories.metadataRepository
}

trait MonitoringService extends ErsLogger {
  val limit: Int = 50000

  val jsonStoreInfoRepository: JsonStoreInfoRepository
  val presubmissionRepository: PresubmissionRepository
  val metadataRepository: MetadataRepository

  def jsonStoreInfoState(status: String): Future[Unit] = {
    jsonStoreInfoRepository.findJsonStoreInfoByStatus(List(status), limit).map { res =>
      val schemeRefList = res.map(_.schemeInfo.schemeRef)
      logSliced(
        schemeRefList,
        s" records in JsonStoreInfo with status ${status}.",
        s"SchemeRefs for JsonStoreInfo with status ${status}"
      )
    }.recover {
      case ex: Exception => logException(s"Failed to retrieve JsonStoreInfo for status ${status}", ex, Some("MonitoringService.jsonStoreInfoState"))
    }
  }

  def missingPresubmissionData(startDate: DateTime, endDate: DateTime): Future[Unit] = {
    presubmissionRepository.getSchemeRefs(startDate, endDate).flatMap { presubSchemeRef =>
      metadataRepository.getSchemeRefs(startDate, endDate, presubSchemeRef).map { metaSchemeRef =>
        logSliced(
          metaSchemeRef,
          s"records in metadataRepository that doesn't have presubmission data for ${startDate.toString()}.",
          s"SchemeRefs for metadataRepository that doesn't have presubmission data for ${startDate.toString()}"
        )
      }.recover {
        case ex: Exception => logException(s"Failed to retrieve SchemeRefs from metadataRepository for ${startDate.toString()}", ex, Some("MonitoringService.missingPresubmissionData"))
      }
    }.recover {
      case ex: Exception => logException(s"Failed to retrieve SchemeRefs from presubmissionRepository for ${startDate.toString()}", ex, Some("MonitoringService.missingPresubmissionData"))
    }
  }

  def syncMetadataAndJsonStoreInfo(startDate: DateTime, endDate: DateTime): Future[Unit] = {
    jsonStoreInfoRepository.getSchemeInfoForPeriod(startDate, endDate).flatMap { schemeInfoList =>
      metadataRepository.getSchemeInfo(startDate, endDate, schemeInfoList).map { schemeInfoToAddList =>
        val schemeRefs = schemeInfoToAddList.map(_.schemeRef)
        logSliced(
          schemeRefs,
          s"records to add in JsonStoreInfo for ${startDate.toString()}.",
          s"SchemeRefs for metadataRepository that doesn't exists in JsonStoreInfo for ${startDate.toString()}"
        )
      }.recover {
        case ex: Exception => logException(s"Failed to retrieve SchemeInfoss from metadataRepository for ${startDate.toString()}", ex, Some("MonitoringService.syncMetadataAndJsonStoreInfo"))
      }
    }.recover {
      case ex: Exception => logException(s"Failed to retrieve SchemeInfoss from jsonStoreInfoRepository for ${startDate.toString()}", ex, Some("MonitoringService.syncMetadataAndJsonStoreInfo"))
    }
  }

  def syncMetadataAndJsonStoreInfoBySchemeRef(schemeRefs: List[String]): Future[List[Unit]] = {
    jsonStoreInfoRepository.getSchemeInfoBySchemeRefs(schemeRefs).flatMap { schemeInfoFromJsonStore =>
      if(schemeInfoFromJsonStore.isEmpty) {
        logWarn(s"There is nothing found in jsonStoreInfoRepository for ${schemeRefs.toList}")
      }
      metadataRepository.getSchemeInfoBySchemeRefs(schemeRefs).flatMap { schemeInfoFromMetadata =>
        if(schemeInfoFromMetadata.isEmpty) {
          logWarn(s"There is nothing found in metadataRepository for ${schemeRefs.toList}")
        }
        val schemeInfoToAddList = schemeInfoFromMetadata.filterNot(schemeInfoFromJsonStore.contains(_))
        logWarn(s"${schemeInfoToAddList.length} records should be added to ErsJsonStoreInfo")
        Future.sequence(
          schemeInfoToAddList.map { schemeInfo =>
            logWarn(s"Start creating ErsJsonStoreInfo", Some(schemeInfo))
            jsonStoreInfoRepository.createErsJsonStoreInfo(
              ErsJsonStoreInfo(schemeInfo, None, None, None, None, Statuses.Saved.toString)
            ).map { res =>
              logWarn(s"ErsJsonStoreInfo successfuly created ${res}", Some(schemeInfo))
            }.recover {
              case ex: Exception => logException(s"Exception during creation of JsonStoreInfo for: ${schemeInfo.schemeRef}", ex, Some("MonitoringService.syncMetadataAndJsonStoreInfoBySchemeRef"))
            }
          }
        )
      }.recover {
        case ex: Exception => List(
          logException(s"Failed to retrieve SchemeInfos from metadataRepository for ${schemeRefs.toString()}", ex, Some("MonitoringService.syncMetadataAndJsonStoreInfoBySchemeRef"))
        )
      }
    }.recover {
      case ex: Exception => List(
        logException(s"Failed to retrieve SchemeInfoss from jsonStoreInfoRepository for ${schemeRefs.toString()}", ex, Some("MonitoringService.syncMetadataAndJsonStoreInfoBySchemeRef"))
      )
    }
  }

}
