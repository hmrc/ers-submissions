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

package repositories
import uk.gov.hmrc.mongo.MongoConnector
//import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.MongoDbConnection

import uk.gov.hmrc.lock.LockRepository

object Repositories extends MongoDbConnection{

  private implicit val connection = {
    import play.api.Play.current
   // ReactiveMongoPlugin.mongoConnector.db
    mongoConnector.db
  }

  lazy val presubmissionRepository: PresubmissionMongoRepository = new PresubmissionMongoRepository()
  lazy val metadataRepository: MetadataMongoRepository = new MetadataMongoRepository()
  lazy val lockRepository: LockRepository = new LockRepository
}
