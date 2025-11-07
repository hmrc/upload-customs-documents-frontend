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

import uk.gov.hmrc.uploaddocuments.models.*
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
