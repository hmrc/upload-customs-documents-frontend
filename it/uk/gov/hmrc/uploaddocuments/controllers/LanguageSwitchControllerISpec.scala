package uk.gov.hmrc.uploaddocuments.controllers

import uk.gov.hmrc.uploaddocuments.stubs.UpscanInitiateStubs

class LanguageSwitchControllerISpec extends ControllerISpecBase with UpscanInitiateStubs {

  "LanguageSwitchController" when {

    "GET /language/:lang" should {
      "redirect to the current page with selected language" in {

        setContext()
        setFileUploads()

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val callbackUrl =
          appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/$getJourneyId"

        givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

        val result =
          await(
            request("/language/cy")
              .addHttpHeaders("Referer" -> "/upload-customs-documents/choose-file")
              .get()
          )

        result.status shouldBe 200
        result.uri.toString should include("/upload-customs-documents/choose-file")
      }

      "redirect to the default page with selected language when no Referer header is set" in {

        setContext()
        setFileUploads()

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val callbackUrl =
          appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/$getJourneyId"

        givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

        val result = await(request("/language/cy").get())

        result.status shouldBe 200
        result.uri.toString should include("/upload-customs-documents/")
      }
    }
  }
}
