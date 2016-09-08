/*
 * Copyright 2016 HM Revenue & Customs
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

import org.joda.time.DateTime
import play.api.libs.json.Json

/*
case class ReturnServiceCache(
                               schemeId: String,
                               aoRef: Option[String],
                               taxYear: Option[String],
                               schemeRef: Option[String],
                               schemeName: Option[String],
                               schemeType: Option[String],
                               schemeConcat: Option[String],
                               checkFileType: Option[String],
                               errorCount: Option[String],
                               fileName: Option[String],
                               errorList: Option[String],
                               errorSummary: Option[String],
                               chooseActivity: Option[String],
                               trustees: Option[String],
                               altActivity: Option[String],
                               errorReportDateTime: Option[String],
                               confirmationDateTime: Option[String],
                               empRef: Option[String],
                               agentRef: Option[String],
                               sapNumber: Option[String],
                               uploadTimestamp: Option[DateTime]
                               )
object ReturnServiceCache {
  implicit val jsonFormat = Json.format[ReturnServiceCache]
}

case class AltAmendsCache(
                           altAmends: Option[String],
                           altAmendsTerms: Option[String],
                           altAmendsEligibility: Option[String],
                           altAmendsExchange: Option[String],
                           altAmendsVariations: Option[String],
                           altAmendsOther: Option[String]
                           )
object AltAmendsCache {
  implicit val jsonFormat = Json.format[AltAmendsCache]
}

case class GroupSchemeCache(
                             groupScheme: Option[String],
                             groupSchemeType: Option[String]
                             )
object GroupSchemeCache {
  implicit val jsonFormat = Json.format[GroupSchemeCache]
}

case class RS_schemeOrganiserDetails(
                                      companyName: String,
                                      addressLine1: String,
                                      addressLine2: Option[String],
                                      addressLine3: Option[String],
                                      addressLine4: Option[String],
                                      postcode: Option[String],
                                      country: Option[String],
                                      companyReg: Option[String],
                                      corporationRef: Option[String]
                                      )
object RS_schemeOrganiserDetails {
  implicit val jsonFormat = Json.format[RS_schemeOrganiserDetails]
}

case class RS_companyDetails(
                              companyName: String,
                              addressLine1: String,
                              addressLine2: Option[String],
                              addressLine3: Option[String],
                              addressLine4: Option[String],
                              postcode: Option[String],
                              country: Option[String],
                              companyReg: Option[String],
                              corporationRef: Option[String]
                              )
object RS_companyDetails {
  implicit val jsonFormat = Json.format[RS_companyDetails]
}

case class RS_companyDetailsList(
                                  companies: List[RS_companyDetails]
                                  )
object RS_companyDetailsList {
  implicit val jsonFormat = Json.format[RS_companyDetailsList]
}


case class RS_trusteeDetails(
                              name: String,
                              addressLine1: String,
                              addressLine2: Option[String],
                              addressLine3: Option[String],
                              addressLine4: Option[String],
                              postcode: Option[String],
                              country: Option[String]
                              )
object RS_trusteeDetails {
  implicit val jsonFormat = Json.format[RS_trusteeDetails]
}

case class RS_trusteeDetailsList(
                                  trustees: List[RS_trusteeDetails]
                                  )
object RS_trusteeDetailsList {
  implicit val jsonFormat = Json.format[RS_trusteeDetailsList]
}

case class FileTransferCache(
                              mark: Option[String],
                              oid: Option[String],
                              timestamp: Option[String],
                              filename: Option[String],
                              objectID: Option[String]
                              )
object FileTransferCache {
  implicit val jsonFormat = Json.format[FileTransferCache]
}

case class FileTransferCaches(
                               filestransfers: List[FileTransferCache]
                               )
object FileTransferCaches {
  implicit val jsonFormat = Json.format[FileTransferCaches]
}

case class CombinedAll(
                        bundleRef: String,
                        ipRef: String,
                        returnService: ReturnServiceCache,
                        altAmendsService: Option[AltAmendsCache],
                        groupService: Option[GroupSchemeCache],
                        schemeOrganiser: Option[RS_schemeOrganiserDetails],
                        companies: Option[RS_companyDetailsList],
                        trustees: Option[RS_trusteeDetailsList],
                        fileTransferData: Option[FileTransferCache],
                        fileTransferList: Option[FileTransferCaches],
                        sessionId: String
                        )

object CombinedAll {
  implicit val jsonFormat = Json.format[CombinedAll]
}
*/

