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
import uk.gov.hmrc.uploaddocuments.support.JsonFormatExplicitTest
import play.api.libs.json.Format
import play.api.libs.json.JsError

class FileUploadErrorSpec extends UnitSpec {

  "FileUploadError" should {

    "apply to FileUpload.Failed" in {
      val fileUpload = FileUpload.Failed(
        Nonce.Any,
        Timestamp.Any,
        "foo",
        UpscanNotification
          .FailureDetails(
            failureReason = UpscanNotification.QUARANTINE,
            message = "This file has virus."
          )
      )
      FileUploadError(fileUpload) shouldBe FileVerificationFailed(
        details = UpscanNotification
          .FailureDetails(
            failureReason = UpscanNotification.QUARANTINE,
            message = "This file has virus."
          )
      )
    }

    "apply to FileUpload.Rejected" in {
      val fileUpload = FileUpload.Rejected(Nonce.Any, Timestamp.Any, "foo", S3UploadError("a", "b", "c"))
      FileUploadError(fileUpload) shouldBe FileTransmissionFailed(error = S3UploadError("a", "b", "c"))
    }

    "apply to FileUpload.Duplicate" in {
      val fileUpload = FileUpload.Duplicate(
        nonce = Nonce.Any,
        timestamp = Timestamp.Any,
        reference = "ref",
        checksum = "foo",
        existingFileName = "baz",
        duplicateFileName = "qux"
      )
      FileUploadError(fileUpload) shouldBe DuplicateFileUpload(
        checksum = "foo",
        existingFileName = "baz",
        duplicateFileName = "qux"
      )
    }

    "serialize and deserialize FileTransmissionFailed" in new JsonFormatExplicitTest[FileUploadError](info) {
      implicit val testedFormat: Format[FileUploadError] = FileUploadError.format
      validateJsonFormat(
        """{"FileTransmissionFailed":{"error":{"key":"a","errorCode":"b","errorMessage":"c","errorRequestId":"e","errorResource":"d"}}}""".stripMargin,
        FileTransmissionFailed(error = S3UploadError("a", "b", "c", Some("e"), Some("d")))
      )
    }

    "serialize and deserialize FileVerificationFailed" in new JsonFormatExplicitTest[FileUploadError](info) {
      implicit val testedFormat: Format[FileUploadError] = FileUploadError.format
      validateJsonFormat(
        """{"FileVerificationFailed":{"details":{"failureReason":"QUARANTINE","message":"This file has virus."}}}""".stripMargin,
        FileVerificationFailed(details =
          UpscanNotification.FailureDetails(UpscanNotification.QUARANTINE, "This file has virus.")
        )
      )
    }

    "have formats for all FileUploadError subclasses" in {
      FileUploadError.formats.size shouldBe 3
      FileUploadError.formats.map(_.key) should contain theSameElementsAs Seq(
        "FileTransmissionFailed",
        "FileVerificationFailed",
        "DuplicateFileUpload"
      )
      FileUploadError.formats.foreach { format =>
        (try
          format
            .reads(format.writes(FileTransmissionFailed(error = S3UploadError("a", "b", "c", Some("e"), Some("d")))))
        catch {
          case e: Exception => JsError(e.getMessage)
        })
          .orElse(
            try
              format.reads(
                format.writes(
                  FileVerificationFailed(details =
                    UpscanNotification.FailureDetails(UpscanNotification.QUARANTINE, "This file has virus.")
                  )
                )
              )
            catch {
              case e: Exception => JsError(e.getMessage)
            }
          )
          .orElse(
            try
              format.reads(
                format.writes(
                  DuplicateFileUpload(checksum = "foo", existingFileName = "baz", duplicateFileName = "qux")
                )
              )
            catch {
              case e: Exception => JsError(e.getMessage)
            }
          )
          .isSuccess shouldBe true
      }
    }
  }
}
