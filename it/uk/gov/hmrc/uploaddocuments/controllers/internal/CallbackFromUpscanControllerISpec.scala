package uk.gov.hmrc.uploaddocuments.controllers.internal

import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.uploaddocuments.controllers.ControllerISpecBase
import uk.gov.hmrc.uploaddocuments.journeys.State
import uk.gov.hmrc.uploaddocuments.models._

import java.time.ZonedDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.uploaddocuments.stubs.ExternalApiStubs
import play.api.http.HeaderNames
import play.api.http.MimeTypes
import uk.gov.hmrc.uploaddocuments.support.SHA256
import uk.gov.hmrc.uploaddocuments.connectors.FileUploadResultPushConnector
import play.api.libs.json.JsNumber

class CallbackFromUpscanControllerISpec extends ControllerISpecBase with ExternalApiStubs {

  "CallbackFromUpscanController" when {

    "POST /internal/callback-from-upscan/journey/:journeyId" should {
      "return 400 if callback body invalid" in {
        val nonce = Nonce.random
        sessionStateService.setState(
          State.UploadMultipleFiles(
            FileUploadContext(fileUploadSessionConfig),
            FileUploads(files =
              Seq(
                FileUpload.Initiated(Nonce.Any, Timestamp.Any, "11370e18-6e24-453e-b45a-76d3e32ea33d"),
                FileUpload.Posted(nonce, Timestamp.Any, "2b72fe99-8adf-4edb-865e-622ae710f77c")
              )
            )
          )
        )
        val result =
          await(
            backchannelRequestWithoutSessionId(
              s"/callback-from-upscan/journey/${SHA256.compute(journeyId)}/$nonce"
            )
              .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
              .post(
                Json.obj(
                  "reference" -> JsString("2b72fe99-8adf-4edb-865e-622ae710f77c")
                )
              )
          )

        result.status shouldBe 400
        sessionStateService.getState should beState(
          State.UploadMultipleFiles(
            FileUploadContext(fileUploadSessionConfig),
            FileUploads(files =
              Seq(
                FileUpload.Initiated(Nonce.Any, Timestamp.Any, "11370e18-6e24-453e-b45a-76d3e32ea33d"),
                FileUpload.Posted(nonce, Timestamp.Any, "2b72fe99-8adf-4edb-865e-622ae710f77c")
              )
            )
          )
        )
        eventually {
          verifyResultPushHasNotHappened("/continue")
        }
      }

      "modify file status to Accepted and return 204" in {
        val nonce = Nonce.random
        givenResultPushEndpoint(
          "/result-post-url",
          FileUploadResultPushConnector.Payload.from(
            FileUploadContext(fileUploadSessionConfig),
            FileUploads(files =
              Seq(
                FileUpload.Accepted(
                  nonce,
                  Timestamp.Any,
                  "2b72fe99-8adf-4edb-865e-622ae710f77c",
                  "https://foo.bar/XYZ123/foo.pdf",
                  ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                  "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                  "foo.pdf",
                  "application/pdf",
                  1
                ),
                FileUpload.Accepted(
                  Nonce.Any,
                  Timestamp.Any,
                  "1c72fe99-8adf-4edb-865e-622ae710f88b",
                  "https://foo.bar/XYZ123/bar.pdf",
                  ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                  "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775101",
                  "bar.pdf",
                  "application/pdf",
                  1,
                  Some(Json.obj("bar" -> 1))
                )
              )
            ),
            "http://base.external.callback"
          ),
          204
        )
        sessionStateService.setState(
          State.UploadMultipleFiles(
            FileUploadContext(fileUploadSessionConfig),
            FileUploads(files =
              Seq(
                FileUpload.Initiated(Nonce.Any, Timestamp.Any, "11370e18-6e24-453e-b45a-76d3e32ea33d"),
                FileUpload.Posted(nonce, Timestamp.Any, "2b72fe99-8adf-4edb-865e-622ae710f77c"),
                FileUpload.Accepted(
                  Nonce.Any,
                  Timestamp.Any,
                  "1c72fe99-8adf-4edb-865e-622ae710f88b",
                  "https://foo.bar/XYZ123/bar.pdf",
                  ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                  "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775101",
                  "bar.pdf",
                  "application/pdf",
                  1,
                  Some(Json.obj("bar" -> 1))
                )
              )
            )
          )
        )
        val result =
          await(
            backchannelRequestWithoutSessionId(
              s"/callback-from-upscan/journey/${SHA256.compute(journeyId)}/$nonce"
            )
              .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
              .post(
                Json.obj(
                  "reference"   -> JsString("2b72fe99-8adf-4edb-865e-622ae710f77c"),
                  "fileStatus"  -> JsString("READY"),
                  "downloadUrl" -> JsString("https://foo.bar/XYZ123/foo.pdf"),
                  "uploadDetails" -> Json.obj(
                    "uploadTimestamp" -> JsString("2018-04-24T09:30:00Z"),
                    "checksum"        -> JsString("396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100"),
                    "fileName"        -> JsString("foo.pdf"),
                    "fileMimeType"    -> JsString("application/pdf"),
                    "size"            -> JsNumber(1)
                  )
                )
              )
          )

        result.status shouldBe 204
        sessionStateService.getState should beState(
          State.UploadMultipleFiles(
            FileUploadContext(fileUploadSessionConfig),
            FileUploads(files =
              Seq(
                FileUpload.Initiated(Nonce.Any, Timestamp.Any, "11370e18-6e24-453e-b45a-76d3e32ea33d"),
                FileUpload.Accepted(
                  nonce,
                  Timestamp.Any,
                  "2b72fe99-8adf-4edb-865e-622ae710f77c",
                  "https://foo.bar/XYZ123/foo.pdf",
                  ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                  "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                  "foo.pdf",
                  "application/pdf",
                  1
                ),
                FileUpload.Accepted(
                  Nonce.Any,
                  Timestamp.Any,
                  "1c72fe99-8adf-4edb-865e-622ae710f88b",
                  "https://foo.bar/XYZ123/bar.pdf",
                  ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                  "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775101",
                  "bar.pdf",
                  "application/pdf",
                  1,
                  Some(Json.obj("bar" -> 1))
                )
              )
            )
          )
        )
        eventually {
          verifyResultPushHasHappened("/result-post-url", 1)
        }
      }

      "modify file status to Failed and return 204" in {
        val nonce = Nonce.random
        sessionStateService.setState(
          State.UploadMultipleFiles(
            FileUploadContext(fileUploadSessionConfig),
            FileUploads(files =
              Seq(
                FileUpload.Initiated(Nonce.Any, Timestamp.Any, "11370e18-6e24-453e-b45a-76d3e32ea33d"),
                FileUpload.Posted(nonce, Timestamp.Any, "2b72fe99-8adf-4edb-865e-622ae710f77c"),
                FileUpload.Accepted(
                  Nonce.Any,
                  Timestamp.Any,
                  "1c72fe99-8adf-4edb-865e-622ae710f88b",
                  "https://foo.bar/XYZ123/bar.pdf",
                  ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                  "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775101",
                  "bar.pdf",
                  "application/pdf",
                  1,
                  Some(Json.obj("bar" -> 1))
                )
              )
            )
          )
        )
        val result =
          await(
            backchannelRequestWithoutSessionId(
              s"/callback-from-upscan/journey/${SHA256.compute(journeyId)}/$nonce"
            )
              .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
              .post(
                Json.obj(
                  "reference"  -> JsString("2b72fe99-8adf-4edb-865e-622ae710f77c"),
                  "fileStatus" -> JsString("FAILED"),
                  "failureDetails" -> Json.obj(
                    "failureReason" -> JsString("QUARANTINE"),
                    "message"       -> JsString("e.g. This file has a virus")
                  )
                )
              )
          )

        result.status shouldBe 204
        sessionStateService.getState should beState(
          State.UploadMultipleFiles(
            FileUploadContext(fileUploadSessionConfig),
            FileUploads(files =
              Seq(
                FileUpload.Initiated(Nonce.Any, Timestamp.Any, "11370e18-6e24-453e-b45a-76d3e32ea33d"),
                FileUpload.Failed(
                  nonce,
                  Timestamp.Any,
                  "2b72fe99-8adf-4edb-865e-622ae710f77c",
                  UpscanNotification.FailureDetails(
                    failureReason = UpscanNotification.QUARANTINE,
                    message = "e.g. This file has a virus"
                  )
                ),
                FileUpload.Accepted(
                  Nonce.Any,
                  Timestamp.Any,
                  "1c72fe99-8adf-4edb-865e-622ae710f88b",
                  "https://foo.bar/XYZ123/bar.pdf",
                  ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                  "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775101",
                  "bar.pdf",
                  "application/pdf",
                  1,
                  Some(Json.obj("bar" -> 1))
                )
              )
            )
          )
        )
        eventually {
          verifyResultPushHasNotHappened("/result-post-url")
        }
      }

      "keep file status Accepted and return 204" in {
        val nonce = Nonce.random
        sessionStateService.setState(
          State.UploadMultipleFiles(
            FileUploadContext(fileUploadSessionConfig),
            FileUploads(files =
              Seq(
                FileUpload.Initiated(Nonce.Any, Timestamp.Any, "11370e18-6e24-453e-b45a-76d3e32ea33d"),
                FileUpload.Accepted(
                  nonce,
                  Timestamp.Any,
                  "2b72fe99-8adf-4edb-865e-622ae710f77c",
                  "https://foo.bar/XYZ123/foo.pdf",
                  ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                  "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                  "foo.pdf",
                  "application/pdf",
                  1
                )
              )
            )
          )
        )
        val result =
          await(
            backchannelRequestWithoutSessionId(
              s"/callback-from-upscan/journey/${SHA256.compute(journeyId)}/$nonce"
            )
              .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
              .post(
                Json.obj(
                  "reference"   -> JsString("2b72fe99-8adf-4edb-865e-622ae710f77c"),
                  "fileStatus"  -> JsString("READY"),
                  "downloadUrl" -> JsString("https://foo.bar/XYZ123/foo.pdf"),
                  "uploadDetails" -> Json.obj(
                    "uploadTimestamp" -> JsString("2018-04-24T09:30:00Z"),
                    "checksum"        -> JsString("396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100"),
                    "fileName"        -> JsString("foo.pdf"),
                    "fileMimeType"    -> JsString("application/pdf"),
                    "size"            -> JsNumber(1)
                  )
                )
              )
          )

        result.status shouldBe 204
        sessionStateService.getState should beState(
          State.UploadMultipleFiles(
            FileUploadContext(fileUploadSessionConfig),
            FileUploads(files =
              Seq(
                FileUpload.Initiated(Nonce.Any, Timestamp.Any, "11370e18-6e24-453e-b45a-76d3e32ea33d"),
                FileUpload.Accepted(
                  nonce,
                  Timestamp.Any,
                  "2b72fe99-8adf-4edb-865e-622ae710f77c",
                  "https://foo.bar/XYZ123/foo.pdf",
                  ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                  "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                  "foo.pdf",
                  "application/pdf",
                  1
                )
              )
            )
          )
        )
        eventually {
          verifyResultPushHasNotHappened("/continue")
        }
      }

      "change nothing if nonce not matching" in {
        val nonce = Nonce.random
        sessionStateService.setState(
          State.UploadMultipleFiles(
            FileUploadContext(fileUploadSessionConfig),
            FileUploads(files =
              Seq(
                FileUpload.Initiated(Nonce.Any, Timestamp.Any, "11370e18-6e24-453e-b45a-76d3e32ea33d"),
                FileUpload.Posted(nonce, Timestamp.Any, "2b72fe99-8adf-4edb-865e-622ae710f77c")
              )
            )
          )
        )
        val result =
          await(
            backchannelRequestWithoutSessionId(
              s"/callback-from-upscan/journey/${SHA256.compute(journeyId)}/${Nonce.random}"
            )
              .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
              .post(
                Json.obj(
                  "reference"   -> JsString("2b72fe99-8adf-4edb-865e-622ae710f77c"),
                  "fileStatus"  -> JsString("READY"),
                  "downloadUrl" -> JsString("https://foo.bar/XYZ123/foo.pdf"),
                  "uploadDetails" -> Json.obj(
                    "uploadTimestamp" -> JsString("2018-04-24T09:30:00Z"),
                    "checksum"        -> JsString("396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100"),
                    "fileName"        -> JsString("foo.pdf"),
                    "fileMimeType"    -> JsString("application/pdf"),
                    "size"            -> JsNumber(1)
                  )
                )
              )
          )

        result.status shouldBe 204
        sessionStateService.getState should beState(
          State.UploadMultipleFiles(
            FileUploadContext(fileUploadSessionConfig),
            FileUploads(files =
              Seq(
                FileUpload.Initiated(Nonce.Any, Timestamp.Any, "11370e18-6e24-453e-b45a-76d3e32ea33d"),
                FileUpload.Posted(nonce, Timestamp.Any, "2b72fe99-8adf-4edb-865e-622ae710f77c")
              )
            )
          )
        )
        eventually {
          verifyResultPushHasNotHappened("/continue")
        }
      }
    }

  }
}
