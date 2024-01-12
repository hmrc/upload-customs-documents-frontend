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

package uk.gov.hmrc.uploaddocuments.views

import uk.gov.hmrc.uploaddocuments.support.UnitSpec

class UploadFileViewHelperSpec extends UnitSpec {

  " UploadFileViewHelper" should {
    "format bytes count to human readable form" in {
      UploadFileViewHelper.humanReadableFileSize(1) shouldBe "1 B"
      UploadFileViewHelper.humanReadableFileSize(2) shouldBe "2 B"
      UploadFileViewHelper.humanReadableFileSize(78) shouldBe "78 B"
      UploadFileViewHelper.humanReadableFileSize(256) shouldBe "256 B"
      UploadFileViewHelper.humanReadableFileSize(999) shouldBe "999 B"
      UploadFileViewHelper.humanReadableFileSize(1000) shouldBe "1 kB"
      UploadFileViewHelper.humanReadableFileSize(1256) shouldBe "1 kB"
      UploadFileViewHelper.humanReadableFileSize(1999) shouldBe "1 kB"
      UploadFileViewHelper.humanReadableFileSize(3256) shouldBe "3 kB"
      UploadFileViewHelper.humanReadableFileSize(9256) shouldBe "9 kB"
      UploadFileViewHelper.humanReadableFileSize(999999) shouldBe "999 kB"
      UploadFileViewHelper.humanReadableFileSize(1000000) shouldBe "1 MB"
      UploadFileViewHelper.humanReadableFileSize(1256788) shouldBe "1 MB"
      UploadFileViewHelper.humanReadableFileSize(3256788) shouldBe "3 MB"
      UploadFileViewHelper.humanReadableFileSize(9256788) shouldBe "9 MB"
      UploadFileViewHelper.humanReadableFileSize(123256788) shouldBe "123 MB"
      UploadFileViewHelper.humanReadableFileSize(999999999L) shouldBe "999 MB"
      UploadFileViewHelper.humanReadableFileSize(1000000000L) shouldBe "1 GB"
      UploadFileViewHelper.humanReadableFileSize(7123256788L) shouldBe "7 GB"
    }
  }
}
