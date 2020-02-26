/*
 * Copyright 2020 HM Revenue & Customs
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

trait Metrics {

  def storePresubmission(diff: Long, unit: TimeUnit): Unit
  def failedStorePresubmission(): Unit

  def removePresubmission(diff: Long, unit: TimeUnit): Unit
  def failedRemovePresubmission(): Unit

  def generateJson(diff: Long, unit: TimeUnit): Unit

  def sendToADR(diff: Long, unit: TimeUnit): Unit
  def successfulSendToADR(): Unit
  def failedSendToADR(): Unit

  def updatePostsubmissionStatus(diff: Long, unit: TimeUnit): Unit

  def saveMetadata(diff: Long, unit: TimeUnit): Unit

  def checkForPresubmission(diff: Long, unit: TimeUnit): Unit

}

object Metrics extends Metrics {
import com.codahale.metrics._
  override def storePresubmission(diff: Long, unit: TimeUnit) = SharedMetricRegistries.getOrCreate("store-presubmission").timer("store-presubmission").update(diff, unit)
  override def failedStorePresubmission(): Unit = SharedMetricRegistries.getOrCreate("failed-store-presubmission").counter("failed-store-presubmission").inc()

  override def removePresubmission(diff: Long, unit: TimeUnit) = SharedMetricRegistries.getOrCreate("remove-presubmission").timer("remove-presubmission").update(diff, unit)
  override def failedRemovePresubmission(): Unit = SharedMetricRegistries.getOrCreate("failed-remove-presubmission").counter("failed-remove-presubmission").inc()

  override def generateJson(diff: Long, unit: TimeUnit) = SharedMetricRegistries.getOrCreate("generate-json").timer("generate-json").update(diff, unit)

  override def sendToADR(diff: Long, unit: TimeUnit) = SharedMetricRegistries.getOrCreate("send-to-ADR").timer("send-to-ADR").update(diff, unit)
  override def successfulSendToADR(): Unit = SharedMetricRegistries.getOrCreate("successful-send-to-ADR").counter("successful-send-to-ADR").inc()
  override def failedSendToADR(): Unit = SharedMetricRegistries.getOrCreate("failed-send-to-ADR").counter("failed-send-to-ADR").inc()

  override def updatePostsubmissionStatus(diff: Long, unit: TimeUnit) = SharedMetricRegistries.getOrCreate("update-postsubmission-status").timer("update-postsubmission-status").update(diff, unit)

  override def saveMetadata(diff: Long, unit: TimeUnit) = SharedMetricRegistries.getOrCreate("save-metadata").timer("save-metadata").update(diff, unit)

  override def checkForPresubmission(diff: Long, unit: TimeUnit) =  SharedMetricRegistries.getOrCreate("check-for-presubmission").timer("check-for-presubmission").update(diff, unit)

}
