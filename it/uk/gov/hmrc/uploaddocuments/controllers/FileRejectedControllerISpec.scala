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

import play.api.http.Status
import uk.gov.hmrc.uploaddocuments.models.*

import java.time.ZonedDateTime

class FileRejectedControllerISpec extends ControllerISpecBase {

  "FileRejectedController" when {

    "GET /file-rejected" should {

      "mark the file as rejected and redirect to the choose-files page" in {

        setContext()
        setFileUploads(
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
              FileUpload.Initiated(Nonce.Any, Timestamp.Any, "2b72fe99-8adf-4edb-865e-622ae710f77c")
            )
          )
        )

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result = await(
          request(
            "/file-rejected?key=2b72fe99-8adf-4edb-865e-622ae710f77c&errorCode=EntityTooLarge&errorMessage=Entity+Too+Large"
          ).withFollowRedirects(false).get()
        )

        result.status shouldBe Status.SEE_OTHER
        result.header("Location") shouldBe Some(routes.ChooseMultipleFilesController.showChooseMultipleFiles.url)

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
              FileUpload.Rejected(
                Nonce.Any,
                Timestamp.Any,
                "2b72fe99-8adf-4edb-865e-622ae710f77c",
                S3UploadError("2b72fe99-8adf-4edb-865e-622ae710f77c", "EntityTooLarge", "Entity Too Large")
              )
            )
          )
        )
      }

      "not mark a file as rejected but redirect to choose-files with the file-required error when no file was submitted" in {

        setContext()
        setFileUploads(
          FileUploads(files =
            Seq(
              FileUpload.Initiated(Nonce.Any, Timestamp.Any, "2b72fe99-8adf-4edb-865e-622ae710f77c")
            )
          )
        )

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result = await(
          request(
            "/file-rejected?key=2b72fe99-8adf-4edb-865e-622ae710f77c&errorCode=InvalidArgument&errorMessage=POST+requires+exactly+one+file+upload+per+request."
          ).withFollowRedirects(false).get()
        )

        result.status shouldBe Status.SEE_OTHER
        result.header("Location") shouldBe Some(
          routes.ChooseMultipleFilesController.showChooseMultipleFiles.url + "?error=fileRequired"
        )

        getFileUploads() shouldBe Some(
          FileUploads(files =
            Seq(
              FileUpload.Initiated(Nonce.Any, Timestamp.Any, "2b72fe99-8adf-4edb-865e-622ae710f77c")
            )
          )
        )
      }

      "not mark a file as rejected but redirect to choose-files with the file-required error when the file is empty" in {

        setContext()
        setFileUploads(
          FileUploads(files =
            Seq(
              FileUpload.Initiated(Nonce.Any, Timestamp.Any, "2b72fe99-8adf-4edb-865e-622ae710f77c")
            )
          )
        )

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result = await(
          request(
            "/file-rejected?key=2b72fe99-8adf-4edb-865e-622ae710f77c&errorCode=EntityTooSmall&errorMessage=Your+proposed+upload+is+smaller+than+the+minimum+allowed+size"
          ).withFollowRedirects(false).get()
        )

        result.status shouldBe Status.SEE_OTHER
        result.header("Location") shouldBe Some(
          routes.ChooseMultipleFilesController.showChooseMultipleFiles.url + "?error=fileRequired"
        )

        getFileUploads() shouldBe Some(
          FileUploads(files =
            Seq(
              FileUpload.Initiated(Nonce.Any, Timestamp.Any, "2b72fe99-8adf-4edb-865e-622ae710f77c")
            )
          )
        )
      }

      "show an error page if the request parameters are invalid" in {

        setContext()
        setFileUploads(
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
              FileUpload.Initiated(Nonce.Any, Timestamp.Any, "2b72fe99-8adf-4edb-865e-622ae710f77c")
            )
          )
        )

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result = await(
          request(
            "/file-rejected?zoo=2b72fe99-8adf-4edb-865e-622ae710f77c&foo=EntityTooLarge&bar=Entity+Too+Large"
          ).withFollowRedirects(false).get()
        )

        result.status shouldBe Status.INTERNAL_SERVER_ERROR

      }
    }
  }
}
