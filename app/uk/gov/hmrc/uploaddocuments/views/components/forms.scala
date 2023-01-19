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

package uk.gov.hmrc.uploaddocuments.views.components

import play.api.i18n.Messages
import play.api.mvc.Request
import play.twirl.api.Html

import javax.inject.{Inject, Singleton}

@Singleton
class forms @Inject() (
  _formWithCSRF: uk.gov.hmrc.govukfrontend.views.html.components.FormWithCSRF,
  val fieldset: uk.gov.hmrc.uploaddocuments.views.html.components.fieldset,
  val errorSummary: uk.gov.hmrc.uploaddocuments.views.html.components.errorSummary,
  val inputText: uk.gov.hmrc.uploaddocuments.views.html.components.inputText,
  val inputNumber: uk.gov.hmrc.uploaddocuments.views.html.components.inputNumber,
  val inputHidden: uk.gov.hmrc.uploaddocuments.views.html.components.inputHidden,
  val inputDate: uk.gov.hmrc.uploaddocuments.views.html.components.inputDate,
  val inputCheckboxes: uk.gov.hmrc.uploaddocuments.views.html.components.inputCheckboxes,
  val inputRadio: uk.gov.hmrc.uploaddocuments.views.html.components.inputRadio,
  val textarea: uk.gov.hmrc.uploaddocuments.views.html.components.textarea,
  val inputCharacterCount: uk.gov.hmrc.uploaddocuments.views.html.components.InputCharacterCount
) {

  def formWithCSRF(action: play.api.mvc.Call, args: (Symbol, String)*)(body: => Html)(implicit
    request: Request[_],
    messages: Messages
  ) =
    _formWithCSRF.apply(
      action,
      (Seq(
        Symbol("id")         -> "upload-documents",
        Symbol("novalidate") -> "novalidate"
      ) ++ args): _*
    )(body)
}
