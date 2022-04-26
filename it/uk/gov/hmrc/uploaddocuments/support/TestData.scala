package uk.gov.hmrc.uploaddocuments.support

import uk.gov.hmrc.uploaddocuments.models._

import java.time.{LocalDate, ZonedDateTime}

object TestData {

  val today = LocalDate.now

  val journeyId = "testJourneyId"

  val fileUploadInitiated = FileUpload.Initiated(
    Nonce(1),
    Timestamp.Any,
    "foo-bar-ref-1"
  )

  val fileUploadPosted = FileUpload.Posted(
    Nonce(2),
    Timestamp.Any,
    "foo-bar-ref-2"
  )

  val acceptedFileUpload =
    FileUpload.Accepted(
      Nonce(1),
      Timestamp.Any,
      "foo-bar-ref-3",
      "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
      ZonedDateTime.parse("2018-04-24T09:30:00Z"),
      "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
      "test.pdf",
      "application/pdf",
      4567890
    )

  val fileUploadFailed = FileUpload.Failed(
    Nonce(4),
    Timestamp.Any,
    "foo-bar-ref-4",
    UpscanNotification.FailureDetails(
      UpscanNotification.QUARANTINE,
      "e.g. This file has a virus"
    )
  )

  val fileUploadRejected = FileUpload.Rejected(
    Nonce(5),
    Timestamp.Any,
    "foo-bar-ref-5",
    S3UploadError("a", "b", "c")
  )

  val fileUploadDuplicate = FileUpload.Duplicate(
    Nonce(6),
    Timestamp.Any,
    "foo-bar-ref-6",
    "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
    "test.pdf",
    "test2.png"
  )

  val nonEmptyFileUploads = FileUploads(files = Seq(acceptedFileUpload))

  def s3Errors(fileKey: String) = S3UploadError(
    key = fileKey,
    errorCode = "ErrCode123",
    errorMessage = "Rejected",
    errorRequestId = None,
    errorResource = None
  )

}
