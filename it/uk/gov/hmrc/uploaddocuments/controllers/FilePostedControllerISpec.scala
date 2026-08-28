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

class FilePostedControllerISpec extends ControllerISpecBase {

  "FilePostedController" when {

    "GET /file-posted" should {

      "mark the initiated file as posted, drop errored rows and redirect to /choose-files" in {

        setContext()
        setFileUploads(
          FileUploads(files =
            Seq(
              FileUpload.Initiated(Nonce.Any, Timestamp.Any, "key-1"),
              FileUpload.Rejected(
                Nonce.Any,
                Timestamp.Any,
                "ref-r",
                S3UploadError("ref-r", "EntityTooLarge", "too big")
              )
            )
          )
        )

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result = await(request("/file-posted?key=key-1").withFollowRedirects(false).get())

        result.status shouldBe Status.SEE_OTHER
        result.header("Location") shouldBe Some(routes.ChooseMultipleFilesController.showChooseMultipleFiles.url)

        val files = getFileUploads().get.files
        files.collect { case p: FileUpload.Posted => p.reference } shouldBe Seq("key-1")
        files.collect { case e: ErroredFileUpload => e } shouldBe empty
      }

      "return 400 when the key query parameter is missing" in {

        setContext()
        setFileUploads()

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result = await(request("/file-posted").withFollowRedirects(false).get())

        result.status shouldBe Status.BAD_REQUEST
      }
    }
  }
}
