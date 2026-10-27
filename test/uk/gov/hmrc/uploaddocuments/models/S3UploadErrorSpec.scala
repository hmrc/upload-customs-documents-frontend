/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.uploaddocuments.models

import uk.gov.hmrc.uploaddocuments.support.UnitSpec

class S3UploadErrorSpec extends UnitSpec {

  def error(code: String): S3UploadError = S3UploadError(key = "some-key", errorCode = code, errorMessage = "msg")

  "S3UploadError.isEmptyOrMissingFile" should {

    "be true when the S3 error code signals no usable file was submitted" in {
      error("EntityTooSmall").isEmptyOrMissingFile shouldBe true
      error("400").isEmptyOrMissingFile shouldBe true
      error("InvalidArgument").isEmptyOrMissingFile shouldBe true
    }

    "be false for errors that describe a real attempted file" in {
      error("EntityTooLarge").isEmptyOrMissingFile shouldBe false
      error("InternalError").isEmptyOrMissingFile shouldBe false
      error("NoSuchKey").isEmptyOrMissingFile shouldBe false
    }
  }
}
