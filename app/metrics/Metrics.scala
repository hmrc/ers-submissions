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

package metrics

import java.util.concurrent.TimeUnit

import javax.inject.Inject

class Metrics @Inject()() {
import com.codahale.metrics.SharedMetricRegistries

  def storePresubmission(diff: Long, unit: TimeUnit): Unit =
    SharedMetricRegistries.getOrCreate("store-presubmission").timer("store-presubmission").update(diff, unit)

  def failedStorePresubmission(): Unit = SharedMetricRegistries.getOrCreate("failed-store-presubmission").counter("failed-store-presubmission").inc()

  def removePresubmission(diff: Long, unit: TimeUnit): Unit =
    SharedMetricRegistries.getOrCreate("remove-presubmission").timer("remove-presubmission").update(diff, unit)

  def failedRemovePresubmission(): Unit = SharedMetricRegistries.getOrCreate("failed-remove-presubmission").counter("failed-remove-presubmission").inc()

  def generateJson(diff: Long, unit: TimeUnit): Unit = SharedMetricRegistries.getOrCreate("generate-json").timer("generate-json").update(diff, unit)

  def sendToADR(diff: Long, unit: TimeUnit): Unit = SharedMetricRegistries.getOrCreate("send-to-ADR").timer("send-to-ADR").update(diff, unit)

  def successfulSendToADR(): Unit = SharedMetricRegistries.getOrCreate("successful-send-to-ADR").counter("successful-send-to-ADR").inc()

  def failedSendToADR(): Unit = SharedMetricRegistries.getOrCreate("failed-send-to-ADR").counter("failed-send-to-ADR").inc()

  def updatePostsubmissionStatus(diff: Long, unit: TimeUnit): Unit =
    SharedMetricRegistries.getOrCreate("update-postsubmission-status").timer("update-postsubmission-status").update(diff, unit)

  def saveMetadata(diff: Long, unit: TimeUnit): Unit = SharedMetricRegistries.getOrCreate("save-metadata").timer("save-metadata").update(diff, unit)

  def checkForPresubmission(diff: Long, unit: TimeUnit): Unit =
    SharedMetricRegistries.getOrCreate("check-for-presubmission").timer("check-for-presubmission").update(diff, unit)

}
