/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.uploaddocuments.support

import uk.gov.hmrc.uploaddocuments.models.UpscanNotification.{FailureDetails, QUARANTINE, UploadDetails}
import uk.gov.hmrc.uploaddocuments.models.*

import java.time.{LocalDate, ZonedDateTime}

object TestData {

  val today = LocalDate.now

  val journeyId = JourneyId("testJourneyId")

  val fileUploadContext = FileUploadContext(
    config = FileUploadSessionConfig(
      nonce = Nonce(0),
      continueUrl = "/continue-url",
      backlinkUrl = Some("/backlink-url"),
      callbackUrl = "callback-url"
    )
  )

  val fileUploadInitiated = FileUpload.Initiated(
    nonce = Nonce(1),
    timestamp = Timestamp.Any,
    reference = "foo-bar-ref-1"
  )

  val fileUploadPosted = FileUpload.Posted(
    nonce = Nonce(2),
    timestamp = Timestamp.Any,
    reference = "foo-bar-ref-2"
  )

  val acceptedFileUpload =
    FileUpload.Accepted(
      nonce = Nonce(1),
      timestamp = Timestamp.Any,
      reference = "foo-bar-ref-3",
      url = "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
      uploadTimestamp = ZonedDateTime.parse("2018-04-24T09:30:00Z"),
      checksum = "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
      fileName = "test.pdf",
      fileMimeType = "application/pdf",
      fileSize = 4567890
    )

  val fileUploadFailed = FileUpload.Failed(
    nonce = Nonce(4),
    timestamp = Timestamp.Any,
    reference = "foo-bar-ref-4",
    details = UpscanNotification.FailureDetails(
      failureReason = UpscanNotification.QUARANTINE,
      message = "e.g. This file has a virus"
    )
  )

  val fileUploadRejected = FileUpload.Rejected(
    nonce = Nonce(5),
    timestamp = Timestamp.Any,
    reference = "foo-bar-ref-5",
    details = S3UploadError("a", "b", "c")
  )

  val fileUploadDuplicate = FileUpload.Duplicate(
    nonce = Nonce(6),
    timestamp = Timestamp.Any,
    reference = "foo-bar-ref-6",
    checksum = "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
    existingFileName = "test.pdf",
    duplicateFileName = "test2.png"
  )

  val nonEmptyFileUploads = FileUploads(files = Seq(acceptedFileUpload))

  def s3Errors(fileKey: String) = S3UploadError(
    key = fileKey,
    errorCode = "ErrCode123",
    errorMessage = "Rejected",
    errorRequestId = None,
    errorResource = None
  )

  def upscanFileReady(upscanRef: String, checksum: String = "checksum") = UpscanFileReady(
    reference = upscanRef,
    downloadUrl = "/download",
    uploadDetails = UploadDetails(
      uploadTimestamp = ZonedDateTime.now(),
      checksum = checksum,
      fileName = "file.png",
      fileMimeType = "png",
      size = 4
    )
  )

  def upscanFailed(upscanRef: String) = UpscanFileFailed(
    reference = upscanRef,
    failureDetails = FailureDetails(
      failureReason = QUARANTINE,
      message = "Virus Detected"
    )
  )

}
