package uk.gov.hmrc.uploaddocuments.controllers

import uk.gov.hmrc.uploaddocuments.stubs.UpscanInitiateStubs
import play.api.libs.ws.DefaultBodyReadables.readableAsString

class StartControllerISpec extends ControllerISpecBase with UpscanInitiateStubs {

  "StartController" when {

    "GET /" should {
      "show the start page when no JS-Detection cookie set" in {

        setContext()
        setFileUploads()

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val callbackUrl =
          appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/$getJourneyId"
        givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

        val result = await(request("/").get())

        result.status shouldBe 200
        result.body should include("0; URL=/upload-customs-documents/choose-files")
      }
    }
  }
}
