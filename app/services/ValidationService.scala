/*
 * Copyright 2021 HM Revenue & Customs
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

import javax.inject.Inject
import models.{SchemeData, SchemeInfo, SubmissionsSchemeData}
import play.api.libs.json.{JsError, JsObject, JsSuccess}

class ValidationService @Inject()() {

  def validateSchemeData(json: JsObject): Option[SchemeData] = {
    json.validate[SchemeData] match {
      case schemeData: JsSuccess[SchemeData] => Some(schemeData.value)
      case _: JsError => {
        None
      }
    }
  }

  def validateSubmissionsSchemeData(json: JsObject): Option[SubmissionsSchemeData] = {
    json.validate[SubmissionsSchemeData] match {
      case schemeData: JsSuccess[SubmissionsSchemeData] => Some(schemeData.value)
      case _: JsError => None
    }
  }

  def validateSchemeInfo(json: JsObject): Option[SchemeInfo] = {
    json.validate[SchemeInfo] match {
      case schemeInfo: JsSuccess[SchemeInfo] => Some(schemeInfo.value)
      case _: JsError => None
    }
  }

}
