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
