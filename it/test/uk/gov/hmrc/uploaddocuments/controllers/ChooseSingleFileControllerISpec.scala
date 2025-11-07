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
import uk.gov.hmrc.uploaddocuments.stubs.UpscanInitiateStubs
import play.api.libs.ws.DefaultBodyReadables.readableAsString

import java.time.ZonedDateTime

class ChooseSingleFileControllerISpec extends ControllerISpecBase with UpscanInitiateStubs {

  "ChooseSingleFileController" when {

    "GET /choose-file" when {

      "The maximum number of files has not been uploaded" must {

        "show the upload page of first document" in {

          setContext()
          setFileUploads()

          givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
          val callbackUrl =
            appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/$getJourneyId"
          givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

          val result = await(request("/choose-file").get())

          result.status shouldBe 200
          result.body should include(htmlEscapedPageTitle("view.upload-file.first.title"))
          result.body should include(htmlEscapedMessage("view.upload-file.first.heading"))

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

        "show the upload next file page and add initiate request" in {

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

          val result = await(request("/choose-file").get())

          result.status shouldBe 200
          result.body should include(htmlEscapedPageTitle("view.upload-file.next.title"))
          result.body should include(htmlEscapedMessage("view.upload-file.next.heading"))

          getFileUploads() shouldBe Some(
            FileUploads(files =
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

        "show the upload next file page and add initiate request and fail if upscan initiate fails" in {

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

          givenUpscanInitiateFails(callbackUrl, hostUserAgent)
          givenDummyStartUrl()

          val result = await(request("/choose-file").get())

          result.status shouldBe 200
          result.body should include("Dummy Start Page")
        }

      }

      "The maximum number of files has been uploaded" must {

        "redirect and render the Summary view" in {

          setContext()
          setFileUploads(nFileUploads(FILES_LIMIT))

          givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

          val result = await(request("/choose-file").get())

          result.status shouldBe 200
          result.body should include(htmlEscapedPageTitle("view.summary.plural.title", FILES_LIMIT.toString))
          result.body should include(htmlEscapedMessage("view.summary.plural.heading", FILES_LIMIT.toString))
        }
      }
    }
  }
}
