package uk.gov.hmrc.uploaddocuments.controllers

import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.stubs.ExternalApiStubs
import play.api.libs.ws.DefaultBodyReadables.readableAsString

class ContinueToHostControllerISpec extends ControllerISpecBase with ExternalApiStubs {

  "ContinueToHostController" when {

    "GET /continue-to-host" should {
      "redirect to the continueUrl if non empty file uploads" in {

        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenEmptyUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-empty"),
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full")
            )
        )

        setContext(context)
        setFileUploads(nonEmptyFileUploads)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val expected = givenSomePage(200, "/continue-url")
        val result   = await(request("/continue-to-host").get())

        result.status shouldBe 200
        result.body shouldBe expected
      }

      "redirect to the continueUrl if empty file uploads and no continueWhenEmptyUrl" in {

        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full")
            )
        )

        setContext(context)
        setFileUploads(FileUploads())

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val expected = givenSomePage(200, "/continue-url")
        val result   = await(request("/continue-to-host").get())

        result.status shouldBe 200
        result.body shouldBe expected
      }

      "redirect to the continueWhenEmptyUrl if empty file uploads" in {
        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenEmptyUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-empty"),
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full")
            )
        )

        setContext(context)
        setFileUploads(FileUploads())

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val expected = givenSomePage(200, "/continue-url-if-empty")
        val result   = await(request("/continue-to-host").get())

        result.status shouldBe 200
        result.body shouldBe expected
      }

      "redirect to the continueUrl if full file uploads and no continueWhenFullUrl" in {

        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenEmptyUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-empty")
            )
        )
        setContext(context)
        setFileUploads(nFileUploads(context.config.maximumNumberOfFiles))

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val expected = givenSomePage(200, "/continue-url")
        val result   = await(request("/continue-to-host").get())

        result.status shouldBe 200
        result.body shouldBe expected
      }

      "redirect to the continueWhenFullUrl if full file uploads" in {

        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenEmptyUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-empty"),
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full")
            )
        )

        setContext(context)
        setFileUploads(nFileUploads(context.config.maximumNumberOfFiles))

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val expected = givenSomePage(200, "/continue-url-if-full")
        val result   = await(request("/continue-to-host").get())

        result.status shouldBe 200
        result.body shouldBe expected
      }
    }
  }
}