case class ErsMetaData(
                        schemeInfo: SchemeInfo,
                        ipRef: String,
                        aoRef: Option[String],
                        empRef: String,
                        agentRef: Option[String],
                        sapNumber: Option[String]
                        )

object ErsMetaData {
  implicit val format = Json.format[ErsMetaData]
}

case class AlterationAmends(
                             altAmendsTerms: Option[String],
                             altAmendsEligibility: Option[String],
                             altAmendsExchange: Option[String],
                             altAmendsVariations: Option[String],
                             altAmendsOther: Option[String]
                             )

object AlterationAmends {
  implicit val format = Json.format[AlterationAmends]
}

case class GroupSchemeInfo(
                            groupScheme: Option[String],
                            groupSchemeType: Option[String]

                            )
object GroupSchemeInfo {
  implicit val format = Json.format[GroupSchemeInfo]
}

case class SchemeOrganiserDetails(
                                   companyName: String,
                                   addressLine1: String,
                                   addressLine2: Option[String],
                                   addressLine3: Option[String],
                                   addressLine4: Option[String],
                                   country: Option[String],
                                   postcode: Option[String],
                                   companyReg: Option[String],
                                   corporationRef: Option[String]
                                   )
object SchemeOrganiserDetails {
  implicit val format = Json.format[SchemeOrganiserDetails]
}

case class CompanyDetails(
                           companyName: String,
                           addressLine1: String,
                           addressLine2: Option[String],
                           addressLine3: Option[String],
                           addressLine4: Option[String],
                           postcode: Option[String],
                           country: Option[String],
                           companyReg: Option[String],
                           corporationRef: Option[String]
                           )
object CompanyDetails {
  implicit val format = Json.format[CompanyDetails]
}
case class CompanyDetailsList(
                               companies: List[CompanyDetails]
                               )
object CompanyDetailsList {
  implicit val format = Json.format[CompanyDetailsList]
}

case class TrusteeDetails(
                           name: String,
                           addressLine1: String,
                           addressLine2: Option[String],
                           addressLine3: Option[String],
                           addressLine4: Option[String],
                           postcode: Option[String],
                           country: Option[String]
                           )
object TrusteeDetails {
  implicit val format = Json.format[TrusteeDetails]
}


case class TrusteeDetailsList(
                               trustees: List[TrusteeDetails]
                               )
object TrusteeDetailsList {
  implicit val format = Json.format[TrusteeDetailsList]
}

case class AltAmendsActivity(altActivity: String)
object AltAmendsActivity {
  implicit val format = Json.format[AltAmendsActivity]
}

case class ErsSummary(
                       bundleRef: String,
                       isNilReturn: String,
                       fileType: Option[String],
                       confirmationDateTime: DateTime,
                       metaData: ErsMetaData,
                       altAmendsActivity: Option[AltAmendsActivity],
                       alterationAmends: Option[AlterationAmends],
                       groupService: Option[GroupSchemeInfo],
                       schemeOrganiser: Option[SchemeOrganiserDetails],
                       companies: Option[CompanyDetailsList],
                       trustees: Option[TrusteeDetailsList],
                       nofOfRows: Option[Int],
                       transferStatus: Option[String]
                       )
object ErsSummary {
  implicit val format = Json.format[ErsSummary]
}
