/*
 * Copyright 2021 HM Revenue & Customs
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
import repositories.{MetaDataVerificationMongoRepository, Repositories}
import models.ERSMetaDataResults
import org.joda.time.DateTime
import play.api.Logging

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class MetaDataVerificationService @Inject()(applicationConfig: ApplicationConfig, repositories: Repositories) extends Logging {
  lazy val metaDataVerificationRepository: MetaDataVerificationMongoRepository = repositories.metaDataVerificationRepository

  def start: Future[List[ERSMetaDataResults]] = {
    logger.warn(s"Start MetaData Verification ${DateTime.now.toString}")
    getCountBySchemeTypeWithInDateRange
    getBundleRefAndSchemeRefBySchemeTypeWithInDateRange
    getSchemeRefsInfo
  }

  def getCountBySchemeTypeWithInDateRange: Future[Int] = {
    metaDataVerificationRepository.getCountBySchemeTypeWithInDateRange(applicationConfig.ersQuery).map{ total=>
      logger.warn(s"The total number of ${applicationConfig.ersQuery.schemeType} Scheme Type files available in the 'ers-metadata' is => ${total}")
      total
    }
  }

  def getBundleRefAndSchemeRefBySchemeTypeWithInDateRange: Future[List[(String,String,String)]] = {
    metaDataVerificationRepository.getBundleRefAndSchemeRefBySchemeTypeWithInDateRange(applicationConfig.ersQuery).map{ schemeRefsList =>
        logger.warn(s"The total (BundleRefs,SchemeRefs,TransferStatus) of ${applicationConfig.ersQuery.schemeType} Scheme Type available in the 'ers-metadata' are => ${schemeRefsList}")
        schemeRefsList
      }
  }

  def getSchemeRefsInfo: Future[List[ERSMetaDataResults]] = {
    metaDataVerificationRepository.getSchemeRefsInfo(applicationConfig.ersQuery).map{ ersMetaDataResults =>
      logger.warn(s"(BundleRefs,SchemeRefs,TransferStatus,FileType,Timestamp, TaxYear) from 'ers-metadata' => ${ersMetaDataResults}")
      ersMetaDataResults
    }
  }

}
