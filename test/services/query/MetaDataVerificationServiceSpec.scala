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

package services.query

import config.ApplicationConfig
import helpers.ERSTestHelper
import org.mockito.Mockito.when
import play.api.Logger
import play.api.libs.json.{JsObject, Json}
import repositories.{MetaDataVerificationMongoRepository, Repositories}

import scala.concurrent.Future

class MetaDataVerificationServiceSpec extends ERSTestHelper {

  val mockApplicationConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val mockRepositories: Repositories = mock[Repositories]
  val mockMetadataMongoRepository: MetaDataVerificationMongoRepository = mock[MetaDataVerificationMongoRepository]
  val mockLogger: Logger = mock[Logger]

  def createMetadataMongoRepository(aggregatedSubmissions: Seq[JsObject]): MetaDataVerificationService =
    new MetaDataVerificationService(mockApplicationConfig, mockRepositories) {
     override lazy val metaDataVerificationRepository: MetaDataVerificationMongoRepository = mockMetadataMongoRepository
     when(mockMetadataMongoRepository.getAggregateCountOfSubmissions)
       .thenReturn(Future(aggregatedSubmissions))
      override val logger: Logger = mockLogger
    }

  val validSipJsonObj: JsObject = Json
    .parse("""{"_id":{"schemeType":"SIP","transferStatus":"failedResubmission"},"count":2}""")
    .as[JsObject]
  val validEmiJsonObj: JsObject = Json
    .parse("""{"_id":{"schemeType":"EMI","transferStatus":"process"},"count":1}""")
    .as[JsObject]
  val invalidEmiJsonObj: JsObject = Json
    .parse("""{"_id":{"schemeType":"EMI","transferStatus":"process"}}""") // missing count
    .as[JsObject]

  "getAggregateMetadataMetrics" should {

    "map json valid json objects correctly to AggregatedLogs" in {
      val expectedAggregatedLogs = Seq(
        AggregatedLog(Id(schemeType = "SIP", transferStatus = "failedResubmission"), count = 2),
        AggregatedLog(Id(schemeType = "EMI", transferStatus = "process"), count = 1)
      )
      val metadataMongoRepository: MetaDataVerificationService = createMetadataMongoRepository(Seq(validSipJsonObj, validEmiJsonObj))
      metadataMongoRepository.getAggregateMetadataMetrics.map(
        _ should contain theSameElementsAs(expectedAggregatedLogs)
      )
    }

    "not return AgregatedLogs for invalid json" in {
      val expectedAggregatedLogs = Seq.empty[AggregatedLog]
      val metadataMongoRepository: MetaDataVerificationService = createMetadataMongoRepository(Seq(invalidEmiJsonObj))
      metadataMongoRepository.getAggregateMetadataMetrics.map(
        _ should contain theSameElementsAs(expectedAggregatedLogs)
      )
    }

  }

  "checkMetaDataHasPresubmissionFile" should {

  }

}
