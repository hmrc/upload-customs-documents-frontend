package uk.gov.hmrc.uploaddocuments.controllers

import uk.gov.hmrc.uploaddocuments.journeys.State
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.stubs.UpscanInitiateStubs
import uk.gov.hmrc.uploaddocuments.support.SHA256

import scala.concurrent.ExecutionContext.Implicits.global

class StartControllerISpec extends ControllerISpecBase with UpscanInitiateStubs {

  "StartController" when {

    "GET /" should {
      "show the start page when no jsenabled cookie set" in {
        val state = State.Initialized(
          FileUploadContext(fileUploadSessionConfig),
          fileUploads = FileUploads()
        )
        sessionStateService.setState(state)
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val callbackUrl =
          appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/${SHA256.compute(journeyId)}"
        givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

        val result = await(request("/").get())

        result.status shouldBe 200
        result.body should include("url=/upload-customs-documents/choose-files")

        sessionStateService.getState shouldBe state
      }
    }

    "GET /foo" should {
      "return an error page not found" in {
        val state = sessionStateService.getState
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result = await(request("/foo").get())

        result.status shouldBe 404
        result.body should include("Page not found")
        sessionStateService.getState shouldBe state
      }
    }

  }
}
