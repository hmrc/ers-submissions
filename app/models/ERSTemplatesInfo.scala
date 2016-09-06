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

case class SheetInfo (schemeType:String, sheetId: Int, sheetName:String)

trait EMITemplateInfo {

  val headerFormat = "[^a-zA-Z0-9]"

  val emi="EMI"
  val emiSheet1Name = "EMI40_Adjustments_V3"
  val emiSheet2Name = "EMI40_Replaced_V3"
  val emiSheet3Name = "EMI40_RLC_V3"
  val emiSheet4Name = "EMI40_NonTaxable_V3"
  val emiSheet5Name = "EMI40_Taxable_V3"

}

trait OtherTemplateInfo {

  val otherHeaderFormat = "[^a-zA-Z0-9]"

  val other = "OTHER"

  val otherSheet1Name = "Other_Grants_V3"
  val otherSheet2Name = "Other_Options_V3"
  val otherSheet3Name = "Other_Acquisition_V3"
  val otherSheet4Name = "Other_RestrictedSecurities_V3"
  val otherSheet5Name = "Other_OtherBenefits_V3"
  val otherSheet6Name = "Other_Convertible_V3"
  val otherSheet7Name = "Other_Notional_V3"
  val otherSheet8Name = "Other_Enhancement_V3"
  val otherSheet9Name = "Other_Sold_V3"

}


trait CsopTemplateInfo {
  val csop = "CSOP"

  val csopSheet1Name = "CSOP_OptionsGranted_V3"
  val csopSheet2Name = "CSOP_OptionsRCL_V3"
  val csopSheet3Name = "CSOP_OptionsExercised_V3"

}

object ERSTemplatesInfo extends EMITemplateInfo with OtherTemplateInfo with CsopTemplateInfo { // with SipTemplateInfo with SayeTemplateInfo{

  val ersSheets = Map(
    csop -> List(
      SheetInfo(csop, 1, csopSheet1Name),
      SheetInfo(csop,2,csopSheet2Name),
      SheetInfo(csop,3, csopSheet3Name)
    ),
    emi -> List(
      SheetInfo(emi, 1,emiSheet1Name),
      SheetInfo(emi, 2,emiSheet2Name),
      SheetInfo(emi, 3, emiSheet3Name),
      SheetInfo(emi, 4, emiSheet4Name),
      SheetInfo(emi, 5, emiSheet5Name)
    ),
    other -> List(
      SheetInfo(other, 1, otherSheet1Name),
      SheetInfo(other, 2, otherSheet2Name),
      SheetInfo(other, 3, otherSheet3Name),
      SheetInfo(other, 4, otherSheet4Name),
      SheetInfo(other, 5, otherSheet5Name),
      SheetInfo(other, 6, otherSheet6Name),
      SheetInfo(other, 7, otherSheet7Name),
      SheetInfo(other, 8, otherSheet8Name),
      SheetInfo(other, 9, otherSheet9Name)
    ))
}
