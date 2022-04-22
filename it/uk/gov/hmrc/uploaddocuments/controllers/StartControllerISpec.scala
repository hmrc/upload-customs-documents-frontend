package uk.gov.hmrc.uploaddocuments.controllers

import uk.gov.hmrc.uploaddocuments.stubs.UpscanInitiateStubs

class StartControllerISpec extends ControllerISpecBase with UpscanInitiateStubs {

  "StartController" when {

    "GET /" should {
      "show the start page when no jsenabled cookie set" in {

        setContext()
        setFileUploads()

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val callbackUrl =
          appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/$getJourneyId"
        givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

        val result = await(request("/").get())

        result.status shouldBe 200
        result.body should include("url=/upload-customs-documents/choose-files")
      }
    }
  }
}
