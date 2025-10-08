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

import play.api.libs.json.Json
import uk.gov.hmrc.uploaddocuments.models.FileUpload.{Duplicate, Failed, Rejected}

sealed trait FileUploadError

case class FileTransmissionFailed(error: S3UploadError) extends FileUploadError
case class FileVerificationFailed(details: UpscanNotification.FailureDetails) extends FileUploadError
case class DuplicateFileUpload(checksum: String, existingFileName: String, duplicateFileName: String)
    extends FileUploadError

object FileUploadError extends SealedTraitFormats[FileUploadError] {

  def apply(file: ErroredFileUpload): FileUploadError = file match {
    case dupe: Duplicate    => DuplicateFileUpload(dupe.checksum, dupe.existingFileName, dupe.duplicateFileName)
    case failed: Failed     => FileVerificationFailed(failed.details)
    case rejected: Rejected => FileTransmissionFailed(rejected.details)
  }

  override val formats = Set(
    Case[FileTransmissionFailed](Json.format[FileTransmissionFailed]),
    Case[FileVerificationFailed](Json.format[FileVerificationFailed]),
    Case[DuplicateFileUpload](Json.format[DuplicateFileUpload])
  )
}
