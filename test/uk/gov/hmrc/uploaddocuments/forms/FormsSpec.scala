/*
 * Copyright 2024 HM Revenue & Customs
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

import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.support.{FormValidator, UnitSpec}

class FormsSpec extends UnitSpec with FormValidator {

  "UploadAnotherFileChoiceForm" should {
    "bind uploadAnotherFile choice and fill it back" in {
      val form = Forms.YesNoChoiceForm
      validate(form, Map("choice" -> "yes"), true)
      validate(form, Map("choice" -> "no"), false)
      validate(form, "choice", Map(), Seq("error.choice.required"))
      validate(form, "choice", Map("choice" -> "foo"), Seq("error.choice.required"))
    }
  }

  "UpscanUploadSuccessForm" should {
    "bind S3 success query parameters" in {
      val form = Forms.UpscanUploadSuccessForm
      validate(
        form,
        Map("key" -> "ABC-123", "bucket" -> "foo-bar-bucket"),
        S3UploadSuccess(key = "ABC-123", bucket = Some("foo-bar-bucket"))
      )
      validate(
        form,
        Map("key" -> "ABC-123"),
        S3UploadSuccess(key = "ABC-123", bucket = None)
      )
      validateErrors(form, Map(), Seq("key" -> "error.required"))
    }
  }

  "UpscanUploadErrorForm" should {
    "bind S3 error query parameters" in {
      val form = Forms.UpscanUploadErrorForm
      validate(
        form,
        Map(
          "key"            -> "ABC-123",
          "errorCode"      -> "code-001",
          "errorMessage"   -> "Strange file, is it?",
          "errorRequestId" -> "0123456789",
          "errorResource"  -> "/foo"
        ),
        S3UploadError(
          key = "ABC-123",
          errorCode = "code-001",
          errorMessage = "Strange file, is it?",
          errorRequestId = Some("0123456789"),
          errorResource = Some("/foo")
        )
      )
      validateErrors(
        form,
        Map(),
        Seq("key" -> "error.required")
      )
    }
  }
}
