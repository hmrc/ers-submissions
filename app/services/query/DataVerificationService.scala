/*
 * Copyright 2022 HM Revenue & Customs
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
import repositories.{DataVerificationMongoRepository, Repositories}
import models.ERSDataResults
import org.joda.time.DateTime
import play.api.Logging

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class DataVerificationService @Inject()(applicationConfig: ApplicationConfig, repositories: Repositories) extends Logging {
  lazy val dataVerificationRepository: DataVerificationMongoRepository = repositories.dataVerificationRepository

  def start: Future[Seq[ERSDataResults]] = {
    logger.info(s"Start DataVerification ${DateTime.now.toString}")
    getCountBySchemeTypeWithInDateRange
    getSchemeRefBySchemeTypeWithInDateRange
    getSchemeRefsInfo
  }

  def getCountBySchemeTypeWithInDateRange: Future[Long] = {
      dataVerificationRepository.getCountBySchemeTypeWithInDateRange(applicationConfig.ersQuery).map{ total=>
        logger.warn(s"The total number of ${applicationConfig.ersQuery.schemeType} Scheme Type files available in the 'ers-presubmission' is => ${total}")
        total
      }
  }

  def getSchemeRefBySchemeTypeWithInDateRange: Future[Seq[String]] = {
    dataVerificationRepository.getSchemeRefBySchemeTypeWithInDateRange(applicationConfig.ersQuery).map{ schemeRefsList =>
      logger.warn(s"The total (SchemeRefs) of ${applicationConfig.ersQuery.schemeType} Scheme Type available in the 'ers-presubmission' are => ${schemeRefsList}")
      schemeRefsList
    }
  }

  def getSchemeRefsInfo: Future[Seq[ERSDataResults]] = {
    dataVerificationRepository.getSchemeRefsInfo(applicationConfig.ersQuery).map{ ersDataResults =>
      logger.warn(s" (SchemeRef,TaxYear,TimeStamp,SheetName) from 'ers-presubmission' => ${ersDataResults}")
      ersDataResults
    }
  }

}
