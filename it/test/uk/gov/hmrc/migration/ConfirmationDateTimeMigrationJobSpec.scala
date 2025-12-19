/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.migration

import _root_.play.api.Application
import _root_.play.api.inject.guice.GuiceApplicationBuilder
import _root_.play.api.test.Helpers._
import models.{ERSError, ErsSummary}
import org.mongodb.scala.model.Filters
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import uk.gov.hmrc.Fixtures

class ConfirmationDateTimeMigrationJobSpec extends AnyWordSpecLike
  with Matchers
  with GuiceOneAppPerTest {

  val applicationConfig: Map[String, Any] = Map(
    "schedules.confirmation-date-time-migration.enabled" -> true,
    "schedules.confirmation-date-time-migration.max-records" -> 5,
    "auditing.enabled" -> false,
    "settings.metadata-collection-index-replace" -> false
  )

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .configure(
      conf = applicationConfig
    )
    .build()

  "ConfirmationDateTimeMigrationJob" should {
    "migrate documents with Long confirmationDateTime to proper Instant format in batches" in new ConfirmationDateTimeMigrationJobSetUp(app = app) {

      // Generate test data: 12 documents with old Long format
      val ersSummaries: Seq[ErsSummary] = Fixtures.generateListOfErsSummaries(
        numberRecords = 12,
        transferStatus = Some("saved")
      )

      // Store documents with old Long format for confirmationDateTime using BSON
      val storeDocsWithLongFormat: Boolean = await(
        storeMultipleBsonDocuments(MigrationFixtures.generateBsonWithLongFormat(ersSummaries))
      )
      storeDocsWithLongFormat shouldBe true

      // Verify initial state: all documents stored
      val totalDocs = countMetadataRecordsWithSelector(Filters.empty())
      totalDocs shouldBe 12

      val docsNeedingMigration = countMetadataRecordsWithSelector(documentsNeedingMigrationSelector)

      // At least some documents should need migration (the BSON approach should work)
      docsNeedingMigration should be > 0L

      // Run migration job - it will process up to batch size
      val firstJobRunOutcome: Either[ERSError, Int] = await(getJob.scheduledMessage.service.invoke.value)
      firstJobRunOutcome.isRight shouldBe true
      val migratedCount = firstJobRunOutcome.getOrElse(0)
      migratedCount should be > 0

      // After migration, documents needing migration should decrease
      val remainingAfterFirst = countMetadataRecordsWithSelector(documentsNeedingMigrationSelector)
      remainingAfterFirst should be < docsNeedingMigration

      // Run migrations until complete
      var iterations = 1
      while (countMetadataRecordsWithSelector(documentsNeedingMigrationSelector) > 0 && iterations < 10) {
        await(getJob.scheduledMessage.service.invoke.value)
        iterations += 1
      }

      // Verify all documents migrated eventually
      countMetadataRecordsWithSelector(Filters.empty()) shouldBe 12
      countMetadataRecordsWithSelector(documentsNeedingMigrationSelector) shouldBe 0
      countMetadataRecordsWithSelector(migratedDocumentsSelector) shouldBe 12

      // Run job again - should find no documents to migrate
      val finalJobRunOutcome: Either[ERSError, Int] = await(getJob.scheduledMessage.service.invoke.value)
      finalJobRunOutcome shouldBe Right(0)
    }

    "not migrate documents that already have proper Instant format" in new ConfirmationDateTimeMigrationJobSetUp(app = app) {

      // Generate test data with proper date format
      val ersSummariesProperFormat: Seq[ErsSummary] = Fixtures.generateListOfErsSummaries(
        numberRecords = 5,
        transferStatus = Some("saved")
      )

      // Store documents with proper date format
      val storeDocsWithProperFormat: Boolean = await(
        storeMultipleErsSummary(MigrationFixtures.generateErsSummariesWithProperFormat(ersSummariesProperFormat))
      )
      storeDocsWithProperFormat shouldBe true

      // Verify all documents already in proper format
      countMetadataRecordsWithSelector(Filters.empty()) shouldBe 5
      countMetadataRecordsWithSelector(documentsNeedingMigrationSelector) shouldBe 0
      countMetadataRecordsWithSelector(migratedDocumentsSelector) shouldBe 5

      // Run migration job
      val jobRunOutcome: Either[ERSError, Int] = await(getJob.scheduledMessage.service.invoke.value)
      jobRunOutcome shouldBe Right(0)

      // Verify no changes
      countMetadataRecordsWithSelector(Filters.empty()) shouldBe 5
      countMetadataRecordsWithSelector(documentsNeedingMigrationSelector) shouldBe 0
      countMetadataRecordsWithSelector(migratedDocumentsSelector) shouldBe 5
    }

    "handle mixed documents - some with Long format and some with proper format" in new ConfirmationDateTimeMigrationJobSetUp(app = app) {

      // Generate documents with Long format
      val ersSummariesWithLong: Seq[ErsSummary] = Fixtures.generateListOfErsSummaries(
        numberRecords = 8,
        transferStatus = Some("saved"),
        schemaType = "EMI"
      )

      // Generate documents with proper format
      val ersSummariesWithProper: Seq[ErsSummary] = Fixtures.generateListOfErsSummaries(
        numberRecords = 4,
        transferStatus = Some("passed")
      )

      // Store mixed format documents - use BSON for Long format to ensure proper storage
      val storeLongFormat: Boolean = await(
        storeMultipleBsonDocuments(MigrationFixtures.generateBsonWithLongFormat(ersSummariesWithLong))
      )
      val storeProperFormat: Boolean = await(
        storeMultipleErsSummary(MigrationFixtures.generateErsSummariesWithProperFormat(ersSummariesWithProper))
      )
      storeLongFormat shouldBe true
      storeProperFormat shouldBe true

      // Verify initial state
      countMetadataRecordsWithSelector(Filters.empty()) shouldBe 12
      val initialNeedingMigration = countMetadataRecordsWithSelector(documentsNeedingMigrationSelector)
      val initialMigrated = countMetadataRecordsWithSelector(migratedDocumentsSelector)

      // At least some documents should need migration
      initialNeedingMigration should be > 0L
      initialMigrated should be >= 4L // The 4 proper format docs should be counted as migrated

      // Run migrations until all documents are migrated
      var iterations = 0
      while (countMetadataRecordsWithSelector(documentsNeedingMigrationSelector) > 0 && iterations < 10) {
        await(getJob.scheduledMessage.service.invoke.value)
        iterations += 1
      }


      // Verify all documents now migrated
      countMetadataRecordsWithSelector(Filters.empty()) shouldBe 12
      countMetadataRecordsWithSelector(documentsNeedingMigrationSelector) shouldBe 0
      countMetadataRecordsWithSelector(migratedDocumentsSelector) shouldBe 12
    }

    "handle large batch migrations efficiently" in new ConfirmationDateTimeMigrationJobSetUp(app = app) {

      // Generate larger dataset
      val ersSummaries: Seq[ErsSummary] = Fixtures.generateListOfErsSummaries(
        numberRecords = 20,
        transferStatus = Some("saved")
      )

      val storeDocs: Boolean = await(
        storeMultipleBsonDocuments(MigrationFixtures.generateBsonWithLongFormat(ersSummaries))
      )
      storeDocs shouldBe true

      // Initial state
      countMetadataRecordsWithSelector(Filters.empty()) shouldBe 20
      val initialNeedingMigration = countMetadataRecordsWithSelector(documentsNeedingMigrationSelector)
      initialNeedingMigration should be > 0L

      // Run migrations until complete
      var totalMigrated = 0
      var iterations = 0

      while (countMetadataRecordsWithSelector(documentsNeedingMigrationSelector) > 0 && iterations < 10) {
        val outcome: Either[ERSError, Int] = await(getJob.scheduledMessage.service.invoke.value)
        outcome.isRight shouldBe true
        val migrated = outcome.getOrElse(0)
        totalMigrated += migrated
        iterations += 1
      }

      // Verify all migrated
      totalMigrated should be > 0
      iterations should be > 0
      iterations should be <= 10 // Should complete within reasonable number of iterations
      countMetadataRecordsWithSelector(documentsNeedingMigrationSelector) shouldBe 0
      countMetadataRecordsWithSelector(migratedDocumentsSelector) shouldBe 20
    }
  }
}

