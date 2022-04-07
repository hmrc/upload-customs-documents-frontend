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

package uk.gov.hmrc.uploaddocuments.journeys

import play.api.libs.json.{Format, JsResultException, Json}
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.support.{JsonFormatTest, UnitSpec}

import java.time.ZonedDateTime

class StateFormatsSpec extends UnitSpec {

  implicit val formats: Format[State] = StateFormats.formats
  val generatedAt = java.time.LocalDateTime.of(2018, 12, 11, 10, 20, 30)
  val fileUploadSessionConfig =
    FileUploadContext(config = FileUploadSessionConfig(Nonce.random, "/foo", "/bar", "/zoo"))

  "FileUploadJourneyStateFormats" should {
    "serialize and deserialize state" in new JsonFormatTest[State](info) {
      validateCanReadAndWriteJson(
        State.Uninitialized
      )
      validateCanReadAndWriteJson(
        State.Initialized(
          fileUploadSessionConfig,
          FileUploads(files =
            Seq(
              FileUpload.Initiated(Nonce.Any, Timestamp.Any, "foo1"),
              FileUpload.Posted(Nonce.Any, Timestamp.Any, "foo3"),
              FileUpload.Accepted(
                Nonce.Any,
                Timestamp.Any,
                "foo4",
                "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                "test.pdf",
                "application/pdf",
                4567890
              ),
              FileUpload
                .Failed(
                  Nonce.Any,
                  Timestamp.Any,
                  "foo2",
                  UpscanNotification.FailureDetails(UpscanNotification.QUARANTINE, "some reason")
                )
            )
          )
        )
      )
      validateCanReadAndWriteJson(
        State.ContinueToHost(
          fileUploadSessionConfig,
          FileUploads(files =
            Seq(
              FileUpload.Initiated(Nonce.Any, Timestamp.Any, "foo1"),
              FileUpload.Posted(Nonce.Any, Timestamp.Any, "foo3"),
              FileUpload.Accepted(
                Nonce.Any,
                Timestamp.Any,
                "foo4",
                "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                "test.pdf",
                "application/pdf",
                4567890
              ),
              FileUpload
                .Failed(
                  Nonce.Any,
                  Timestamp.Any,
                  "foo2",
                  UpscanNotification.FailureDetails(UpscanNotification.QUARANTINE, "some reason")
                )
            )
          )
        )
      )
      validateCanReadAndWriteJson(
        State.UploadSingleFile(
          fileUploadSessionConfig,
          "foo-bar-ref",
          UploadRequest(href = "https://foo.bar", fields = Map.empty),
          FileUploads(files =
            Seq(
              FileUpload.Initiated(Nonce.Any, Timestamp.Any, "foo1"),
              FileUpload.Posted(Nonce.Any, Timestamp.Any, "foo3"),
              FileUpload.Accepted(
                Nonce.Any,
                Timestamp.Any,
                "foo4",
                "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                "test.pdf",
                "application/pdf",
                4567890
              ),
              FileUpload
                .Failed(
                  Nonce.Any,
                  Timestamp.Any,
                  "foo2",
                  UpscanNotification.FailureDetails(UpscanNotification.QUARANTINE, "some reason")
                )
            )
          ),
          Some(FileVerificationFailed(UpscanNotification.FailureDetails(UpscanNotification.QUARANTINE, "some reason")))
        )
      )
      validateCanReadAndWriteJson(
        State.UploadSingleFile(
          fileUploadSessionConfig,
          "foo-bar-ref-2",
          UploadRequest(href = "https://foo.bar", fields = Map("amz" -> "123")),
          FileUploads(files =
            Seq(
              FileUpload.Initiated(Nonce.Any, Timestamp.Any, "foo1"),
              FileUpload.Accepted(
                Nonce.Any,
                Timestamp.Any,
                "foo4",
                "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                "test.pdf",
                "application/pdf",
                4567890
              ),
              FileUpload
                .Failed(
                  Nonce.Any,
                  Timestamp.Any,
                  "foo2",
                  UpscanNotification.FailureDetails(UpscanNotification.QUARANTINE, "some reason")
                ),
              FileUpload.Posted(Nonce.Any, Timestamp.Any, "foo3")
            )
          )
        )
      )
      validateCanReadAndWriteJson(
        State.WaitingForFileVerification(
          fileUploadSessionConfig,
          "foo-bar-ref-2",
          UploadRequest(href = "https://foo.bar", fields = Map("amz" -> "123")),
          FileUpload.Posted(Nonce.Any, Timestamp.Any, "foo3"),
          FileUploads(files =
            Seq(
              FileUpload.Initiated(Nonce.Any, Timestamp.Any, "foo1"),
              FileUpload.Accepted(
                Nonce.Any,
                Timestamp.Any,
                "foo4",
                "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                "test.pdf",
                "application/pdf",
                4567890
              ),
              FileUpload
                .Failed(
                  Nonce.Any,
                  Timestamp.Any,
                  "foo2",
                  UpscanNotification.FailureDetails(UpscanNotification.QUARANTINE, "some reason")
                ),
              FileUpload.Posted(Nonce.Any, Timestamp.Any, "foo3")
            )
          )
        )
      )

      validateCanReadAndWriteJson(
        State.Summary(
          fileUploadSessionConfig,
          FileUploads(files =
            Seq(
              FileUpload.Initiated(Nonce.Any, Timestamp.Any, "foo1"),
              FileUpload.Accepted(
                Nonce.Any,
                Timestamp.Any,
                "foo4",
                "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                "test.pdf",
                "application/pdf",
                4567890
              ),
              FileUpload
                .Failed(
                  Nonce.Any,
                  Timestamp.Any,
                  "foo2",
                  UpscanNotification.FailureDetails(UpscanNotification.QUARANTINE, "some reason")
                ),
              FileUpload.Posted(Nonce.Any, Timestamp.Any, "foo3")
            )
          )
        )
      )

      validateCanReadAndWriteJson(
        State.UploadMultipleFiles(
          fileUploadSessionConfig,
          FileUploads(files =
            Seq(
              FileUpload.Initiated(
                Nonce.Any,
                Timestamp.Any,
                "foo1",
                uploadRequest = Some(UploadRequest(href = "https://foo.bar", fields = Map("amz" -> "123"))),
                uploadId = Some("aBc")
              ),
              FileUpload.Accepted(
                Nonce.Any,
                Timestamp.Any,
                "foo4",
                "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                "test.pdf",
                "application/pdf",
                4567890
              ),
              FileUpload
                .Failed(
                  Nonce.Any,
                  Timestamp.Any,
                  "foo2",
                  UpscanNotification.FailureDetails(UpscanNotification.QUARANTINE, "some reason")
                ),
              FileUpload.Posted(Nonce.Any, Timestamp.Any, "foo3")
            )
          )
        )
      )
    }

    "throw an exception when unknown state" in {
      val json = Json.parse("""{"state":"StrangeState","properties":{}}""")
      an[JsResultException] shouldBe thrownBy {
        json.as[State]
      }
    }

  }
}
