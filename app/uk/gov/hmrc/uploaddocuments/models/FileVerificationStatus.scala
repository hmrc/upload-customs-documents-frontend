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

package uk.gov.hmrc.uploaddocuments.models

import play.api.i18n.Messages
import play.api.libs.json.{Format, Json}
import play.api.mvc.Call
import uk.gov.hmrc.uploaddocuments.views.UploadFileViewHelper

case class FileVerificationStatus(
  reference: String,
  fileStatus: String,
  fileMimeType: Option[String]         = None,
  fileName: Option[String]             = None,
  fileSize: Option[Int]                = None,
  previewUrl: Option[String]           = None,
  errorMessage: Option[String]         = None,
  uploadRequest: Option[UploadRequest] = None,
  description: Option[String]          = None
)

object FileVerificationStatus {

  def apply(
    fileUpload: FileUpload,
    filePreviewUrl: (String, String) => Call,
    maximumFileSizeBytes: Long,
    allowedFileTypesHint: String
  )(implicit messages: Messages): FileVerificationStatus = {
    val errorMessage: ErroredFileUpload => String =
      file => UploadFileViewHelper.toMessage(FileUploadError(file), maximumFileSizeBytes, allowedFileTypesHint)

    fileUpload match {
      case f: FileUpload.Initiated =>
        FileVerificationStatus(fileUpload.reference, "NOT_UPLOADED", uploadRequest = f.uploadRequest)
      case _: FileUpload.Posted => FileVerificationStatus(fileUpload.reference, "WAITING")
      case f: FileUpload.Accepted =>
        FileVerificationStatus(
          f.reference,
          "ACCEPTED",
          fileMimeType = Some(f.fileMimeType),
          fileName     = Some(f.fileName),
          fileSize     = Some(f.fileSize),
          previewUrl   = Some(s"${filePreviewUrl(f.reference, f.fileName).url}"),
          description  = f.safeDescription
        )
      case f: FileUpload.Failed =>
        FileVerificationStatus(fileUpload.reference, "FAILED", errorMessage = Some(errorMessage(f)))
      case f: FileUpload.Rejected =>
        FileVerificationStatus(fileUpload.reference, "REJECTED", errorMessage = Some(errorMessage(f)))
      case f: FileUpload.Duplicate =>
        FileVerificationStatus(fileUpload.reference, "DUPLICATE", errorMessage = Some(errorMessage(f)))
    }
  }
  implicit val formats: Format[FileVerificationStatus] = Json.format[FileVerificationStatus]
}
