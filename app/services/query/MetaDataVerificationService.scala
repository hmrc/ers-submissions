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

import repositories.{MetaDataVerificationMongoRepository, Repositories}
import models.ERSQuery
import org.joda.time.DateTime
import play.api.Logger
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object MetaDataVerificationService extends MetaDataVerificationService {
  override lazy val metaDataVerificationRepository: MetaDataVerificationMongoRepository = Repositories.metaDataVerificationRepository
}

trait MetaDataVerificationService extends DataVerificationConfig {
  lazy val metaDataVerificationRepository: MetaDataVerificationMongoRepository = ???

  def start() = {
    Logger.info(s"Start MetaData Verification ${DateTime.now.toString}")
    getCountBySchemeTypeWithInDateRange
    getBundleRefAndSchemeRefBySchemeTypeWithInDateRange
  }

  def getCountBySchemeTypeWithInDateRange():Future[Int] = {
    metaDataVerificationRepository.getCountBySchemeTypeWithInDateRange(ersQuery).map{ total=>
      Logger.warn(s"The total number of ${ersQuery.schemeType} Scheme Type files available in the 'ers-metadata' is => ${total}")
      total
    }
  }

  def getBundleRefAndSchemeRefBySchemeTypeWithInDateRange():Future[List[(String,String,String)]] = {
    metaDataVerificationRepository.getBundleRefAndSchemeRefBySchemeTypeWithInDateRange(ersQuery).map{ schemeRefsList =>
        Logger.warn(s"The total (BundleRefs,SchemeRefs,TransferStatus) of ${ersQuery.schemeType} Scheme Type available in the 'ers-metadata' are => ${schemeRefsList}")
        schemeRefsList
      }
  }

}
