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

package uk.gov.hmrc.uploaddocuments.controllers.internal

import uk.gov.hmrc.uploaddocuments.controllers.ControllerISpecBase
import uk.gov.hmrc.uploaddocuments.models.*
import play.api.libs.ws.writeableOf_String
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.SessionId

class WipeOutControllerISpec extends ControllerISpecBase {

  "WipeOutController" when {

    "POST /internal/wipe-out" should {

      "return 404 if wrong http method" in {

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val result = await(backchannelRequest("/wipe-out").get())
        result.status shouldBe 404
      }

      "return 204 and cleanup uploaded files" in {

        val context = FileUploadContext(fileUploadSessionConfig)
        val fileUploads = FileUploads(files =
          Seq(
            FileUpload.Initiated(Nonce.Any, Timestamp.Any, "11370e18-6e24-453e-b45a-76d3e32ea33d")
          )
        )

        setContext(context)
        setFileUploads(fileUploads)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val result = await(backchannelRequest("/wipe-out").post(""))
        result.status shouldBe 204

        getFileUploads() shouldBe None
        getContext() shouldBe Some(context.deactivate())
      }

      "return 204 and do nothing if missing session data" in {

        val context = FileUploadContext(fileUploadSessionConfig)
        val fileUploads = FileUploads(files =
          Seq(
            FileUpload.Initiated(Nonce.Any, Timestamp.Any, "11370e18-6e24-453e-b45a-76d3e32ea33d")
          )
        )

        setContext(context)
        setFileUploads(fileUploads)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val result =
          await(
            backchannelRequest("/wipe-out")(using
              HeaderCarrier(
                sessionId = Some(SessionId("eddb6ee7-c087-4945-a27f-bc9e50ab817b"))
              )
            ).post("")
          )
        result.status shouldBe 204

        getFileUploads() shouldBe Some(fileUploads)
        getContext() shouldBe Some(context)
      }
    }
  }
}
