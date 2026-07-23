/*
 * Copyright 2026 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock.*
import uk.gov.hmrc.uploaddocuments.models.*
import uk.gov.hmrc.uploaddocuments.stubs.{ExternalApiStubs, UpscanInitiateStubs}
import play.api.libs.ws.DefaultBodyReadables.readableAsString

import java.time.ZonedDateTime
import scala.concurrent.duration.*

class ChooseMultipleFilesControllerISpec extends ControllerISpecBase with UpscanInitiateStubs with ExternalApiStubs {

  "ChooseMultipleFilesController" when {

    "GET /choose-files" should {

      "return 200 and render the upload multiple files page with a file upload form" in {

        setContext()
        setFileUploads()

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val callbackUrl =
          appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/$getJourneyId"
        givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

        val result = await(request("/choose-files").get())

        result.status shouldBe 200
        result.body should include(htmlEscapedPageTitle("view.upload-multiple-files.title"))
        result.body should include(htmlEscapedMessage("view.upload-multiple-files.heading"))
        result.body should include("<input")
      }

      "return 200 and show an already-accepted file's name in the file list" in {

        setContext()
        setFileUploads(
          FileUploads(
            Seq(
              FileUpload.Accepted(
                Nonce.Any,
                Timestamp.Any,
                "f029444f-415c-4dec-9cf2-36774ec63ab8",
                "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                "test.pdf",
                "application/pdf",
                4567890
              )
            )
          )
        )

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val callbackUrl =
          appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/$getJourneyId"
        givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

        val result = await(request("/choose-files").get())

        result.status shouldBe 200
        result.body should include(htmlEscapedPageTitle("view.upload-multiple-files.title"))
        result.body should include("test.pdf")
      }

      "show an error in the summary and the file list" in {

        setContext()
        setFileUploads(
          FileUploads(
            Seq(
              FileUpload.Accepted(
                Nonce.Any,
                Timestamp.Any,
                "f029444f-415c-4dec-9cf2-36774ec63ab8",
                "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                "test.pdf",
                "application/pdf",
                4567890
              ),
              FileUpload.Duplicate(
                Nonce.Any,
                Timestamp.Any,
                "cc0dcf5e-415c-4dec-9cf2-36774ec63ab8",
                "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                existingFileName = "test.pdf",
                duplicateFileName = "test.pdf"
              )
            )
          )
        )

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val callbackUrl =
          appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/$getJourneyId"
        givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

        val result = await(request("/choose-files").get())

        result.status shouldBe 200
        result.body should include(htmlEscapedMessage("error.summary.heading"))
        result.body should include(htmlEscapedMessage("error.file-upload.duplicate"))
        result.body should include("govuk-form-group--error")
        result.body should include("#error-cc0dcf5e-415c-4dec-9cf2-36774ec63ab8")
      }

      "initiate Upscan and store the initiated file upload in the session" in {

        setContext()
        setFileUploads()

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val callbackUrl =
          appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/$getJourneyId"
        givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

        val result = await(request("/choose-files").get())

        result.status shouldBe 200

        getFileUploads() shouldBe Some(
          FileUploads(files =
            Seq(
              FileUpload.Initiated(
                Nonce.Any,
                Timestamp.Any,
                "11370e18-6e24-453e-b45a-76d3e32ea33d",
                Some(
                  UploadRequest(
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
                  )
                )
              )
            )
          )
        )
      }

      "reuse the existing initiated upload on a repeat render instead of calling upscan-initiate again" in {
        setContext()
        setFileUploads()
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val callbackUrl = appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/$getJourneyId"
        givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

        await(request("/choose-files").get()).status shouldBe 200
        await(request("/choose-files").get()).status shouldBe 200

        verify(1, postRequestedFor(urlEqualTo("/upscan/v2/initiate")))
      }

      "initiate a fresh upload instead of reusing an Initiated row older than 30 minutes" in {
        setContext()
        setFileUploads(
          FileUploads(
            Seq(
              FileUpload.Initiated(
                Nonce.Any,
                Timestamp(System.currentTimeMillis() - 31.minutes.toMillis),
                "stale-ref",
                Some(testUploadRequest)
              )
            )
          )
        )
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val callbackUrl = appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/$getJourneyId"
        givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

        await(request("/choose-files").get()).status shouldBe 200

        verify(1, postRequestedFor(urlEqualTo("/upscan/v2/initiate")))
      }

      "render the standard error page when upscan initiation fails" in {
        setContext()
        setFileUploads()
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val callbackUrl = appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/$getJourneyId"
        givenUpscanInitiateFails(callbackUrl, hostUserAgent)

        val result = await(request("/choose-files").get())

        // The platform ErrorHandler (see PreviewControllerISpec for the same pattern) renders the
        // standard error view with an Ok status rather than a 5xx status.
        result.status shouldBe 200
        result.body should include(htmlEscapedPageTitle("global.error.500.title"))
        result.body should include(htmlEscapedMessage("global.error.500.heading"))
      }

      "not initiate Upscan and hide the upload form when the maximum number of files is reached" in {
        setContext()
        setFileUploads(FileUploads(Seq.tabulate(FILES_LIMIT) { i =>
          FileUpload.Accepted(
            Nonce.Any,
            Timestamp.Any,
            s"ref-$i",
            "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
            ZonedDateTime.parse("2018-04-24T09:30:00Z"),
            "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
            s"test$i.pdf",
            "application/pdf",
            4567890
          )
        }))
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result = await(request("/choose-files").get())

        result.status shouldBe 200
        result.body should not include "type=\"file\""
        result.body should include("You can upload up to")
        verify(0, postRequestedFor(urlEqualTo("/upscan/v2/initiate")))
      }

      "not initiate Upscan and hide the upload form when a Posted file fills the last remaining slot" in {
        setContext()
        val accepted = Seq.tabulate(FILES_LIMIT - 1) { i =>
          FileUpload.Accepted(
            Nonce.Any,
            Timestamp.Any,
            s"ref-$i",
            "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
            ZonedDateTime.parse("2018-04-24T09:30:00Z"),
            "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
            s"test$i.pdf",
            "application/pdf",
            4567890
          )
        }
        setFileUploads(FileUploads(accepted :+ FileUpload.Posted(Nonce.Any, Timestamp.Any, "ref-posted")))
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result = await(request("/choose-files").get())

        result.status shouldBe 200
        result.body should not include "type=\"file\""
        verify(0, postRequestedFor(urlEqualTo("/upscan/v2/initiate")))
      }
    }
  }
}
