/*
 * Copyright 2017 HM Revenue & Customs
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

import repositories.{DataVerificationMongoRepository, Repositories}
import models.ERSQuery
import org.joda.time.DateTime
import play.api.Logger
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object DataVerificationService extends DataVerificationService {
  override lazy val dataVerificationRepository: DataVerificationMongoRepository = Repositories.dataVerificationRepository
}

trait DataVerificationService extends DataVerificationConfig {
  lazy val dataVerificationRepository: DataVerificationMongoRepository = ???

  def start() = {
    Logger.info(s"Start DataVerification ${DateTime.now.toString}")
    getCountBySchemeTypeWithInDateRange
    getSchemeRefBySchemeTypeWithInDateRange
  }

  def getCountBySchemeTypeWithInDateRange():Future[Int] = {
      dataVerificationRepository.getCountBySchemeTypeWithInDateRange(ersQuery).map{ total=>
        Logger.warn(s"The total number of ${ersQuery.schemeType} Scheme Type files available in the 'ers-presubmission' database is => ${total}")
        total
      }
  }

  def getSchemeRefBySchemeTypeWithInDateRange():Future[List[String]] = {
    dataVerificationRepository.getSchemeRefBySchemeTypeWithInDateRange(ersQuery).map{ schemeRefsList =>
      Logger.warn(s"The total (SchemeRefs) of ${ersQuery.schemeType} Scheme Type available in the 'ers-presubmission' database are => ${schemeRefsList}")
      schemeRefsList
    }
  }

  def ersQuery: ERSQuery = {
    ERSQuery(Some(ersQuerySchemeType),Some(ersQueryStartDate),Some(ersQueryEndDate),None)
  }
}
