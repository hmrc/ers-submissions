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

package repositories.helpers

import helpers.ERSTestHelper
import org.mongodb.scala.bson.{BsonDocument, BsonString}
import repositories.helpers.BsonDocumentHelper.BsonOps

class BsonDocumentHelperSpec extends ERSTestHelper {

  val emptyBson: BsonDocument = BsonDocument()
  val nonEmptyBson: BsonDocument = BsonDocument("nonEmpty" -> "stuff", "another" -> "also stuff", "this one" -> "also works")

  "bsonToSeqOfTuples" should {
    "convert an empty Bson document into an empty sequence" in {
      BsonDocumentHelper.bsonToSeqOfTuples(emptyBson) shouldBe Seq()
    }

    "convert non-empty Bson into a valid sequence" in {
      BsonDocumentHelper.bsonToSeqOfTuples(nonEmptyBson) shouldBe Seq("nonEmpty" -> BsonString("stuff"), "another" -> BsonString("also stuff"), "this one" -> BsonString("also works"))
    }
  }

  "+:+" should {
    "add two empty Bson documents and result in an empty Bson document" in {
      emptyBson +:+ emptyBson shouldBe emptyBson
    }
    "add an empty document to a non-empty document" in {
      nonEmptyBson +:+ emptyBson shouldBe nonEmptyBson
    }
    "add a non-empty document to an empty document" in {
      emptyBson +:+ nonEmptyBson shouldBe nonEmptyBson
    }
    "add two non-empty documents" in {
      nonEmptyBson +:+ BsonDocument("another" -> "one") shouldBe BsonDocument("nonEmpty" -> "stuff", "another" -> "also stuff", "this one" -> "also works", "another" -> "one")
    }
  }
}
