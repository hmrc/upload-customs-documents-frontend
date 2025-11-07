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

import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.uploaddocuments.models.*
import uk.gov.hmrc.uploaddocuments.stubs.UpscanInitiateStubs
import play.api.libs.ws.writeableOf_String
import play.api.libs.ws.readableAsJson

class InitiateUpscanControllerISpec extends ControllerISpecBase with UpscanInitiateStubs {

  "InitiateUpscanController" when {

    "POST /initiate-upscan/:uploadId" should {

      "initialise first file upload" in {

        setContext()
        setFileUploads()

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val callbackUrl =
          appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/$getJourneyId"

        givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

        val result = await(request("/initiate-upscan/001").post(""))

        result.status shouldBe 200
        val json = result.body[JsValue]
        (json \ "upscanReference").as[String] shouldBe "11370e18-6e24-453e-b45a-76d3e32ea33d"
        (json \ "uploadId").as[String] shouldBe "001"
        (json \ "uploadRequest").as[JsObject] shouldBe Json.obj(
          "href" -> "https://bucketName.s3.eu-west-2.amazonaws.com",
          "fields" -> Json.obj(
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

        getFileUploads() shouldBe Some(
          FileUploads(files =
            Seq(
              FileUpload.Initiated(
                Nonce.Any,
                Timestamp.Any,
                "11370e18-6e24-453e-b45a-76d3e32ea33d",
                uploadId = Some("001"),
                uploadRequest = Some(
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

      "initialise next file upload" in {

        setContext()
        setFileUploads(
          FileUploads(
            Seq(FileUpload.Posted(Nonce.Any, Timestamp.Any, "23370e18-6e24-453e-b45a-76d3e32ea389"))
          )
        )

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val callbackUrl =
          appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/$getJourneyId"
        givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

        val result = await(request("/initiate-upscan/002").post(""))

        result.status shouldBe 200
        val json = result.body[JsValue]
        (json \ "upscanReference").as[String] shouldBe "11370e18-6e24-453e-b45a-76d3e32ea33d"
        (json \ "uploadId").as[String] shouldBe "002"
        (json \ "uploadRequest").as[JsObject] shouldBe Json.obj(
          "href" -> "https://bucketName.s3.eu-west-2.amazonaws.com",
          "fields" -> Json.obj(
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

        getFileUploads() shouldBe Some(
          FileUploads(files =
            Seq(
              FileUpload.Posted(Nonce.Any, Timestamp.Any, "23370e18-6e24-453e-b45a-76d3e32ea389"),
              FileUpload.Initiated(
                Nonce.Any,
                Timestamp.Any,
                "11370e18-6e24-453e-b45a-76d3e32ea33d",
                uploadId = Some("002"),
                uploadRequest = Some(
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

      "initialise next file upload and fail if upscan initiate fails" in {

        setContext()
        setFileUploads(
          FileUploads(
            Seq(FileUpload.Posted(Nonce.Any, Timestamp.Any, "23370e18-6e24-453e-b45a-76d3e32ea389"))
          )
        )

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val callbackUrl =
          appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/$getJourneyId"

        givenUpscanInitiateFails(callbackUrl, hostUserAgent)

        val result = await(request("/initiate-upscan/002").post(""))

        result.status shouldBe 400

      }
    }
  }
}
