/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.resubmission

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import scheduler.{PreSubWithoutMetadataQueryImpl, ResubmissionServiceImpl}

class SchedulerModuleITSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "schedules.resubmission-service.enabled"                    -> false,
      "schedules.generate-pre-sub-without-metadata-query.enabled" -> false,
      "auditing.enabled"                                          -> false
    )
    .build()

  "With SchedulerModule enabled" should {

    "construct ResubmissionServiceImpl" in {
      app.injector.instanceOf[ResubmissionServiceImpl].jobName shouldBe "resubmission-service"
    }

    "construct PreSubWithoutMetadataQueryImpl" in {
      app.injector.instanceOf[PreSubWithoutMetadataQueryImpl].jobName shouldBe
        "generate-pre-sub-without-metadata-query"
    }
  }

}
