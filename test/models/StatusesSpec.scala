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

package models

import org.scalatest.prop.TableDrivenPropertyChecks.*
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

class StatusesSpec extends AnyWordSpec with Matchers {

  "Statuses" should {

    val cases = Table(
      ("status", "expected"),
      (Statuses.Saved, "saved"),
      (Statuses.Sent, "sent"),
      (Statuses.Failed, "failed"),
      (Statuses.Process, "process")
    )

    "have the correct value" in
      forAll(cases) { (status, expected) =>
        status.value shouldBe expected
      }
  }

}
