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

import play.api.data.FormError
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.Call
import uk.gov.hmrc.uploaddocuments.models.*

object UploadFileViewHelper {

  def initialScriptStateFrom(
    initialFileUploads: Seq[FileUpload],
    previewFile: (String, String) => Call,
    maximumFileSizeBytes: Long,
    allowedFileTypesHint: String
  )(implicit messages: Messages): String = {
    val fileVerificationStatuses = initialFileUploads.map(file =>
      FileVerificationStatus(file, previewFile, maximumFileSizeBytes, allowedFileTypesHint)
    )
    Json.stringify(Json.toJson(fileVerificationStatuses))
  }

  def toFormError(error: FileUploadError, maximumFileSizeBytes: Long, allowedFileTypesHint: String)(implicit
    messages: Messages
  ): FormError =
    FormError("file", Seq(toMessage(error, maximumFileSizeBytes, allowedFileTypesHint)))

  def toMessage(error: FileUploadError, maximumFileSizeBytes: Long, allowedFileTypesHint: String)(implicit
    messages: Messages
  ): String = error match {
    case FileTransmissionFailed(error) =>
      messages(
        UploadFileViewHelper.toMessageKey(error),
        humanReadableFileSize(maximumFileSizeBytes),
        allowedFileTypesHint
      )
    case FileVerificationFailed(details) =>
      messages(
        UploadFileViewHelper.toMessageKey(details),
        humanReadableFileSize(maximumFileSizeBytes),
        allowedFileTypesHint
      )
    case _: DuplicateFileUpload => messages(duplicateFileMessageKey)
  }

  def toMessageKey(error: S3UploadError): String = error.errorCode match {
    case "400" | "InvalidArgument" => "error.file-upload.required"
    case "InternalError"           => "error.file-upload.try-again"
    case "EntityTooLarge"          => "error.file-upload.invalid-size-large"
    case "EntityTooSmall"          => "error.file-upload.invalid-size-small"
    case _                         => "error.file-upload.unknown"
  }

  def toMessageKey(details: UpscanNotification.FailureDetails): String = details.failureReason match {
    case UpscanNotification.QUARANTINE => "error.file-upload.quarantine"
    case UpscanNotification.REJECTED   => "error.file-upload.invalid-type"
    case UpscanNotification.UNKNOWN    => "error.file-upload.unknown"
  }

  val duplicateFileMessageKey: String = "error.file-upload.duplicate"

  def humanReadableFileSize(bytes: Long): String =
    if (bytes >= 1000000000) s"${(bytes / 1000000000)} GB"
    else if (bytes >= 1000000) s"${(bytes / 1000000)} MB"
    else if (bytes >= 1000) s"${(bytes / 1000)} kB"
    else s"$bytes B"
}
