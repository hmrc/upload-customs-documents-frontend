/*
 * Copyright 2025 HM Revenue & Customs
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
