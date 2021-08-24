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

import collection.JavaConverters._
import org.mongodb.scala.bson.{BsonDocument, BsonValue}

object BsonDocumentHelper {

  implicit class BsonOps(bsonDocument: BsonDocument) {
    private def bsonToSeqOfTuples(bson: BsonDocument): Seq[(String, BsonValue)] = {
      bson.entrySet().asScala.toSeq.map(javaMap => (javaMap.getKey, javaMap.getValue))
    }

    //scalastyle:off method.name
    def +:+(toAdd: BsonDocument): BsonDocument = {
      BsonDocument(bsonToSeqOfTuples(bsonDocument) ++ bsonToSeqOfTuples(toAdd))
    }
  }

}
