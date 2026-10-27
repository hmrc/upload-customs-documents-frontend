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

import java.time.ZonedDateTime

class FileUploadContextSpec extends UnitSpec {

  private def context(min: Int, continueWhenEmptyUrl: Option[String]): FileUploadContext =
    FileUploadContext(
      FileUploadSessionConfig(
        nonce = Nonce.random,
        continueUrl = "/continue",
        callbackUrl = "/callback",
        minimumNumberOfFiles = min,
        continueWhenEmptyUrl = continueWhenEmptyUrl
      )
    )

  private val accepted = FileUpload.Accepted(
    Nonce(1),
    Timestamp.Any,
    "ref",
    "url",
    ZonedDateTime.parse("2020-01-01T00:00:00Z"),
    "checksum",
    "file.pdf",
    "application/pdf",
    1
  )

  private def acceptedFiles(n: Int): FileUploads =
    FileUploads((1 to n).map(i => accepted.copy(reference = s"ref-$i")))

  "FileUploadContext.isBelowMinimumFiles" should {

    "block when no files, minimum 1, and no continueWhenEmptyUrl" in {
      context(1, None).isBelowMinimumFiles(FileUploads()) shouldBe true
    }

    "allow empty when continueWhenEmptyUrl is set and minimum is 1" in {
      context(1, Some("/empty")).isBelowMinimumFiles(FileUploads()) shouldBe false
    }

    "allow empty when continueWhenEmptyUrl is set even if minimum is 3" in {
      context(3, Some("/empty")).isBelowMinimumFiles(FileUploads()) shouldBe false
    }

    "block a partial upload below minimum even when continueWhenEmptyUrl is set" in {
      context(3, Some("/empty")).isBelowMinimumFiles(acceptedFiles(1)) shouldBe true
    }

    "allow when accepted count meets the minimum" in {
      context(1, None).isBelowMinimumFiles(acceptedFiles(2)) shouldBe false
    }
  }
}
