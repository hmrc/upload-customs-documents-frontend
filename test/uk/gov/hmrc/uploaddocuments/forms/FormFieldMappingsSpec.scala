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

package uk.gov.hmrc.uploaddocuments.forms

import play.api.data.FormError
import uk.gov.hmrc.uploaddocuments.support.{FormMappingMatchers, UnitSpec}

class FormFieldMappingsSpec extends UnitSpec with FormMappingMatchers {

  "FormFieldMappings" should {

    "for YesNoForm" should {

      lazy val booleanMapping = FormFieldMappings.booleanMapping("field", "yes", "no")

      "error when no boolean answer supplied" in {
        booleanMapping.bind(Map("" -> "")) shouldBe Left(List(FormError("", List("error.field.required"), List())))
      }

      "error when not valid boolean answer supplied" in {
        booleanMapping.bind(Map("" -> "notValid")) shouldBe Left(List(FormError("", List("error.field.required"), List())))
      }

      "true when trueValue" in {
        booleanMapping.bind(Map("" -> "yes")) shouldBe Right(true)
      }

      "false when falseValue" in {
        booleanMapping.bind(Map("" -> "no")) shouldBe Right(false)
      }
    }
  }
}
