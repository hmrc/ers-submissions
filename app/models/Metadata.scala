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

package models

import play.api.libs.json._

import java.time.Instant

case class ErsMetaData(schemeInfo: SchemeInfo,
                       ipRef: String,
                       aoRef: Option[String],
                       empRef: String,
                       agentRef: Option[String],
                       sapNumber: Option[String])
object ErsMetaData {
  implicit val format: OFormat[ErsMetaData] = Json.format[ErsMetaData]
}

case class AlterationAmends(altAmendsTerms: Option[String],
                            altAmendsEligibility: Option[String],
                            altAmendsExchange: Option[String],
                            altAmendsVariations: Option[String],
                            altAmendsOther: Option[String])
object AlterationAmends {
  implicit val format: OFormat[AlterationAmends] = Json.format[AlterationAmends]
}

case class GroupSchemeInfo(groupScheme: Option[String], groupSchemeType: Option[String])
object GroupSchemeInfo {
  implicit val format: OFormat[GroupSchemeInfo] = Json.format[GroupSchemeInfo]
}

case class SchemeOrganiserDetails(companyName: String,
                                  addressLine1: String,
                                  addressLine2: Option[String],
                                  addressLine3: Option[String],
                                  addressLine4: Option[String],
                                  country: Option[String],
                                  postcode: Option[String],
                                  companyReg: Option[String],
                                  corporationRef: Option[String])
object SchemeOrganiserDetails {
  implicit val format: OFormat[SchemeOrganiserDetails] = Json.format[SchemeOrganiserDetails]
}

case class CompanyDetails(companyName: String,
                          addressLine1: String,
                          addressLine2: Option[String],
                          addressLine3: Option[String],
                          addressLine4: Option[String],
                          postcode: Option[String],
                          country: Option[String],
                          companyReg: Option[String],
                          corporationRef: Option[String])
object CompanyDetails {
  implicit val format: OFormat[CompanyDetails] = Json.format[CompanyDetails]
}

case class CompanyDetailsList(companies: List[CompanyDetails])
object CompanyDetailsList {
  implicit val format: OFormat[CompanyDetailsList] = Json.format[CompanyDetailsList]
}

case class TrusteeDetails(name: String,
                          addressLine1: String,
                          addressLine2: Option[String],
                          addressLine3: Option[String],
                          addressLine4: Option[String],
                          postcode: Option[String],
                          country: Option[String])
object TrusteeDetails {
  implicit val format: OFormat[TrusteeDetails] = Json.format[TrusteeDetails]
}

case class TrusteeDetailsList(trustees: List[TrusteeDetails])
object TrusteeDetailsList {
  implicit val format: OFormat[TrusteeDetailsList] = Json.format[TrusteeDetailsList]
}

case class AltAmendsActivity(altActivity: String)
object AltAmendsActivity {
  implicit val format: OFormat[AltAmendsActivity] = Json.format[AltAmendsActivity]
}

case class ErsSummary(bundleRef: String,
                      isNilReturn: String,
                      fileType: Option[String],
                      confirmationDateTime: Instant,
                      metaData: ErsMetaData,
                      altAmendsActivity: Option[AltAmendsActivity],
                      alterationAmends: Option[AlterationAmends],
                      groupService: Option[GroupSchemeInfo],
                      schemeOrganiser: Option[SchemeOrganiserDetails],
                      companies: Option[CompanyDetailsList],
                      trustees: Option[TrusteeDetailsList],
                      nofOfRows: Option[Int],
                      transferStatus: Option[String]) {

  val basicLogMessage: String = List(s"bundleRef $bundleRef", s"filetype $fileType", s"nofOfRows $nofOfRows", transferStatus).mkString("[",",","]")
}
object ErsSummary {
  import models.DateTime._

  implicit val format: OFormat[ErsSummary] = Json.format[ErsSummary]
}

case class ErsSchemeMetaData(metaData: ErsMetaDataDetails)

object ErsSchemeMetaData {
  implicit val format: OFormat[ErsSchemeMetaData] = Json.format[ErsSchemeMetaData]
}


case class ErsMetaDataDetails(schemeInfo: SubSchemeInfo)
object ErsMetaDataDetails {
  implicit val format: OFormat[ErsMetaDataDetails] = Json.format[ErsMetaDataDetails]
}
