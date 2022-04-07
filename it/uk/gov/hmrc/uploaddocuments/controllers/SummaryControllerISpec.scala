package uk.gov.hmrc.uploaddocuments.controllers

import uk.gov.hmrc.uploaddocuments.journeys.State
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.stubs.UpscanInitiateStubs
import uk.gov.hmrc.uploaddocuments.support.SHA256

import scala.concurrent.ExecutionContext.Implicits.global
import java.time.ZonedDateTime
import uk.gov.hmrc.uploaddocuments.support.TestData
import uk.gov.hmrc.uploaddocuments.stubs.ExternalApiStubs

class SummaryControllerISpec extends ControllerISpecBase with UpscanInitiateStubs with ExternalApiStubs {

  "SummaryController" when {

    "GET /summary" should {
      "show uploaded singular file view" in {
        val state = State.Summary(
          FileUploadContext(fileUploadSessionConfig),
          fileUploads = FileUploads(files = Seq(TestData.acceptedFileUpload))
        )
        sessionStateService.setState(state)
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result = await(request("/summary").get())

        result.status shouldBe 200
        result.body should include(htmlEscapedPageTitle("view.summary.singular.title", "1"))
        result.body should include(htmlEscapedMessage("view.summary.singular.heading", "1"))
        sessionStateService.getState shouldBe state
      }

      "show uploaded plural file view" in {
        val state = State.Summary(
          FileUploadContext(fileUploadSessionConfig),
          fileUploads = FileUploads(files =
            Seq(
              FileUpload.Accepted(
                Nonce.Any,
                Timestamp.Any,
                "11370e18-6e24-453e-b45a-76d3e32ea33d",
                "https://s3.amazonaws.com/bucket/123",
                ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                "test2.pdf",
                "application/pdf",
                5234567
              ),
              FileUpload.Accepted(
                Nonce.Any,
                Timestamp.Any,
                "22370e18-6e24-453e-b45a-76d3e32ea33d",
                "https://s3.amazonaws.com/bucket/123",
                ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                "test1.png",
                "image/png",
                4567890
              )
            )
          )
        )
        sessionStateService.setState(state)
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result = await(request("/summary").get())

        result.status shouldBe 200
        result.body should include(htmlEscapedPageTitle("view.summary.plural.title", "2"))
        result.body should include(htmlEscapedMessage("view.summary.plural.heading", "2"))
        sessionStateService.getState shouldBe state
      }

      "show file upload summary view" in {
        val state = State.Summary(
          FileUploadContext(fileUploadSessionConfig),
          fileUploads = nFileUploads(FILES_LIMIT)
        )
        sessionStateService.setState(state)
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result = await(request("/summary").get())

        result.status shouldBe 200
        result.body should include(htmlEscapedPageTitle("view.summary.plural.title", FILES_LIMIT.toString))
        result.body should include(htmlEscapedMessage("view.summary.plural.heading", FILES_LIMIT.toString))
        sessionStateService.getState shouldBe state
      }
    }

    "POST /summary" should {

      "show upload a file view for export when yes and number of files below the limit" in {

        val fileUploads = FileUploads(files = for (i <- 1 until FILES_LIMIT) yield TestData.acceptedFileUpload)
        val state = State.Summary(
          FileUploadContext(fileUploadSessionConfig),
          fileUploads
        )
        sessionStateService.setState(state)
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val callbackUrl =
          appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/${SHA256.compute(journeyId)}"
        givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

        val result = await(request("/summary").post(Map("choice" -> "yes")))

        result.status shouldBe 200
        result.body should include(htmlEscapedPageTitle("view.upload-file.next.title"))
        result.body should include(htmlEscapedMessage("view.upload-file.next.heading"))
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
            fileUploads.files ++
              Seq(FileUpload.Initiated(Nonce.Any, Timestamp.Any, "11370e18-6e24-453e-b45a-76d3e32ea33d"))
          )
        )
      }

      "show upload a file view when yes and number of files below the limit" in {

        val fileUploads = FileUploads(files = for (i <- 1 until FILES_LIMIT) yield TestData.acceptedFileUpload)
        val state = State.Summary(
          FileUploadContext(fileUploadSessionConfig),
          fileUploads
        )
        sessionStateService.setState(state)
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val callbackUrl =
          appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/${SHA256.compute(journeyId)}"
        givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

        val result = await(request("/summary").post(Map("choice" -> "yes")))

        result.status shouldBe 200
        result.body should include(htmlEscapedPageTitle("view.upload-file.next.title"))
        result.body should include(htmlEscapedMessage("view.upload-file.next.heading"))
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
            fileUploads.files ++
              Seq(FileUpload.Initiated(Nonce.Any, Timestamp.Any, "11370e18-6e24-453e-b45a-76d3e32ea33d"))
          )
        )
      }

      "redirect to the continue_url when yes and files number limit has been reached" in {

        val fileUploads = nFileUploads(FILES_LIMIT)
        val state = State.Summary(
          FileUploadContext(fileUploadSessionConfig),
          fileUploads
        )
        sessionStateService.setState(state)
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val expected = givenSomePage(200, "/continue-url")

        val result = await(request("/summary").post(Map("choice" -> "yes")))

        sessionStateService.getState shouldBe State.ContinueToHost(
          FileUploadContext(fileUploadSessionConfig),
          fileUploads
        )

        result.status shouldBe 200
        result.body shouldBe expected
      }

      "redirect to the continue_url when no and files number below the limit" in {

        val fileUploads = FileUploads(files = for (i <- 1 until FILES_LIMIT) yield TestData.acceptedFileUpload)
        val state = State.Summary(
          FileUploadContext(fileUploadSessionConfig),
          fileUploads
        )
        sessionStateService.setState(state)
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val expected = givenSomePage(200, "/continue-url")

        val result = await(request("/summary").post(Map("choice" -> "no")))

        sessionStateService.getState shouldBe State.ContinueToHost(
          FileUploadContext(fileUploadSessionConfig),
          fileUploads
        )

        result.status shouldBe 200
        result.body shouldBe expected
      }

      "redirect to the continue_url when no and files number above the limit" in {
        val fileUploads = nFileUploads(FILES_LIMIT)
        val state = State.Summary(
          FileUploadContext(fileUploadSessionConfig),
          fileUploads
        )
        sessionStateService.setState(state)
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val expected = givenSomePage(200, "/continue-url")

        val result = await(request("/summary").post(Map("choice" -> "no")))

        sessionStateService.getState shouldBe State.ContinueToHost(
          FileUploadContext(fileUploadSessionConfig),
          fileUploads
        )

        result.status shouldBe 200
        result.body shouldBe expected
      }
    }
  }
}
