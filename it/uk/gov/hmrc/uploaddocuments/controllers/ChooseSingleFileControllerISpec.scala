package uk.gov.hmrc.uploaddocuments.controllers

import uk.gov.hmrc.uploaddocuments.journeys.State
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.stubs.UpscanInitiateStubs
import uk.gov.hmrc.uploaddocuments.support.SHA256

import scala.concurrent.ExecutionContext.Implicits.global
import java.time.ZonedDateTime

class ChooseSingleFileControllerISpec extends ControllerISpecBase with UpscanInitiateStubs {

  "ChooseSingleFileController" when {

    "GET /choose-file" should {
      "show the upload page of first document" in {
        val state = State.Initialized(FileUploadContext(fileUploadSessionConfig), FileUploads())
        sessionStateService.setState(state)
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val callbackUrl =
          appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/${SHA256.compute(journeyId)}"
        givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

        val result = await(request("/choose-file").get())

        result.status shouldBe 200
        result.body should include(htmlEscapedPageTitle("view.upload-file.first.title"))
        result.body should include(htmlEscapedMessage("view.upload-file.first.heading"))

        sessionStateService.getState shouldBe State.UploadSingleFile(
          FileUploadContext(fileUploadSessionConfig),
          reference = "11370e18-6e24-453e-b45a-76d3e32ea33d",
          uploadRequest = UploadRequest(
            href = "https://bucketName.s3.eu-west-2.amazonaws.com",
            fields = Map(
              "Content-Type"            -> "application/xml",
              "acl"                     -> "private",
              "key"                     -> "xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
              "policy"                  -> "xxxxxxxx==",
              "x-amz-algorithm"         -> "AWS4-HMAC-SHA256",
              "x-amz-credential"        -> "ASIAxxxxxxxxx/20180202/eu-west-2/s3/aws4_request",
              "x-amz-date"              -> "yyyyMMddThhmmssZ",
              "x-amz-meta-callback-url" -> callbackUrl,
              "x-amz-signature"         -> "xxxx",
              "success_action_redirect" -> "https://myservice.com/nextPage",
              "error_action_redirect"   -> "https://myservice.com/errorPage"
            )
          ),
          fileUploads = FileUploads(files =
            Seq(FileUpload.Initiated(Nonce.Any, Timestamp.Any, "11370e18-6e24-453e-b45a-76d3e32ea33d"))
          )
        )
      }

      "show the singular file uploaded page" in {
        val state = State.Initialized(
          FileUploadContext(fileUploadSessionConfig),
          FileUploads(
            Seq(
              FileUpload.Accepted(
                Nonce.Any,
                Timestamp.Any,
                "f029444f-415c-4dec-9cf2-36774ec63ab8",
                "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                "test.pdf",
                "application/pdf",
                4567890
              )
            )
          )
        )
        sessionStateService.setState(state)
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val callbackUrl =
          appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/${SHA256.compute(journeyId)}"
        givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

        val result = await(request("/choose-file").get())

        result.status shouldBe 200
        result.body should include(htmlEscapedPageTitle("view.summary.singular.title", "1"))
        result.body should include(htmlEscapedMessage("view.summary.singular.heading", "1"))

        sessionStateService.getState shouldBe State.Summary(
          FileUploadContext(fileUploadSessionConfig),
          fileUploads = FileUploads(files =
            Seq(
              FileUpload.Accepted(
                Nonce.Any,
                Timestamp.Any,
                "f029444f-415c-4dec-9cf2-36774ec63ab8",
                "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                "test.pdf",
                "application/pdf",
                4567890
              )
            )
          )
        )
      }

      "show the plural file uploaded page" in {
        val state = State.Initialized(
          FileUploadContext(fileUploadSessionConfig),
          FileUploads(
            Seq(
              FileUpload.Accepted(
                Nonce.Any,
                Timestamp.Any,
                "f029444f-415c-4dec-9cf2-36774ec63ab8",
                "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                "test.pdf",
                "application/pdf",
                4567890
              ),
              FileUpload.Accepted(
                Nonce.Any,
                Timestamp.Any,
                "f029444f-415c-4dec-9cf2-36774ec63fff",
                "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                ZonedDateTime.parse("2019-06-11T19:17:21Z"),
                "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                "test2.jpeg",
                "image/jpeg",
                4567891
              )
            )
          )
        )
        sessionStateService.setState(state)
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val callbackUrl =
          appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/${SHA256.compute(journeyId)}"
        givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

        val result = await(request("/choose-file").get())

        result.status shouldBe 200
        result.body should include(htmlEscapedPageTitle("view.summary.plural.title", "2"))
        result.body should include(htmlEscapedMessage("view.summary.plural.heading", "2"))

        sessionStateService.getState shouldBe State.Summary(
          FileUploadContext(fileUploadSessionConfig),
          fileUploads = FileUploads(files =
            Seq(
              FileUpload.Accepted(
                Nonce.Any,
                Timestamp.Any,
                "f029444f-415c-4dec-9cf2-36774ec63ab8",
                "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                "test.pdf",
                "application/pdf",
                4567890
              ),
              FileUpload.Accepted(
                Nonce.Any,
                Timestamp.Any,
                "f029444f-415c-4dec-9cf2-36774ec63fff",
                "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                ZonedDateTime.parse("2019-06-11T19:17:21Z"),
                "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                "test2.jpeg",
                "image/jpeg",
                4567891
              )
            )
          )
        )
      }
    }
  }
}
