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

import uk.gov.hmrc.uploaddocuments.models.fileUploadResultPush.*
import uk.gov.hmrc.uploaddocuments.models.*
import uk.gov.hmrc.uploaddocuments.stubs.ExternalApiStubs

import java.time.ZonedDateTime
import uk.gov.hmrc.uploaddocuments.stubs.UpscanInitiateStubs

class RemoveControllerISpec extends ControllerISpecBase with ExternalApiStubs with UpscanInitiateStubs {

  val fileUploadNotDeleted: FileUpload.Accepted = FileUpload.Accepted(
    Nonce.Any,
    Timestamp.Any,
    "22370e18-6e24-453e-b45a-76d3e32ea33d",
    "https://s3.amazonaws.com/bucket/123",
    ZonedDateTime.parse("2018-04-24T09:30:00Z"),
    "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
    "test1.png",
    "image/png",
    4567890
  )

  val fileUploadToBeDeleted: FileUpload.Accepted = FileUpload.Accepted(
    Nonce.Any,
    Timestamp.Any,
    "11370e18-6e24-453e-b45a-76d3e32ea33d",
    "https://s3.amazonaws.com/bucket/123",
    ZonedDateTime.parse("2018-04-24T09:30:00Z"),
    "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
    "test2.pdf",
    "application/pdf",
    5234567
  )

  "RemoveController" when {

    "GET /uploaded/:reference/remove" should {

      "remove file from upload list by reference if only one file uploaded and redirect to choose-files" in {

        givenResultPushEndpoint(
          "/result-post-url",
          Payload(
            Request(FileUploadContext(fileUploadSessionConfig), FileUploads(files = Seq.empty)),
            "http://base.external.callback"
          ),
          204
        )

        setContext()
        setFileUploads(FileUploads(files = Seq(fileUploadToBeDeleted)))

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result =
          await(request(s"/uploaded/${fileUploadToBeDeleted.reference}/remove").withFollowRedirects(false).get())

        result.status shouldBe 303
        result.header("Location") shouldBe Some(routes.ChooseMultipleFilesController.showChooseMultipleFiles.url)

        getFileUploads() shouldBe Some(FileUploads(files = Seq.empty))

      }

      "remove file from upload list by reference if more files uploaded and redirect to choose-files" in {

        givenResultPushEndpoint(
          "/result-post-url",
          Payload(
            Request(FileUploadContext(fileUploadSessionConfig), FileUploads(files = Seq(fileUploadNotDeleted))),
            "http://base.external.callback"
          ),
          204
        )

        setContext()
        setFileUploads(FileUploads(files = Seq(fileUploadToBeDeleted, fileUploadNotDeleted)))

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result =
          await(request(s"/uploaded/${fileUploadToBeDeleted.reference}/remove").withFollowRedirects(false).get())

        result.status shouldBe 303
        result.header("Location") shouldBe Some(routes.ChooseMultipleFilesController.showChooseMultipleFiles.url)

        getFileUploads() shouldBe Some(FileUploads(files = Seq(fileUploadNotDeleted)))

        eventually(verifyResultPushHasHappened("/result-post-url", 1))
      }

      "respond 500 if file does not exist" in {

        givenResultPushEndpoint(
          "/result-post-url",
          Payload(
            Request(FileUploadContext(fileUploadSessionConfig), FileUploads(files = Seq(fileUploadNotDeleted))),
            "http://base.external.callback"
          ),
          204
        )

        setContext()
        setFileUploads(FileUploads(files = Seq(fileUploadToBeDeleted, fileUploadNotDeleted)))

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result =
          await(request(s"/uploaded/28c4096b-1e34-4329-8a4c-a72176b64e0f/remove").withFollowRedirects(false).get())

        result.status shouldBe 500
      }
    }
  }
}
