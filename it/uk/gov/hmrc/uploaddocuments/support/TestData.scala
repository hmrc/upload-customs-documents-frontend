package uk.gov.hmrc.uploaddocuments.support

import uk.gov.hmrc.uploaddocuments.models._

import java.time.{LocalDate, ZonedDateTime}

object TestData {

  val today = LocalDate.now

  final val acceptedFileUpload =
    FileUpload.Accepted(
      Nonce(1),
      Timestamp.Any,
      "foo-bar-ref-1",
      "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
      ZonedDateTime.parse("2018-04-24T09:30:00Z"),
      "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
      "test.pdf",
      "application/pdf",
      4567890
    )

}
