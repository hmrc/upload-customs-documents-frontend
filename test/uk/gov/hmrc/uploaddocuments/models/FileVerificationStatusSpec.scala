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

package uk.gov.hmrc.uploaddocuments.models

import uk.gov.hmrc.uploaddocuments.support.UnitSpec
import uk.gov.hmrc.uploaddocuments.support.JsonFormatTest

class FileVerificationStatusSpec extends UnitSpec {

  "FileVerificationStatus format" should {

    "serialize and deserialize FileVerificationStatus" in new JsonFormatTest[FileVerificationStatus](info) {
      validateJsonFormat(
        """{"reference":"ref","fileStatus":"NOT_UPLOADED","uploadRequest":{"href":"https://s3.amazonaws.com/bucket/123abc","fields":{"foo1":"bar1"}}}""".stripMargin,
        FileVerificationStatus(
          reference = "ref",
          fileStatus = "NOT_UPLOADED",
          uploadRequest =
            Some(UploadRequest(href = "https://s3.amazonaws.com/bucket/123abc", fields = Map("foo1" -> "bar1")))
        )
      )
    }

  }
}
