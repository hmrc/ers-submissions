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
import helpers.ERSTestHelper
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import repositories.{DataVerificationMongoRepository, Repositories}

import scala.concurrent.{ExecutionContext, Future}

class DataVerificationServiceSpec extends ERSTestHelper {

  val mockApplicationConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val mockRepositories: Repositories = mock[Repositories]
  val mockDataVerificationRepository: DataVerificationMongoRepository = mock[DataVerificationMongoRepository]

  def buildDataVerificationService(implicit ec: ExecutionContext): DataVerificationService =
    new DataVerificationService(mockApplicationConfig, mockRepositories) {
    override lazy val dataVerificationRepository: DataVerificationMongoRepository = mockDataVerificationRepository
    when(mockDataVerificationRepository.getCountBySchemeTypeWithInDateRange(any()))
      .thenReturn(Future(10L))
  }

  "Calling getCountBySchemeTypeWithInDateRange" should {

    "return correct count" in {
      val totalNumberOfRecords = 10
      val dataVerificationService =  buildDataVerificationService
      val result = await(dataVerificationService.getCountBySchemeTypeWithInDateRange)
      result shouldBe totalNumberOfRecords
    }
  }
}
