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

package repositories.helpers

import config.ApplicationConfig
import models.ERSQuery
import org.joda.time.DateTime
import org.mongodb.scala.bson.{BsonDocument, BsonString}
import repositories.helpers.BsonDocumentHelper.BsonOps

trait BaseVerificationRepository {

  val applicationConfig: ApplicationConfig
  val mongoKeyPrefix: String = ""

  lazy val dateRangeSelector: ERSQuery => BsonDocument = ersQuery => BsonDocument(
    s"${mongoKeyPrefix}schemeInfo.timestamp" -> BsonDocument(
      "$gte" -> DateTime.parse(ersQuery.startDate.getOrElse(applicationConfig.defaultScheduleStartDate)).getMillis,
      "$lte" -> DateTime.parse(ersQuery.endDate.getOrElse(applicationConfig.defaultScheduleStartDate)).getMillis
    )
  )

  lazy val schemeSelector: ERSQuery => BsonDocument = ersQuery => if (ersQuery.schemeType.nonEmpty) {
    BsonDocument(
      s"${mongoKeyPrefix}schemeInfo.schemeType" -> BsonString(ersQuery.schemeType.getOrElse(applicationConfig.ersQuerySchemeType))
    )
  }
  else {
    BsonDocument()
  }

  lazy val schemeRefsSelector: ERSQuery => BsonDocument = ersQuery => if (ersQuery.schemeRefsList.nonEmpty) {
    BsonDocument(s"${mongoKeyPrefix}schemeInfo.schemeRef" -> BsonDocument("$in" -> ersQuery.schemeRefsList))
  }
  else {
    BsonDocument()
  }

  def combineSelectors(listOfSelectors: Seq[ERSQuery => BsonDocument], ersQuery: ERSQuery): BsonDocument = {
    listOfSelectors.map(_(ersQuery)).foldLeft(BsonDocument())(_ +:+ _)
  }


}
