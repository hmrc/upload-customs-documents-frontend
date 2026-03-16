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

import play.api.i18n.{DefaultMessagesApi, Lang, MessagesImpl}
import uk.gov.hmrc.uploaddocuments.models.*
import uk.gov.hmrc.uploaddocuments.support.UnitSpec

class UploadFileViewHelperSpec extends UnitSpec {

  implicit val messages: MessagesImpl = MessagesImpl(
    Lang("en"),
    new DefaultMessagesApi(
      messages = Map(
        "en" -> Map(
          "error.file-upload.invalid-type"      -> "The selected file must be {1}",
          "error.file-upload.invalid-extension" -> "The selected file must have an extension of {1}",
          "error.file-upload.quarantine"        -> "The selected file contains a virus",
          "error.file-upload.unknown"           -> "The selected file could not be uploaded"
        )
      )
    )
  )

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

  "UploadFileViewHelper.toMessageKey(FailureDetails)" should {

    "return 'error.file-upload.invalid-extension' when reason is REJECTED and message starts with INVALID_EXTENSION:" in {
      val details = UpscanNotification.FailureDetails(UpscanNotification.REJECTED, "INVALID_EXTENSION:.pdf,.xlsx")
      UploadFileViewHelper.toMessageKey(details) shouldBe "error.file-upload.invalid-extension"
    }

    "return 'error.file-upload.invalid-type' when reason is REJECTED and message does not start with INVALID_EXTENSION:" in {
      val details = UpscanNotification.FailureDetails(UpscanNotification.REJECTED, "MIME type application/exe is not allowed")
      UploadFileViewHelper.toMessageKey(details) shouldBe "error.file-upload.invalid-type"
    }

    "return 'error.file-upload.quarantine' when reason is QUARANTINE" in {
      val details = UpscanNotification.FailureDetails(UpscanNotification.QUARANTINE, "Virus detected")
      UploadFileViewHelper.toMessageKey(details) shouldBe "error.file-upload.quarantine"
    }

    "return 'error.file-upload.unknown' when reason is UNKNOWN" in {
      val details = UpscanNotification.FailureDetails(UpscanNotification.UNKNOWN, "Something went wrong")
      UploadFileViewHelper.toMessageKey(details) shouldBe "error.file-upload.unknown"
    }
  }

  "UploadFileViewHelper.toMessage" should {

    "use the raw allowedFileExtensions (from FailureDetails message) for INVALID_EXTENSION errors, not allowedFileTypesHint" in {
      val allowedExts     = ".pdf,.png,.xlsx"
      val humanReadyHint  = "Excel, PNG or PDF"
      val error = FileVerificationFailed(
        UpscanNotification.FailureDetails(UpscanNotification.REJECTED, s"INVALID_EXTENSION:$allowedExts")
      )
      val result = UploadFileViewHelper.toMessage(error, 10000000L, humanReadyHint)
      result shouldBe s"The selected file must have an extension of $allowedExts"
    }

    "use allowedFileTypesHint for regular REJECTED (MIME type) errors" in {
      val humanReadyHint = "a PDF or PNG"
      val error = FileVerificationFailed(
        UpscanNotification.FailureDetails(UpscanNotification.REJECTED, "MIME type application/exe is not allowed")
      )
      val result = UploadFileViewHelper.toMessage(error, 10000000L, humanReadyHint)
      result shouldBe s"The selected file must be $humanReadyHint"
    }
  }
}
