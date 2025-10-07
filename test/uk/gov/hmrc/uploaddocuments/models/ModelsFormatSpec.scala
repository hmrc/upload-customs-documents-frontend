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

package uk.gov.hmrc.uploaddocuments.models

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.*
import java.time.ZonedDateTime

class ModelsFormatSpec extends AnyWordSpec with Matchers {

  "Json formatter" should {
    "be provided for FileUploadError" in {
      val entity = FileUploadError(
        FileUpload.Failed(
          Nonce.Any,
          Timestamp.Any,
          "123",
          UpscanNotification.FailureDetails(UpscanNotification.QUARANTINE, "This file has virus.")
        )
      )
      FileUploadError.format.reads(FileUploadError.format.writes(entity)).asOpt.get shouldBe entity
    }

    "be provided for S3UploadSuccess" in {
      val entity = S3UploadSuccess(key = "123", bucket = Some("foo-bar-bucket"))
      S3UploadSuccess.formats.reads(S3UploadSuccess.formats.writes(entity)).asOpt.get shouldBe entity
    }

    "be provided for UpscanInitiateRequest" in {
      val entity = UpscanInitiateRequest(
        callbackUrl = "https://foo.bar/callback",
        successRedirect = Some("https://foo.bar/success"),
        errorRedirect = Some("https://foo.bar/failure"),
        minimumFileSize = Some(0),
        maximumFileSize = Some(100000000),
        consumingService = Some("foo-bar-service")
      )
      UpscanInitiateRequest.formats.reads(UpscanInitiateRequest.formats.writes(entity)).asOpt.get shouldBe entity
    }

    "be provided for UpscanInitiateResponse" in {
      val entity = UpscanInitiateResponse(
        reference = "123",
        uploadRequest =
          UploadRequest(href = "https://foo.bar/upload", fields = Map("Content-Type" -> "application/xml"))
      )
      UpscanInitiateResponse.formats.reads(UpscanInitiateResponse.formats.writes(entity)).asOpt.get shouldBe entity
    }

    "be provided for fileUploadResultPush.Request" in {
      val entity = fileUploadResultPush.Request(
        url = "https://foo.bar/upload",
        nonce = Nonce(123),
        uploadedFiles = Seq(
          UploadedFile(
            upscanReference = "123",
            downloadUrl = "https://foo.bar/download",
            uploadTimestamp = ZonedDateTime.parse("2007-12-03T10:15:30+01:00"),
            checksum = "123",
            fileName = "foo.bar",
            fileMimeType = "application/xml",
            fileSize = 1000
          )
        ),
        context =
          Some(Json.obj("foo" -> Json.obj("bar" -> JsNumber(123), "url" -> JsString("https://foo.bar/upload")))),
        hostService = HostService.Any
      )
      fileUploadResultPush.Request.format
        .reads(fileUploadResultPush.Request.format.writes(entity))
        .asOpt
        .get shouldBe entity
    }

    "be provided for fileUploadResultPush.Payload" in {
      val entity = fileUploadResultPush.Payload(
        nonce = Nonce(123),
        uploadedFiles = Seq(
          UploadedFile(
            upscanReference = "123",
            downloadUrl = "https://foo.bar/download",
            uploadTimestamp = ZonedDateTime.parse("2007-12-03T10:15:30+01:00"),
            checksum = "123",
            fileName = "foo.bar",
            fileMimeType = "application/xml",
            fileSize = 1000
          )
        ),
        cargo = Some(Json.obj("foo" -> Json.obj("bar" -> JsNumber(123), "url" -> JsString("https://foo.bar/upload"))))
      )
      fileUploadResultPush.Payload.format
        .reads(fileUploadResultPush.Payload.format.writes(entity))
        .asOpt
        .get shouldBe entity
    }
  }

}
