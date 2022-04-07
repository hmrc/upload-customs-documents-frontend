/*
 * Copyright 2022 HM Revenue & Customs
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

import java.time.ZonedDateTime
import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.JsValue

case class UploadedFile(
  upscanReference: String,
  downloadUrl: String,
  uploadTimestamp: ZonedDateTime,
  checksum: String,
  fileName: String,
  fileMimeType: String,
  fileSize: Int,
  cargo: Option[JsValue] = None, // data carried through, from and to host service
  description: Option[String] = None,
  previewUrl: Option[String] = None
) {
  def toFileUpload: FileUpload =
    FileUpload.Accepted(
      nonce = Nonce.Any,
      timestamp = Timestamp.Any,
      reference = upscanReference,
      checksum = checksum,
      fileName = fileName,
      fileMimeType = fileMimeType,
      fileSize = fileSize,
      url = downloadUrl,
      uploadTimestamp = uploadTimestamp,
      cargo = cargo,
      description = description
    )
}

object UploadedFile {
  implicit val formats: Format[UploadedFile] = Json.format[UploadedFile]
}
