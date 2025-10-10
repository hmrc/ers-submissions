/*
 * Copyright 2025 HM Revenue & Customs
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

import org.bson.Document

object SubmissionDataTemplates {

  val csopOptionExercised: Document = new Document()
    .append("_id", 0) // exclude _id
    .append("dateOfExercise", new Document("$arrayElemAt", Seq("$data", 0))) // TODO: Is there a better way to do this???
    .append("individual.firstName", new Document("$arrayElemAt", Seq("$data", 1)))
    .append("individual.secondName", new Document("$arrayElemAt", Seq("$data", 2)))
    .append("individual.surname", new Document("$arrayElemAt", Seq("$data", 3)))
    .append("individual.nino", new Document("$arrayElemAt", Seq("$data", 4)))
    .append("individual.payeReference", new Document("$arrayElemAt", Seq("$data", 5)))
    .append("dateOfGrant", new Document("$arrayElemAt", Seq("$data", 6)))
    .append("numberSharesAcquired", new Document("$arrayElemAt", Seq("$data", 7)))
    .append("sharesPartOfLargestClass", new Document("$arrayElemAt", Seq("$data", 8)))
    .append("sharesListedOnSE", new Document("$arrayElemAt", Seq("$data", 9)))
    .append("mvAgreedHMRC", new Document("$arrayElemAt", Seq("$data", 10)))
    .append("hmrcRef", new Document("$arrayElemAt", Seq("$data", 11)))
    .append("amvPerShareAtAcquisitionDate", new Document("$arrayElemAt", Seq("$data", 12)))
    .append("exerciseValuePerShare", new Document("$arrayElemAt", Seq("$data", 13)))
    .append("umvPerShareAtExerciseDate", new Document("$arrayElemAt", Seq("$data", 14)))
    .append("qualifyForTaxRelief", new Document("$arrayElemAt", Seq("$data", 15)))
    .append("payeOperatedApplied", new Document("$arrayElemAt", Seq("$data", 16)))
    .append("deductibleAmount", new Document("$arrayElemAt", Seq("$data", 17)))
    .append("nicsElectionAgreementEnteredInto", new Document("$arrayElemAt", Seq("$data", 18)))
    .append("sharesDisposedOnSameDay", new Document("$arrayElemAt", Seq("$data", 19)))
}
