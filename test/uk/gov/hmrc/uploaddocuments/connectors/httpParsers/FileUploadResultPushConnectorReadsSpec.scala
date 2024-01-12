/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.uploaddocuments.connectors.httpParsers

import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.uploaddocuments.connectors.FileUploadResultPushConnector
import uk.gov.hmrc.uploaddocuments.models.{HostService, JourneyId}
import uk.gov.hmrc.uploaddocuments.models.fileUploadResultPush.Error
import uk.gov.hmrc.uploaddocuments.support.UnitSpec

class FileUploadResultPushConnectorReadsSpec extends UnitSpec {

  object TestFileUploadResultPushConnectorReads extends FileUploadResultPushConnectorReads(HostService.Any)(JourneyId("foo"))

  "FileUploadResultPushConnectorReads.reads" when {

    "given NO_CONTENT (204)" must {

      "return the success response" in {

        val expectedResult = FileUploadResultPushConnector.SuccessResponse
        val actualResult = TestFileUploadResultPushConnectorReads.read("POST", "/foo/bar", HttpResponse(Status.NO_CONTENT, json = Json.obj(), Map()))

        actualResult shouldBe expectedResult
      }
    }

    "given any of result" must {

      "return an error response" in {

        val expectedResult = Left(Error(Status.SERVICE_UNAVAILABLE, s"Failure pushing uploaded files to /foo/bar: { } ${HostService.Any}"))
        val actualResult = TestFileUploadResultPushConnectorReads.read("POST", "/foo/bar", HttpResponse(Status.SERVICE_UNAVAILABLE, json = Json.obj(), Map()))

        actualResult shouldBe expectedResult
      }
    }
  }
}
