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

package uk.gov.hmrc.uploaddocuments.views

import play.api.data.Form
import play.api.i18n.Messages
import uk.gov.hmrc.uploaddocuments.models.CustomizedServiceContent

object ViewHelpers {

  /** Key of the prefix to the page's title when the form has errors. */
  val errorBrowserTitlePrefixKey = "error.browser.title.prefix"

  def pageTitle(
    key: String,
    args: Any*
  )(implicit messages: Messages, content: CustomizedServiceContent): Option[String] =
    content.title.orElse(Some(messages(key, args: _*)))

  def pageTitleWithErrors(
    key: String,
    hasErrors: Boolean,
    args: Any*
  )(implicit messages: Messages, content: CustomizedServiceContent): Option[String] =
    pageTitle(key, args: _*).map { title =>
      if (hasErrors) messages(errorBrowserTitlePrefixKey) + " " + title
      else title
    }

  def pageWithFormTitle(
    key: String,
    form: Form[_],
    args: Any*
  )(implicit messages: Messages, content: CustomizedServiceContent): Option[String] =
    pageTitleWithErrors(key, form.hasErrors, args: _*)

  def pageHeading(
    key: String
  )(implicit messages: Messages, content: CustomizedServiceContent): String =
    content.title.getOrElse(messages(key))

}
