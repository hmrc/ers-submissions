/*
 * Copyright 2017 HM Revenue & Customs
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
import models.ERSQuery

trait DataVerificationConfig {

  lazy val isErsQueryEnabled: Boolean = ApplicationConfig.isErsQueryEnabled
  lazy val ersQuerySchemeType: String = ApplicationConfig.ersQuerySchemeType
  lazy val ersQueryStartDate: String = ApplicationConfig.ersQueryStartDate
  lazy val ersQueryEndDate: String = ApplicationConfig.ersQueryEndDate
  lazy val schedulerSchemeRefList: List[String] = ApplicationConfig.schedulerSchemeRefList

  def ersQuery: ERSQuery = {
    ERSQuery(Some(ersQuerySchemeType),Some(ersQueryStartDate),Some(ersQueryEndDate),None,schedulerSchemeRefList)
  }
}
