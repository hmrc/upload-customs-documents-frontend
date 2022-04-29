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

import play.api.libs.json.{Format, JsValue, Json}
import uk.gov.hmrc.uploaddocuments.support.HtmlCleaner

import java.time.ZonedDateTime
import scala.util.matching.Regex

/** Container for file upload status tracking. */
case class FileUploads(files: Seq[FileUpload] = Seq.empty) {

  lazy val isEmpty: Boolean  = acceptedCount == 0
  lazy val isSingle: Boolean = acceptedCount == 1

  lazy val acceptedCount: Int  = files.count { case _: FileUpload.Accepted  => true; case _ => false }
  lazy val initiatedCount: Int = files.count { case _: FileUpload.Initiated => true; case _ => false }
  lazy val postedCount: Int    = files.count { case _: FileUpload.Posted    => true; case _ => false }

  lazy val initiatedOrAcceptedCount: Int = acceptedCount + initiatedCount + postedCount

  lazy val toUploadedFiles: Seq[UploadedFile] =
    files.flatMap(UploadedFile(_))

  def +(file: FileUpload): FileUploads = copy(files = files :+ file)

  lazy val onlyAccepted: FileUploads =
    copy(files = files.filter { case _: FileUpload.Accepted => true; case _ => false })

  def hasFileWithDescription(description: String): Boolean =
    files.exists { case a: FileUpload.Accepted => a.safeDescription.contains(description); case _ => false }

  lazy val tofileUploadErrors: Seq[FileUploadError] = files.collect { case e: ErroredFileUpload => FileUploadError(e) }

}

object FileUploads {
  implicit val formats: Format[FileUploads] = Json.format[FileUploads]
  def apply(initRequest: FileUploadInitializationRequest): FileUploads =
    FileUploads(initRequest.existingFiles.take(initRequest.config.maximumNumberOfFiles).map(FileUpload.apply))
}

/** File upload status */
sealed trait FileUpload {
  val nonce: Nonce
  val timestamp: Timestamp
  val reference: String
  val isReady: Boolean
}
sealed trait ErroredFileUpload extends FileUpload

object FileUpload extends SealedTraitFormats[FileUpload] {

  final def unapply(fileUpload: FileUpload): Option[(Nonce, String)] = Some((fileUpload.nonce, fileUpload.reference))

  def apply(uploadedFile: UploadedFile): FileUpload =
    FileUpload.Accepted(
      nonce           = Nonce.Any,
      timestamp       = Timestamp.Any,
      reference       = uploadedFile.upscanReference,
      checksum        = uploadedFile.checksum,
      fileName        = uploadedFile.fileName,
      fileMimeType    = uploadedFile.fileMimeType,
      fileSize        = uploadedFile.fileSize,
      url             = uploadedFile.downloadUrl,
      uploadTimestamp = uploadedFile.uploadTimestamp,
      cargo           = uploadedFile.cargo,
      description     = uploadedFile.description
    )
  def apply(nonce: Nonce, uploadId: Option[String])(
    upscanResponse: UpscanInitiateResponse): FileUpload.Initiated =
    FileUpload.Initiated(
      nonce         = nonce,
      timestamp     = Timestamp.now,
      reference     = upscanResponse.reference,
      uploadRequest = Some(upscanResponse.uploadRequest),
      uploadId      = uploadId
    )

  final val isWindowPathHaving: Regex = "[a-zA-Z]:.*\\\\(.+)".r("name")

  final def sanitizeFileName(fileName: String): String =
    fileName match {
      case isWindowPathHaving(name) => name
      case name                     => name
    }

  /** Status when file upload attributes has been requested from upscan-initiate but the file itself has not been yet
    * transmitted to S3 bucket.
    */
  case class Initiated(
    nonce: Nonce,
    timestamp: Timestamp,
    reference: String,
    uploadRequest: Option[UploadRequest] = None,
    uploadId: Option[String]             = None
  ) extends FileUpload {
    override val isReady: Boolean = false
  }

  /** Status when the file has successfully arrived to AWS S3 for verification. */
  case class Posted(
    nonce: Nonce,
    timestamp: Timestamp,
    reference: String
  ) extends FileUpload {
    override val isReady: Boolean = false
  }

  /** Status when file transmission has been rejected by AWS S3. */
  case class Rejected(
    nonce: Nonce,
    timestamp: Timestamp,
    reference: String,
    details: S3UploadError
  ) extends ErroredFileUpload {
    override val isReady: Boolean = true
  }

  /** Status when the file has been positively verified and is ready for further actions. */
  case class Accepted(
    nonce: Nonce,
    timestamp: Timestamp,
    reference: String,
    url: String,
    uploadTimestamp: ZonedDateTime,
    checksum: String,
    fileName: String,
    fileMimeType: String,
    fileSize: Int,
    cargo: Option[JsValue]                  = None, // data carried through, from and to host service
    private val description: Option[String] = None
  ) extends FileUpload {
    override val isReady: Boolean = true

    final val safeDescription: Option[String] = description.map(HtmlCleaner.cleanSimpleText)
  }

  /** Status when the file has failed verification and may not be used. */
  case class Failed(
    nonce: Nonce,
    timestamp: Timestamp,
    reference: String,
    details: UpscanNotification.FailureDetails
  ) extends ErroredFileUpload {
    override val isReady: Boolean = true
  }

  /** Status when the file is a duplicate of an existing upload. */
  case class Duplicate(
    nonce: Nonce,
    timestamp: Timestamp,
    reference: String,
    checksum: String,
    existingFileName: String,
    duplicateFileName: String
  ) extends ErroredFileUpload {
    override val isReady: Boolean = true
  }

  override val formats = Set(
    Case[Initiated](Json.format[Initiated]),
    Case[Rejected](Json.format[Rejected]),
    Case[Posted](Json.format[Posted]),
    Case[Accepted](Json.format[Accepted]),
    Case[Failed](Json.format[Failed]),
    Case[Duplicate](Json.format[Duplicate])
  )

}
