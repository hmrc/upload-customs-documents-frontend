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

import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText, optional, text}
import uk.gov.hmrc.uploaddocuments.models.{S3UploadError, S3UploadSuccess}

object Forms {

  import FormFieldMappings._

  val YesNoChoiceForm = Form[Boolean](
    mapping("choice" -> yesNoMapping)(identity)(Option.apply)
  )

  val UpscanUploadSuccessForm = Form[S3UploadSuccess](
    mapping(
      "key"    -> nonEmptyText,
      "bucket" -> optional(nonEmptyText)
    )(S3UploadSuccess.apply)(S3UploadSuccess.unapply)
  )

  val UpscanUploadErrorForm = Form[S3UploadError](
    mapping(
      "key"            -> nonEmptyText,
      "errorCode"      -> optional(text),
      "errorMessage"   -> optional(text),
      "errorRequestId" -> optional(text),
      "errorResource"  -> optional(text)
    )(S3UploadError.from)(S3UploadError.unapplyOptional)
  )

}
