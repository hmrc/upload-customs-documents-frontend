package uk.gov.hmrc.uploaddocuments.controllers

import uk.gov.hmrc.uploaddocuments.journeys.State
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.stubs.UpscanInitiateStubs
import uk.gov.hmrc.uploaddocuments.support.SHA256

import scala.concurrent.ExecutionContext.Implicits.global

class ChooseMultipleFilesControllerISpec extends ControllerISpecBase with UpscanInitiateStubs {

  "ChooseMultipleFilesController" when {

    "GET /choose-files" should {
      "show the upload multiple files page when cookie set" in {
        val state = State.Initialized(
          FileUploadContext(fileUploadSessionConfig),
          fileUploads = FileUploads()
        )
        sessionStateService.setState(state)
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result = await(requestWithCookies("/choose-files", "jsenabled" -> "true").get())

        result.status shouldBe 200
        result.body should include(htmlEscapedPageTitle("view.upload-multiple-files.title"))
        result.body should include(htmlEscapedMessage("view.upload-multiple-files.heading"))
        sessionStateService.getState shouldBe State.UploadMultipleFiles(
          FileUploadContext(fileUploadSessionConfig),
          fileUploads = FileUploads()
        )
      }

      "show the upload single file per page when no cookie set" in {
        val state = State.Initialized(
          FileUploadContext(fileUploadSessionConfig),
          fileUploads = FileUploads()
        )
        sessionStateService.setState(state)
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val callbackUrl =
          appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/${SHA256.compute(journeyId)}"
        givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

        val result = await(request("/choose-files").get())

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

      "reload the upload multiple files page " in {
        val state = State.UploadMultipleFiles(
          FileUploadContext(fileUploadSessionConfig),
          fileUploads = FileUploads()
        )
        sessionStateService.setState(state)
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result = await(request("/choose-files").get())

        result.status shouldBe 200
        result.body should include(htmlEscapedPageTitle("view.upload-multiple-files.title"))
        result.body should include(htmlEscapedMessage("view.upload-multiple-files.heading"))
        sessionStateService.getState shouldBe state
      }

      "retreat from finished to the upload multiple files page " in {
        val state = State.ContinueToHost(
          FileUploadContext(fileUploadSessionConfig),
          FileUploads()
        )
        sessionStateService.setState(state)
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result = await(requestWithCookies("/choose-files", "jsenabled" -> "true").get())

        result.status shouldBe 200
        result.body should include(htmlEscapedPageTitle("view.upload-multiple-files.title"))
        result.body should include(htmlEscapedMessage("view.upload-multiple-files.heading"))
        sessionStateService.getState shouldBe State.UploadMultipleFiles(
          FileUploadContext(fileUploadSessionConfig),
          fileUploads = FileUploads()
        )
      }
    }

  }
}
