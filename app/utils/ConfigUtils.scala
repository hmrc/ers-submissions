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

package utils

import com.typesafe.config.{Config, ConfigFactory}
import models.ErsSummary
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggingAndRexceptions.{ADRExceptionEmitter, ErsLogger}

import javax.inject.Inject

class ConfigUtils @Inject()(adrExceptionEmitter: ADRExceptionEmitter) extends ErsLogger {

  def getConfigData(configPath: String, configValue: String, ersSummary: ErsSummary)(implicit hc: HeaderCarrier): Config = {
    try {
      ConfigFactory.load(s"schemes/$configPath").getConfig(configValue)
    }
    catch {
      case ex: Exception =>
        val data = Map(
          "message" -> s"Trying to load invalid configuration. Path: $configPath, value: $configValue",
          "context" -> "ConfigUtils.getConfigData"
        )
        logException(ersSummary.metaData.schemeInfo, ex, Some(buildEmiterMessage(data)))
        adrExceptionEmitter.auditAndThrowWithStackTrace(ersSummary.metaData, data, ex)
    }
  }

  def getClearData(data: Object): Object = {
    data match {
      case Some(innerData: Object) => innerData
      case _ => data
    }
  }

  def extractField(configData: Config, data: Object): Object = {
    if(!configData.hasPath("extract") || data.isInstanceOf[Option[_]]) {
      data
    } else {
      val currentElem: Config = configData.getConfig("extract")
      val field = data.getClass.getDeclaredField(currentElem.getString("name"))
      field.setAccessible(true)
      val result: Object = getClearData(field.get(data))
      extractField(currentElem, result)
    }
  }
}
