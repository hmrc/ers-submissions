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

package repositories

import config.ApplicationConfig
import javax.inject.Inject
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.DefaultDB
import uk.gov.hmrc.lock.LockRepository

import scala.concurrent.ExecutionContext


class Repositories @Inject()(applicationConfig: ApplicationConfig, mongoComponent: ReactiveMongoComponent)
                            (implicit ec: ExecutionContext) {

  private implicit val connection: () => DefaultDB = {
    mongoComponent.mongoConnector.db
  }

  lazy val presubmissionRepository: PresubmissionMongoRepository = new PresubmissionMongoRepository(applicationConfig, mongoComponent)
  lazy val metadataRepository: MetadataMongoRepository = new MetadataMongoRepository(applicationConfig, mongoComponent)
  lazy val dataVerificationRepository: DataVerificationMongoRepository = new DataVerificationMongoRepository(applicationConfig, mongoComponent)
  lazy val metaDataVerificationRepository: MetaDataVerificationMongoRepository = new MetaDataVerificationMongoRepository(applicationConfig, mongoComponent)
  lazy val lockRepository: LockRepository = new LockRepository
}
