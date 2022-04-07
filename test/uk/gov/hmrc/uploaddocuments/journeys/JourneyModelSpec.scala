/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.uploaddocuments.journeys

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.uploaddocuments.connectors.{UpscanInitiateRequest, UpscanInitiateResponse}
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.support.BaseJourneySpec
import uk.gov.hmrc.uploaddocuments.journeys.JourneyModel._
import uk.gov.hmrc.uploaddocuments.journeys.State._
import uk.gov.hmrc.uploaddocuments.journeys.JourneyModel._

import java.time.ZonedDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.uploaddocuments.connectors.FileUploadResultPushConnector

class JourneyModelSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with BaseJourneySpec with TestData {

  val fileUploadContext = FileUploadContext(
    config = FileUploadSessionConfig(
      nonce = Nonce.random,
      continueUrl = "https://tax.service.gov.uk/continue-url",
      backlinkUrl = "http://localhost:1234/backlink-url",
      callbackUrl = "https://cds.public.mdtp:443/result-post-url",
      maximumNumberOfFiles = 13,
      maximumFileSizeBytes = 13 * 1024
    )
  )

  val maxFileUploadsNumber = fileUploadContext.config.maximumNumberOfFiles
  val maximumFileSizeBytes = fileUploadContext.config.maximumFileSizeBytes

  "FileUploadJourneyModel" when {
    "at state Initialized" should {
      "go to UploadSingleFile when initiateFileUpload" in {
        val mockUpscanInitiate: (String, UpscanInitiateRequest) => Future[UpscanInitiateResponse] =
          (serviceId, request) =>
            Future.successful(
              UpscanInitiateResponse(
                reference = "foo-bar-ref",
                uploadRequest =
                  UploadRequest(href = "https://s3.bucket", fields = Map("callbackUrl" -> request.callbackUrl))
              )
            )
        val upscanRequest = (nonce: String, maxFileSize: Long) =>
          UpscanInitiateRequest(
            "https://foo.bar/callback",
            Some("https://foo.bar/success"),
            Some("https://foo.bar/failure"),
            Some(maxFileSize.toInt)
          )
        given(
          Initialized(fileUploadContext, FileUploads())
        ) when initiateFileUpload(upscanRequest)(mockUpscanInitiate) should thenGo(
          UploadSingleFile(
            fileUploadContext,
            "foo-bar-ref",
            UploadRequest(href = "https://s3.bucket", fields = Map("callbackUrl" -> "https://foo.bar/callback")),
            FileUploads(files = Seq(FileUpload.Initiated(Nonce.Any, Timestamp.Any, "foo-bar-ref")))
          )
        )
      }

      "go to UploadSingleFile when initiateFileUpload and uploads not empty and showYesNoQuestionBeforeContinue" in {
        val mockUpscanInitiate: (String, UpscanInitiateRequest) => Future[UpscanInitiateResponse] =
          (serviceId, request) =>
            Future.successful(
              UpscanInitiateResponse(
                reference = "foo-bar-ref",
                uploadRequest =
                  UploadRequest(href = "https://s3.bucket", fields = Map("callbackUrl" -> request.callbackUrl))
              )
            )
        val upscanRequest = (nonce: String, maxFileSize: Long) =>
          UpscanInitiateRequest(
            "https://foo.bar/callback",
            Some("https://foo.bar/success"),
            Some("https://foo.bar/failure"),
            Some(maxFileSize.toInt)
          )
        val context = fileUploadContext.copy(config =
          fileUploadContext.config
            .copy(features = Features(showYesNoQuestionBeforeContinue = true))
        )
        given(
          Initialized(
            context,
            nonEmptyFileUploads
          )
        ) when initiateFileUpload(upscanRequest)(mockUpscanInitiate) should thenGo(
          UploadSingleFile(
            context,
            "foo-bar-ref",
            UploadRequest(href = "https://s3.bucket", fields = Map("callbackUrl" -> "https://foo.bar/callback")),
            FileUploads(files =
              nonEmptyFileUploads.files ++ Seq(FileUpload.Initiated(Nonce.Any, Timestamp.Any, "foo-bar-ref"))
            )
          )
        )
      }

      "go to Summary when initiateFileUpload and uploads not empty and not showYesNoQuestionBeforeContinue" in {
        val mockUpscanInitiate: (String, UpscanInitiateRequest) => Future[UpscanInitiateResponse] =
          (serviceId, request) =>
            Future.successful(
              UpscanInitiateResponse(
                reference = "foo-bar-ref",
                uploadRequest =
                  UploadRequest(href = "https://s3.bucket", fields = Map("callbackUrl" -> request.callbackUrl))
              )
            )
        val upscanRequest = (nonce: String, maxFileSize: Long) =>
          UpscanInitiateRequest(
            "https://foo.bar/callback",
            Some("https://foo.bar/success"),
            Some("https://foo.bar/failure"),
            Some(maxFileSize.toInt)
          )
        val context = fileUploadContext.copy(config =
          fileUploadContext.config
            .copy(features = Features(showYesNoQuestionBeforeContinue = false))
        )
        given(
          Initialized(
            context,
            nonEmptyFileUploads
          )
        ) when initiateFileUpload(upscanRequest)(mockUpscanInitiate) should thenGo(
          Summary(
            fileUploadContext,
            nonEmptyFileUploads,
            acknowledged = false
          )
        )
      }

      "go back to Summary when backToFileUploaded and non-empty file uploads" in {
        given(Initialized(fileUploadContext, nonEmptyFileUploads))
          .when(backToSummary)
          .thenGoes(
            Summary(
              fileUploadContext,
              nonEmptyFileUploads,
              acknowledged = true
            )
          )
      }

      "go back to ContinueToHost when backToFileUploaded and empty file uploads" in {
        given(Initialized(fileUploadContext, FileUploads()))
          .when(backToSummary)
          .thenGoes(ContinueToHost(fileUploadContext, FileUploads()))
      }

      "go to UploadMultipleFiles when toUploadMultipleFiles transition" in {
        given(
          Initialized(
            fileUploadContext,
            nonEmptyFileUploads
          )
        ) when toUploadMultipleFiles(preferUploadMultipleFiles = true) should thenGo(
          UploadMultipleFiles(
            fileUploadContext,
            nonEmptyFileUploads
          )
        )
      }

      "go to Unitialized when wipeOut transition" in
        given(
          Initialized(
            fileUploadContext,
            nonEmptyFileUploads
          )
        ).when(wipeOut).thenGoes(Uninitialized)

      "go to SwitchToSingleFileUpload when toUploadMultipleFiles transition and preferUploadMutipleFiles is false" in {
        given(
          Initialized(
            fileUploadContext,
            nonEmptyFileUploads
          )
        ) when toUploadMultipleFiles(preferUploadMultipleFiles = false) should thenGo(
          SwitchToUploadSingleFile(
            fileUploadContext,
            Some(nonEmptyFileUploads)
          )
        )
      }
    }

    "at state UploadMultipleFiles" should {
      "go back to Initialized when initialize" in {
        given(UploadMultipleFiles(fileUploadContext, nonEmptyFileUploads))
          .when(
            initialize(HostService.Any)(
              FileUploadInitializationRequest(fileUploadContext.config, nonEmptyFileUploads.toUploadedFiles)
            )
          )
          .thenGoes(
            Initialized(fileUploadContext, nonEmptyFileUploads)
          )
      }

      "go to Unitialized when wipeOut transition" in
        given(UploadMultipleFiles(fileUploadContext, nonEmptyFileUploads)).when(wipeOut).thenGoes(Uninitialized)

      "fail when initialization with invalid config" in {
        val invalidContext = fileUploadContext.copy(config = fileUploadContext.config.copy(callbackUrl = ""))
        given(
          UploadMultipleFiles(
            invalidContext,
            nonEmptyFileUploads
          )
        )
          .when(
            initialize(HostService.Any)(
              FileUploadInitializationRequest(invalidContext.config, nonEmptyFileUploads.toUploadedFiles)
            )
          )
          .thenFailsWith[Exception]
      }

      "fail when initialization without user agent" in {
        given(
          UploadMultipleFiles(
            fileUploadContext,
            nonEmptyFileUploads
          )
        )
          .when(
            initialize(HostService.InitializationRequestHeaders(Seq.empty))(
              FileUploadInitializationRequest(fileUploadContext.config, nonEmptyFileUploads.toUploadedFiles)
            )
          )
          .thenFailsWith[Exception]
      }

      "go to ContinueToHost when non-empty file uploads and continueToHost" in {
        given(
          UploadMultipleFiles(fileUploadContext, nonEmptyFileUploads)
        ) when continueToHost should thenGo(
          ContinueToHost(fileUploadContext, nonEmptyFileUploads)
        )
      }

      "go to ContinueToHost when empty file uploads and continueToHost transition" in {
        given(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads()
          )
        ) when continueToHost should thenGo(
          ContinueToHost(
            fileUploadContext,
            FileUploads()
          )
        )
      }

      "go to Initialized when non-empty file uploads and continue with yes" in {
        given(
          UploadMultipleFiles(fileUploadContext, nonEmptyFileUploads)
        ) when continueWithYesNo(true) should thenGo(
          Initialized(fileUploadContext, nonEmptyFileUploads)
        )
      }

      "go to Initialized when empty file uploads and continue with yes transition" in {
        given(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads()
          )
        ) when continueWithYesNo(true) should thenGo(
          Initialized(
            fileUploadContext,
            FileUploads()
          )
        )
      }

      "go to ContinueToHost when non-empty file uploads and continue with no" in {
        given(
          UploadMultipleFiles(fileUploadContext, nonEmptyFileUploads)
        ) when continueWithYesNo(false) should thenGo(
          ContinueToHost(fileUploadContext, nonEmptyFileUploads)
        )
      }

      "go to ContinueToHost when empty file uploads and continue with no transition" in {
        given(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads()
          )
        ) when continueWithYesNo(false) should thenGo(
          ContinueToHost(
            fileUploadContext,
            FileUploads()
          )
        )
      }

      "stay and filter accepted uploads when toUploadMultipleFiles transition" in {
        given(
          UploadMultipleFiles(
            fileUploadContext,
            nonEmptyFileUploads + FileUpload.Initiated(Nonce.Any, Timestamp.Any, "foo-2") + FileUpload
              .Posted(Nonce.Any, Timestamp.Any, "foo-3") + FileUpload.Accepted(
              Nonce(4),
              Timestamp.Any,
              "foo-bar-ref-4",
              "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
              ZonedDateTime.parse("2018-04-24T09:30:00Z"),
              "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
              "test.pdf",
              "application/pdf",
              4567890
            )
          )
        ) when toUploadMultipleFiles() should thenGo(
          UploadMultipleFiles(
            fileUploadContext,
            nonEmptyFileUploads + FileUpload.Accepted(
              Nonce(4),
              Timestamp.Any,
              "foo-bar-ref-4",
              "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
              ZonedDateTime.parse("2018-04-24T09:30:00Z"),
              "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
              "test.pdf",
              "application/pdf",
              4567890
            )
          )
        )
      }

      "initiate new file upload when initiateNextFileUpload transition and empty uploads" in {
        given(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads()
          )
        ) when initiateNextFileUpload("001")(testUpscanRequest)(mockUpscanInitiate) should thenGo(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads() +
              FileUpload.Initiated(
                Nonce.Any,
                Timestamp.Any,
                "foo-bar-ref",
                uploadId = Some("001"),
                uploadRequest = Some(someUploadRequest(testUpscanRequest("", maximumFileSizeBytes)))
              )
          )
        )
      }

      "initiate new file upload when initiateNextFileUpload transition and some uploads exist already" in {
        val fileUploads = FileUploads(files =
          (0 until (maxFileUploadsNumber - 1))
            .map(i => FileUpload.Initiated(Nonce(i), Timestamp.Any, s"foo-bar-ref-$i", uploadId = Some(s"0$i")))
        ) + FileUpload.Rejected(Nonce(9), Timestamp.Any, "foo-bar-ref-9", S3UploadError("a", "b", "c"))
        given(
          UploadMultipleFiles(
            fileUploadContext,
            fileUploads
          )
        ) when initiateNextFileUpload("001")(testUpscanRequest)(mockUpscanInitiate) should thenGo(
          UploadMultipleFiles(
            fileUploadContext,
            fileUploads +
              FileUpload.Initiated(
                Nonce.Any,
                Timestamp.Any,
                "foo-bar-ref",
                uploadId = Some("001"),
                uploadRequest = Some(someUploadRequest(testUpscanRequest("", maximumFileSizeBytes)))
              )
          )
        )
      }

      "do nothing when initiateNextFileUpload with existing uploadId" in {
        given(
          UploadMultipleFiles(
            fileUploadContext,
            nonEmptyFileUploads +
              FileUpload.Initiated(Nonce.Any, Timestamp.Any, "foo-bar-ref", uploadId = Some("101"))
          )
        ) when initiateNextFileUpload("101")(testUpscanRequest)(mockUpscanInitiate) should thenGo(
          UploadMultipleFiles(
            fileUploadContext,
            nonEmptyFileUploads +
              FileUpload.Initiated(Nonce.Any, Timestamp.Any, "foo-bar-ref", uploadId = Some("101"))
          )
        )
      }

      "do nothing when initiateNextFileUpload and maximum number of uploads already reached" in {
        val fileUploads = FileUploads(files =
          (0 until maxFileUploadsNumber)
            .map(i => FileUpload.Initiated(Nonce(i), Timestamp.Any, s"foo-bar-ref-$i", uploadId = Some(s"0$i")))
        )
        given(
          UploadMultipleFiles(
            fileUploadContext,
            fileUploads
          )
        ) when initiateNextFileUpload("101")(testUpscanRequest)(mockUpscanInitiate) should thenGo(
          UploadMultipleFiles(
            fileUploadContext,
            fileUploads
          )
        )
      }

      "mark file upload as POSTED when markUploadAsPosted transition" in {
        given(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
              )
            )
          )
        ) when markUploadAsPosted(S3UploadSuccess("foo-bar-ref-2", Some("bucket-123"))) should thenGo(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Posted(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
              )
            )
          )
        )
      }

      "do nothing when markUploadAsPosted transition and already in POSTED state" in {
        val state = UploadMultipleFiles(
          fileUploadContext,
          FileUploads(files =
            Seq(
              FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
              FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
              FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
            )
          )
        )
        given(state) when markUploadAsPosted(S3UploadSuccess("foo-bar-ref-1", Some("bucket-123"))) should thenGo(state)
      }

      "overwrite upload status when markUploadAsPosted transition and already in ACCEPTED state" in {
        given(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c")),
                FileUpload.Accepted(
                  Nonce(4),
                  Timestamp.Any,
                  "foo-bar-ref-4",
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
        ) when markUploadAsPosted(S3UploadSuccess("foo-bar-ref-4", Some("bucket-123"))) should thenGo(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c")),
                FileUpload.Posted(Nonce(4), Timestamp.Any, "foo-bar-ref-4")
              )
            )
          )
        )
      }

      "do not overwrite upload status when markUploadAsPosted transition and already in ACCEPTED state but timestamp gap is too small" in {
        val state = UploadMultipleFiles(
          fileUploadContext,
          FileUploads(files =
            Seq(
              FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
              FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
              FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c")),
              FileUpload.Accepted(
                Nonce(4),
                Timestamp.now,
                "foo-bar-ref-4",
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
        given(state) when markUploadAsPosted(S3UploadSuccess("foo-bar-ref-4", Some("bucket-123"))) should thenGo(state)
      }

      "do nothing when markUploadAsPosted transition and none matching upload exist" in {
        val state = UploadMultipleFiles(
          fileUploadContext,
          FileUploads(files =
            Seq(
              FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
              FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
              FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
            )
          )
        )
        given(state) when markUploadAsPosted(S3UploadSuccess("foo-bar-ref-4", Some("bucket-123"))) should thenGo(state)
      }

      "mark file upload as REJECTED when markUploadAsRejected transition" in {
        given(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
              )
            )
          )
        ) when markUploadAsRejected(
          S3UploadError("foo-bar-ref-2", "errorCode1", "errorMessage2")
        ) should thenGo(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Rejected(
                  Nonce(2),
                  Timestamp.Any,
                  "foo-bar-ref-2",
                  S3UploadError("foo-bar-ref-2", "errorCode1", "errorMessage2")
                ),
                FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
              )
            )
          )
        )
      }

      "overwrite upload status when markUploadAsRejected transition and already in REJECTED state" in {
        given(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
              )
            )
          )
        ) when markUploadAsRejected(
          S3UploadError("foo-bar-ref-3", "errorCode1", "errorMessage2")
        ) should thenGo(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(
                  Nonce(3),
                  Timestamp.Any,
                  "foo-bar-ref-3",
                  S3UploadError("foo-bar-ref-3", "errorCode1", "errorMessage2")
                )
              )
            )
          )
        )
      }

      "overwrite upload status when markUploadAsRejected transition and already in ACCEPTED state" in {
        given(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c")),
                FileUpload.Accepted(
                  Nonce(4),
                  Timestamp.Any,
                  "foo-bar-ref-4",
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
        ) when markUploadAsRejected(
          S3UploadError("foo-bar-ref-4", "errorCode1", "errorMessage2")
        ) should thenGo(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c")),
                FileUpload.Rejected(
                  Nonce(4),
                  Timestamp.Any,
                  "foo-bar-ref-4",
                  S3UploadError("foo-bar-ref-4", "errorCode1", "errorMessage2")
                )
              )
            )
          )
        )
      }

      "do nothing when markUploadAsRejected transition and none matching file upload found" in {
        val state = UploadMultipleFiles(
          fileUploadContext,
          FileUploads(files =
            Seq(
              FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
              FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
              FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
            )
          )
        )
        given(state) when markUploadAsRejected(
          S3UploadError("foo-bar-ref-4", "errorCode1", "errorMessage2")
        ) should thenGo(state)
      }

      "update file upload status to ACCEPTED when positive upscanCallbackArrived transition" in {
        val mockPushFileUploadResult: FileUploadResultPushApi =
          _ => Future.successful(FileUploadResultPushConnector.SuccessResponse)
        given(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
              )
            )
          )
        ) when upscanCallbackArrived(mockPushFileUploadResult)(Nonce(1))(
          UpscanFileReady(
            reference = "foo-bar-ref-1",
            downloadUrl = "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
            uploadDetails = UpscanNotification.UploadDetails(
              uploadTimestamp = ZonedDateTime.parse("2018-04-24T09:30:00Z"),
              checksum = "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
              fileName = "test.pdf",
              fileMimeType = "application/pdf",
              size = 4567890
            )
          )
        ) should thenGo(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                acceptedFileUpload,
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
              )
            )
          )
        )
      }

      "do not update file upload status to ACCEPTED if push failure" in {
        val mockPushFileUploadResult: FileUploadResultPushApi =
          _ => Future.successful(Left(FileUploadResultPushConnector.Error(500, "")))
        given(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
              )
            )
          )
        ).when(
          upscanCallbackArrived(mockPushFileUploadResult)(Nonce(1))(
            UpscanFileReady(
              reference = "foo-bar-ref-1",
              downloadUrl = "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
              uploadDetails = UpscanNotification.UploadDetails(
                uploadTimestamp = ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                checksum = "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                fileName = "test.pdf",
                fileMimeType = "application/pdf",
                size = 4567890
              )
            )
          )
        ).thenFailsWith[Exception]
      }

      "update file upload status to ACCEPTED with sanitized name of the file" in {
        val mockPushFileUploadResult: FileUploadResultPushApi =
          _ => Future.successful(FileUploadResultPushConnector.SuccessResponse)
        given(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
              )
            )
          )
        ) when upscanCallbackArrived(mockPushFileUploadResult)(Nonce(1))(
          UpscanFileReady(
            reference = "foo-bar-ref-1",
            downloadUrl = "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
            uploadDetails = UpscanNotification.UploadDetails(
              uploadTimestamp = ZonedDateTime.parse("2018-04-24T09:30:00Z"),
              checksum = "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
              fileName = """F:\My Documents\my invoices\invoice00001_1234.pdf""",
              fileMimeType = "application/pdf",
              size = 4567890
            )
          )
        ) should thenGo(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Accepted(
                  Nonce(1),
                  Timestamp.Any,
                  "foo-bar-ref-1",
                  "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                  ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                  "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                  "invoice00001_1234.pdf",
                  "application/pdf",
                  4567890
                ),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
              )
            )
          )
        )
      }

      "do nothing when positive upscanCallbackArrived transition and none matching file upload found" in {
        val mockPushFileUploadResult: FileUploadResultPushApi =
          _ => Future.successful(FileUploadResultPushConnector.SuccessResponse)
        val state = UploadMultipleFiles(
          fileUploadContext,
          FileUploads(files =
            Seq(
              FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
              FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
              FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
            )
          )
        )
        given(state) when upscanCallbackArrived(mockPushFileUploadResult)(Nonce(4))(
          UpscanFileReady(
            reference = "foo-bar-ref-4",
            downloadUrl = "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
            uploadDetails = UpscanNotification.UploadDetails(
              uploadTimestamp = ZonedDateTime.parse("2018-04-24T09:30:00Z"),
              checksum = "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
              fileName = "test.pdf",
              fileMimeType = "application/pdf",
              size = 4567890
            )
          )
        ) should thenGo(state)
      }

      "overwrite status when positive upscanCallbackArrived transition and file upload already in ACCEPTED state" in {
        val mockPushFileUploadResult: FileUploadResultPushApi =
          _ => Future.successful(FileUploadResultPushConnector.SuccessResponse)
        given(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Accepted(
                  Nonce(1),
                  Timestamp.Any,
                  "foo-bar-ref-1",
                  "https://bucketName.s3.eu-west-2.amazonaws.com?0035699",
                  ZonedDateTime.parse("2018-04-24T09:28:00Z"),
                  "786f101dd52e8b2ace0dcf5ed09b1d1ba30e608938510ce46e7a5c7a4e775189",
                  "test.png",
                  "image/png",
                  4567890
                ),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
              )
            )
          )
        ) when upscanCallbackArrived(mockPushFileUploadResult)(Nonce(1))(
          UpscanFileReady(
            reference = "foo-bar-ref-1",
            downloadUrl = "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
            uploadDetails = UpscanNotification.UploadDetails(
              uploadTimestamp = ZonedDateTime.parse("2018-04-24T09:30:00Z"),
              checksum = "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
              fileName = "test.pdf",
              fileMimeType = "application/pdf",
              size = 4567890
            )
          )
        ) should thenGo(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                acceptedFileUpload,
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
              )
            )
          )
        )
      }

      "do not overwrite status when positive upscanCallbackArrived transition and file upload already in ACCEPTED state if timestamp gap is to small" in {
        val now = Timestamp.now
        val mockPushFileUploadResult: FileUploadResultPushApi =
          _ => Future.successful(FileUploadResultPushConnector.SuccessResponse)
        val state = UploadMultipleFiles(
          fileUploadContext,
          FileUploads(files =
            Seq(
              FileUpload.Accepted(
                Nonce(1),
                now,
                "foo-bar-ref-1",
                "https://bucketName.s3.eu-west-2.amazonaws.com?0035699",
                ZonedDateTime.parse("2018-04-24T09:28:00Z"),
                "786f101dd52e8b2ace0dcf5ed09b1d1ba30e608938510ce46e7a5c7a4e775189",
                "test.png",
                "image/png",
                4567890
              ),
              FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
              FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
            )
          )
        )
        given(state) when upscanCallbackArrived(mockPushFileUploadResult)(Nonce(1))(
          UpscanFileReady(
            reference = "foo-bar-ref-1",
            downloadUrl = "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
            uploadDetails = UpscanNotification.UploadDetails(
              uploadTimestamp = ZonedDateTime.parse("2018-04-24T09:30:00Z"),
              checksum = "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
              fileName = "test.pdf",
              fileMimeType = "application/pdf",
              size = 4567890
            )
          )
        ) should thenGo(state)
      }

      "overwrite upload status when positive upscanCallbackArrived transition and file upload already in REJECTED state" in {
        val mockPushFileUploadResult: FileUploadResultPushApi =
          _ => Future.successful(FileUploadResultPushConnector.SuccessResponse)
        given(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Rejected(Nonce(1), Timestamp.Any, "foo-bar-ref-1", S3UploadError("a", "b", "c")),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
              )
            )
          )
        ) when upscanCallbackArrived(mockPushFileUploadResult)(Nonce(1))(
          UpscanFileReady(
            reference = "foo-bar-ref-1",
            downloadUrl = "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
            uploadDetails = UpscanNotification.UploadDetails(
              uploadTimestamp = ZonedDateTime.parse("2018-04-24T09:30:00Z"),
              checksum = "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
              fileName = "test.pdf",
              fileMimeType = "application/pdf",
              size = 4567890
            )
          )
        ) should thenGo(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                acceptedFileUpload,
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
              )
            )
          )
        )
      }

      "overwrite upload status when positive upscanCallbackArrived transition and file upload already in FAILED state" in {
        val mockPushFileUploadResult: FileUploadResultPushApi =
          _ => Future.successful(FileUploadResultPushConnector.SuccessResponse)
        given(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Failed(
                  Nonce(1),
                  Timestamp.Any,
                  "foo-bar-ref-1",
                  UpscanNotification.FailureDetails(
                    failureReason = UpscanNotification.QUARANTINE,
                    message = "e.g. This file has a virus"
                  )
                ),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
              )
            )
          )
        ) when upscanCallbackArrived(mockPushFileUploadResult)(Nonce(1))(
          UpscanFileReady(
            reference = "foo-bar-ref-1",
            downloadUrl = "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
            uploadDetails = UpscanNotification.UploadDetails(
              uploadTimestamp = ZonedDateTime.parse("2018-04-24T09:30:00Z"),
              checksum = "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
              fileName = "test.pdf",
              fileMimeType = "application/pdf",
              size = 4567890
            )
          )
        ) should thenGo(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                acceptedFileUpload,
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
              )
            )
          )
        )
      }

      "update file upload status to FAILED when negative upscanCallbackArrived transition" in {
        val mockPushFileUploadResult: FileUploadResultPushApi =
          _ => Future.successful(FileUploadResultPushConnector.SuccessResponse)
        given(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
              )
            )
          )
        ) when upscanCallbackArrived(mockPushFileUploadResult)(Nonce(1))(
          UpscanFileFailed(
            reference = "foo-bar-ref-1",
            failureDetails = UpscanNotification.FailureDetails(
              failureReason = UpscanNotification.QUARANTINE,
              message = "e.g. This file has a virus"
            )
          )
        ) should thenGo(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Failed(
                  Nonce(1),
                  Timestamp.Any,
                  "foo-bar-ref-1",
                  UpscanNotification.FailureDetails(
                    failureReason = UpscanNotification.QUARANTINE,
                    message = "e.g. This file has a virus"
                  )
                ),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
              )
            )
          )
        )
      }

      "do nothing when negative upscanCallbackArrived transition and none matching file upload found" in {
        val mockPushFileUploadResult: FileUploadResultPushApi =
          _ => Future.successful(FileUploadResultPushConnector.SuccessResponse)
        val state = UploadMultipleFiles(
          fileUploadContext,
          FileUploads(files =
            Seq(
              FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
              FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
              FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
            )
          )
        )
        given(state) when upscanCallbackArrived(mockPushFileUploadResult)(Nonce(4))(
          UpscanFileFailed(
            reference = "foo-bar-ref-4",
            failureDetails = UpscanNotification.FailureDetails(
              failureReason = UpscanNotification.QUARANTINE,
              message = "e.g. This file has a virus"
            )
          )
        ) should thenGo(state)
      }

      "overwrite upload status when negative upscanCallbackArrived transition and upload already in FAILED state" in {
        val mockPushFileUploadResult: FileUploadResultPushApi =
          _ => Future.successful(FileUploadResultPushConnector.SuccessResponse)
        given(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Failed(
                  Nonce(1),
                  Timestamp.Any,
                  "foo-bar-ref-1",
                  UpscanNotification.FailureDetails(
                    failureReason = UpscanNotification.REJECTED,
                    message = "e.g. This file has wrong type"
                  )
                ),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
              )
            )
          )
        ) when upscanCallbackArrived(mockPushFileUploadResult)(Nonce(1))(
          UpscanFileFailed(
            reference = "foo-bar-ref-1",
            failureDetails = UpscanNotification.FailureDetails(
              failureReason = UpscanNotification.QUARANTINE,
              message = "e.g. This file has a virus"
            )
          )
        ) should thenGo(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Failed(
                  Nonce(1),
                  Timestamp.Any,
                  "foo-bar-ref-1",
                  UpscanNotification.FailureDetails(
                    failureReason = UpscanNotification.QUARANTINE,
                    message = "e.g. This file has a virus"
                  )
                ),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
              )
            )
          )
        )
      }

      "overwrite upload status when negative upscanCallbackArrived transition and upload already in ACCEPTED state" in {
        val mockPushFileUploadResult: FileUploadResultPushApi =
          _ => Future.successful(FileUploadResultPushConnector.SuccessResponse)
        given(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Accepted(
                  Nonce(1),
                  Timestamp.Any,
                  "foo-bar-ref-1",
                  "https://bucketName.s3.eu-west-2.amazonaws.com?0035699",
                  ZonedDateTime.parse("2018-04-24T09:28:00Z"),
                  "786f101dd52e8b2ace0dcf5ed09b1d1ba30e608938510ce46e7a5c7a4e775189",
                  "test.png",
                  "image/png",
                  4567890
                ),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
              )
            )
          )
        ) when upscanCallbackArrived(mockPushFileUploadResult)(Nonce(1))(
          UpscanFileFailed(
            reference = "foo-bar-ref-1",
            failureDetails = UpscanNotification.FailureDetails(
              failureReason = UpscanNotification.QUARANTINE,
              message = "e.g. This file has a virus"
            )
          )
        ) should thenGo(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Failed(
                  Nonce(1),
                  Timestamp.Any,
                  "foo-bar-ref-1",
                  UpscanNotification.FailureDetails(
                    failureReason = UpscanNotification.QUARANTINE,
                    message = "e.g. This file has a virus"
                  )
                ),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(Nonce(3), Timestamp.Any, "foo-bar-ref-3", S3UploadError("a", "b", "c"))
              )
            )
          )
        )
      }

      "remove file upload when removeFileUploadByReference transition and reference exists" in {
        val mockPushFileUploadResult: FileUploadResultPushApi =
          _ => Future.successful(FileUploadResultPushConnector.SuccessResponse)
        given(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Accepted(
                  Nonce(3),
                  Timestamp.Any,
                  "foo-bar-ref-3",
                  "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                  ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                  "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                  "test.pdf",
                  "application/pdf",
                  4567890
                ),
                FileUpload.Rejected(Nonce(4), Timestamp.Any, "foo-bar-ref-4", S3UploadError("a", "b", "c"))
              )
            )
          )
        ) when removeFileUploadByReference("foo-bar-ref-3")(testUpscanRequest)(mockUpscanInitiate)(
          mockPushFileUploadResult
        ) should thenGo(
          UploadMultipleFiles(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Rejected(Nonce(4), Timestamp.Any, "foo-bar-ref-4", S3UploadError("a", "b", "c"))
              )
            )
          )
        )
      }

      "do nothing when removeFileUploadByReference transition and none file upload matches" in {
        val mockPushFileUploadResult: FileUploadResultPushApi =
          _ => Future.successful(FileUploadResultPushConnector.SuccessResponse)
        val state = UploadMultipleFiles(
          fileUploadContext,
          FileUploads(files =
            Seq(
              FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
              FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
              FileUpload.Accepted(
                Nonce(3),
                Timestamp.Any,
                "foo-bar-ref-3",
                "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                "test.pdf",
                "application/pdf",
                4567890
              ),
              FileUpload.Rejected(Nonce(4), Timestamp.Any, "foo-bar-ref-4", S3UploadError("a", "b", "c"))
            )
          )
        )
        given(state) when removeFileUploadByReference("foo-bar-ref-5")(testUpscanRequest)(mockUpscanInitiate)(
          mockPushFileUploadResult
        ) should thenGo(state)
      }
    }

    "at state UploadFile" should {
      "go to Unitialized when wipeOut transition" in
        given(
          UploadSingleFile(
            fileUploadContext,
            "foo-bar-ref-2",
            UploadRequest(
              href = "https://s3.bucket",
              fields = Map(
                "callbackUrl"     -> "https://foo.bar/callback",
                "successRedirect" -> "https://foo.bar/success",
                "errorRedirect"   -> "https://foo.bar/failure"
              )
            ),
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Accepted(
                  Nonce(3),
                  Timestamp.Any,
                  "foo-bar-ref-3",
                  "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                  ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                  "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                  "test.pdf",
                  "application/pdf",
                  4567890
                ),
                FileUpload.Failed(
                  Nonce(4),
                  Timestamp.Any,
                  "foo-bar-ref-4",
                  UpscanNotification.FailureDetails(UpscanNotification.REJECTED, "some failure reason")
                )
              )
            )
          )
        ).when(wipeOut).thenGoes(Uninitialized)

      "go to WaitingForFileVerification when waitForFileVerification and not verified yet" in {
        given(
          UploadSingleFile(
            fileUploadContext,
            "foo-bar-ref-2",
            UploadRequest(
              href = "https://s3.bucket",
              fields = Map(
                "callbackUrl"     -> "https://foo.bar/callback",
                "successRedirect" -> "https://foo.bar/success",
                "errorRedirect"   -> "https://foo.bar/failure"
              )
            ),
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Accepted(
                  Nonce(3),
                  Timestamp.Any,
                  "foo-bar-ref-3",
                  "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                  ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                  "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                  "test.pdf",
                  "application/pdf",
                  4567890
                ),
                FileUpload.Failed(
                  Nonce(4),
                  Timestamp.Any,
                  "foo-bar-ref-4",
                  UpscanNotification.FailureDetails(UpscanNotification.REJECTED, "some failure reason")
                )
              )
            )
          )
        ) when waitForFileVerification should thenGo(
          WaitingForFileVerification(
            fileUploadContext,
            "foo-bar-ref-2",
            UploadRequest(
              href = "https://s3.bucket",
              fields = Map(
                "callbackUrl"     -> "https://foo.bar/callback",
                "successRedirect" -> "https://foo.bar/success",
                "errorRedirect"   -> "https://foo.bar/failure"
              )
            ),
            FileUpload.Posted(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Posted(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Accepted(
                  Nonce(3),
                  Timestamp.Any,
                  "foo-bar-ref-3",
                  "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                  ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                  "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                  "test.pdf",
                  "application/pdf",
                  4567890
                ),
                FileUpload.Failed(
                  Nonce(4),
                  Timestamp.Any,
                  "foo-bar-ref-4",
                  UpscanNotification.FailureDetails(UpscanNotification.REJECTED, "some failure reason")
                )
              )
            )
          )
        )
      }

      "go to Summary when waitForFileVerification and accepted already" in {
        given(
          UploadSingleFile(
            fileUploadContext,
            "foo-bar-ref-3",
            UploadRequest(
              href = "https://s3.bucket",
              fields = Map(
                "callbackUrl"     -> "https://foo.bar/callback",
                "successRedirect" -> "https://foo.bar/success",
                "errorRedirect"   -> "https://foo.bar/failure"
              )
            ),
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Accepted(
                  Nonce(3),
                  Timestamp.Any,
                  "foo-bar-ref-3",
                  "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                  ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                  "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                  "test.pdf",
                  "application/pdf",
                  4567890
                ),
                FileUpload.Failed(
                  Nonce(4),
                  Timestamp.Any,
                  "foo-bar-ref-4",
                  UpscanNotification.FailureDetails(UpscanNotification.REJECTED, "some failure reason")
                )
              )
            )
          )
        ) when waitForFileVerification should thenGo(
          Summary(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Accepted(
                  Nonce(3),
                  Timestamp.Any,
                  "foo-bar-ref-3",
                  "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                  ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                  "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                  "test.pdf",
                  "application/pdf",
                  4567890
                ),
                FileUpload.Failed(
                  Nonce(4),
                  Timestamp.Any,
                  "foo-bar-ref-4",
                  UpscanNotification.FailureDetails(UpscanNotification.REJECTED, "some failure reason")
                )
              )
            )
          )
        )
      }

      "go to UploadSingleFile when waitForFileVerification and file upload already rejected" in {
        given(
          UploadSingleFile(
            fileUploadContext,
            "foo-bar-ref-4",
            UploadRequest(
              href = "https://s3.bucket",
              fields = Map(
                "callbackUrl"     -> "https://foo.bar/callback",
                "successRedirect" -> "https://foo.bar/success",
                "errorRedirect"   -> "https://foo.bar/failure"
              )
            ),
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Accepted(
                  Nonce(3),
                  Timestamp.Any,
                  "foo-bar-ref-3",
                  "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                  ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                  "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                  "test.pdf",
                  "application/pdf",
                  4567890
                ),
                FileUpload.Failed(
                  Nonce(4),
                  Timestamp.Any,
                  "foo-bar-ref-4",
                  UpscanNotification.FailureDetails(UpscanNotification.REJECTED, "some failure reason")
                )
              )
            )
          )
        ) when waitForFileVerification should thenGo(
          UploadSingleFile(
            fileUploadContext,
            "foo-bar-ref-4",
            UploadRequest(
              href = "https://s3.bucket",
              fields = Map(
                "callbackUrl"     -> "https://foo.bar/callback",
                "successRedirect" -> "https://foo.bar/success",
                "errorRedirect"   -> "https://foo.bar/failure"
              )
            ),
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Initiated(Nonce(2), Timestamp.Any, "foo-bar-ref-2"),
                FileUpload.Accepted(
                  Nonce(3),
                  Timestamp.Any,
                  "foo-bar-ref-3",
                  "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                  ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                  "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                  "test.pdf",
                  "application/pdf",
                  4567890
                ),
                FileUpload.Failed(
                  Nonce(4),
                  Timestamp.Any,
                  "foo-bar-ref-4",
                  UpscanNotification.FailureDetails(UpscanNotification.REJECTED, "some failure reason")
                )
              )
            ),
            Some(
              FileVerificationFailed(
                UpscanNotification.FailureDetails(UpscanNotification.REJECTED, "some failure reason")
              )
            )
          )
        )
      }

      "go to Summary when upscanCallbackArrived and accepted, and reference matches" in {
        val mockPushFileUploadResult: FileUploadResultPushApi =
          _ => Future.successful(FileUploadResultPushConnector.SuccessResponse)
        given(
          UploadSingleFile(
            fileUploadContext,
            "foo-bar-ref-1",
            UploadRequest(
              href = "https://s3.bucket",
              fields = Map(
                "callbackUrl"     -> "https://foo.bar/callback",
                "successRedirect" -> "https://foo.bar/success",
                "errorRedirect"   -> "https://foo.bar/failure"
              )
            ),
            FileUploads(files = Seq(FileUpload.Initiated(Nonce(1), Timestamp.Any, "foo-bar-ref-1")))
          )
        ) when upscanCallbackArrived(mockPushFileUploadResult)(Nonce(1))(
          UpscanFileReady(
            reference = "foo-bar-ref-1",
            downloadUrl = "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
            uploadDetails = UpscanNotification.UploadDetails(
              uploadTimestamp = ZonedDateTime.parse("2018-04-24T09:30:00Z"),
              checksum = "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
              fileName = "test.pdf",
              fileMimeType = "application/pdf",
              size = 4567890
            )
          )
        ) should thenGo(
          Summary(
            fileUploadContext,
            FileUploads(files = Seq(acceptedFileUpload))
          )
        )
      }

      "go to UploadSingleFile when upscanCallbackArrived and accepted, and reference matches but upload is a duplicate" in {
        val mockPushFileUploadResult: FileUploadResultPushApi =
          _ => Future.successful(FileUploadResultPushConnector.SuccessResponse)
        given(
          UploadSingleFile(
            fileUploadContext,
            "foo-bar-ref-1",
            UploadRequest(
              href = "https://s3.bucket",
              fields = Map(
                "callbackUrl"     -> "https://foo.bar/callback",
                "successRedirect" -> "https://foo.bar/success",
                "errorRedirect"   -> "https://foo.bar/failure"
              )
            ),
            FileUploads(files =
              Seq(
                FileUpload.Initiated(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Accepted(
                  Nonce(2),
                  Timestamp.Any,
                  "foo-bar-ref-2",
                  "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                  ZonedDateTime.parse("2020-04-24T09:30:00Z"),
                  "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                  "test.pdf",
                  "application/pdf",
                  4567890
                )
              )
            )
          )
        ) when upscanCallbackArrived(mockPushFileUploadResult)(Nonce(1))(
          UpscanFileReady(
            reference = "foo-bar-ref-1",
            downloadUrl = "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
            uploadDetails = UpscanNotification.UploadDetails(
              uploadTimestamp = ZonedDateTime.parse("2020-04-24T09:32:13Z"),
              checksum = "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
              fileName = "test2.png",
              fileMimeType = "image/png",
              size = 4567890
            )
          )
        ) should thenGo(
          UploadSingleFile(
            fileUploadContext,
            "foo-bar-ref-1",
            UploadRequest(
              href = "https://s3.bucket",
              fields = Map(
                "callbackUrl"     -> "https://foo.bar/callback",
                "successRedirect" -> "https://foo.bar/success",
                "errorRedirect"   -> "https://foo.bar/failure"
              )
            ),
            FileUploads(files =
              Seq(
                FileUpload.Duplicate(
                  Nonce(1),
                  Timestamp.Any,
                  "foo-bar-ref-1",
                  "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                  "test.pdf",
                  "test2.png"
                ),
                FileUpload.Accepted(
                  Nonce(2),
                  Timestamp.Any,
                  "foo-bar-ref-2",
                  "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                  ZonedDateTime.parse("2020-04-24T09:30:00Z"),
                  "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                  "test.pdf",
                  "application/pdf",
                  4567890
                )
              )
            ),
            Some(
              DuplicateFileUpload(
                "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                "test.pdf",
                "test2.png"
              )
            )
          )
        )
      }

      "go to UploadSingleFile when upscanCallbackArrived and failed, and reference matches" in {
        val mockPushFileUploadResult: FileUploadResultPushApi =
          _ => Future.successful(FileUploadResultPushConnector.SuccessResponse)
        given(
          UploadSingleFile(
            fileUploadContext,
            "foo-bar-ref-1",
            UploadRequest(
              href = "https://s3.bucket",
              fields = Map(
                "callbackUrl"     -> "https://foo.bar/callback",
                "successRedirect" -> "https://foo.bar/success",
                "errorRedirect"   -> "https://foo.bar/failure"
              )
            ),
            FileUploads(files = Seq(FileUpload.Initiated(Nonce(1), Timestamp.Any, "foo-bar-ref-1")))
          )
        ) when upscanCallbackArrived(mockPushFileUploadResult)(Nonce(1))(
          UpscanFileFailed(
            reference = "foo-bar-ref-1",
            failureDetails = UpscanNotification.FailureDetails(
              failureReason = UpscanNotification.UNKNOWN,
              message = "e.g. This file has a virus"
            )
          )
        ) should thenGo(
          UploadSingleFile(
            fileUploadContext,
            "foo-bar-ref-1",
            UploadRequest(
              href = "https://s3.bucket",
              fields = Map(
                "callbackUrl"     -> "https://foo.bar/callback",
                "successRedirect" -> "https://foo.bar/success",
                "errorRedirect"   -> "https://foo.bar/failure"
              )
            ),
            FileUploads(files =
              Seq(
                FileUpload.Failed(
                  Nonce(1),
                  Timestamp.Any,
                  "foo-bar-ref-1",
                  UpscanNotification.FailureDetails(UpscanNotification.UNKNOWN, "e.g. This file has a virus")
                )
              )
            ),
            Some(
              FileVerificationFailed(
                UpscanNotification.FailureDetails(UpscanNotification.UNKNOWN, "e.g. This file has a virus")
              )
            )
          )
        )
      }

      "go to UploadSingleFile with error when fileUploadWasRejected" in {
        val state =
          UploadSingleFile(
            fileUploadContext,
            "foo-bar-ref-1",
            UploadRequest(
              href = "https://s3.bucket",
              fields = Map(
                "callbackUrl"     -> "https://foo.bar/callback",
                "successRedirect" -> "https://foo.bar/success",
                "errorRedirect"   -> "https://foo.bar/failure"
              )
            ),
            FileUploads(files = Seq(FileUpload.Initiated(Nonce(1), Timestamp.Any, "foo-bar-ref-1")))
          )

        given(state) when markUploadAsRejected(
          S3UploadError(
            key = "foo-bar-ref-1",
            errorCode = "a",
            errorMessage = "b",
            errorResource = Some("c"),
            errorRequestId = Some("d")
          )
        ) should thenGo(
          state.copy(
            fileUploads = FileUploads(files =
              Seq(
                FileUpload.Rejected(
                  Nonce(1),
                  Timestamp.Any,
                  "foo-bar-ref-1",
                  S3UploadError(
                    key = "foo-bar-ref-1",
                    errorCode = "a",
                    errorMessage = "b",
                    errorResource = Some("c"),
                    errorRequestId = Some("d")
                  )
                )
              )
            ),
            maybeUploadError = Some(
              FileTransmissionFailed(
                S3UploadError(
                  key = "foo-bar-ref-1",
                  errorCode = "a",
                  errorMessage = "b",
                  errorResource = Some("c"),
                  errorRequestId = Some("d")
                )
              )
            )
          )
        )
      }

      "switch over to UploadMultipleFiles when toUploadMultipleFiles()" in {
        given(
          UploadSingleFile(
            fileUploadContext,
            "foo-bar-ref-4",
            UploadRequest(
              href = "https://s3.bucket",
              fields = Map(
                "callbackUrl"     -> "https://foo.bar/callback",
                "successRedirect" -> "https://foo.bar/success",
                "errorRedirect"   -> "https://foo.bar/failure"
              )
            ),
            nonEmptyFileUploads,
            None
          )
        )
          .when(toUploadMultipleFiles())
          .thenGoes(
            UploadMultipleFiles(
              fileUploadContext,
              nonEmptyFileUploads
            )
          )
      }

      "go to UploadSingleFile when initiateFileUpload and number of uploaded files below the limit" in {
        val hostData = fileUploadContext
        val fileUploads = FileUploads(files =
          for (i <- 0 until (maxFileUploadsNumber - 1))
            yield FileUpload.Accepted(
              Nonce(i),
              Timestamp.Any,
              s"foo-bar-ref-$i",
              "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
              ZonedDateTime.parse("2018-04-24T09:30:00Z"),
              "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
              "test.pdf",
              "application/pdf",
              4567890
            )
        )
        given(UploadMultipleFiles(hostData, fileUploads))
          .when(initiateFileUpload(testUpscanRequest)(mockUpscanInitiate))
          .thenGoes(
            UploadSingleFile(
              hostData,
              "foo-bar-ref",
              someUploadRequest(testUpscanRequest("foo", maximumFileSizeBytes)),
              fileUploads + FileUpload.Initiated(Nonce.Any, Timestamp.Any, "foo-bar-ref")
            )
          )
      }

      "go to Summary when initiateFileUpload and number of uploaded files above the limit" in {
        val hostData = fileUploadContext
        val fileUploads = FileUploads(files =
          for (i <- 0 until maxFileUploadsNumber)
            yield FileUpload.Accepted(
              Nonce(i),
              Timestamp.Any,
              s"foo-bar-ref-$i",
              "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
              ZonedDateTime.parse("2018-04-24T09:30:00Z"),
              "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
              "test.pdf",
              "application/pdf",
              4567890
            )
        )
        given(UploadMultipleFiles(hostData, fileUploads))
          .when(initiateFileUpload(testUpscanRequest)(mockUpscanInitiate))
          .thenGoes(
            Summary(
              hostData,
              fileUploads
            )
          )
      }
    }

    "at state WaitingForFileVerification" should {
      "stay when waitForFileVerification and not verified yet" in {
        val state = WaitingForFileVerification(
          fileUploadContext,
          "foo-bar-ref-1",
          UploadRequest(
            href = "https://s3.bucket",
            fields = Map(
              "callbackUrl"     -> "https://foo.bar/callback",
              "successRedirect" -> "https://foo.bar/success",
              "errorRedirect"   -> "https://foo.bar/failure"
            )
          ),
          FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
          FileUploads(files =
            Seq(
              FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1")
            )
          )
        )
        given(state).when(waitForFileVerification).thenNoChange
      }

      "go to UploadSingleFile when waitForFileVerification and reference unknown" in {
        given(
          WaitingForFileVerification(
            fileUploadContext,
            "foo-bar-ref-2",
            UploadRequest(
              href = "https://s3.bucket",
              fields = Map(
                "callbackUrl"     -> "https://foo.bar/callback",
                "successRedirect" -> "https://foo.bar/success",
                "errorRedirect"   -> "https://foo.bar/failure"
              )
            ),
            FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1")
              )
            )
          )
        ) when waitForFileVerification should thenGo(
          UploadSingleFile(
            fileUploadContext,
            "foo-bar-ref-2",
            UploadRequest(
              href = "https://s3.bucket",
              fields = Map(
                "callbackUrl"     -> "https://foo.bar/callback",
                "successRedirect" -> "https://foo.bar/success",
                "errorRedirect"   -> "https://foo.bar/failure"
              )
            ),
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1")
              )
            )
          )
        )
      }

      "go to Summary when waitForFileVerification and file already accepted" in {
        given(
          WaitingForFileVerification(
            fileUploadContext,
            "foo-bar-ref-1",
            UploadRequest(
              href = "https://s3.bucket",
              fields = Map(
                "callbackUrl"     -> "https://foo.bar/callback",
                "successRedirect" -> "https://foo.bar/success",
                "errorRedirect"   -> "https://foo.bar/failure"
              )
            ),
            FileUpload.Accepted(
              Nonce(1),
              Timestamp.Any,
              "foo-bar-ref-1",
              "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
              ZonedDateTime.parse("2018-04-24T09:30:00Z"),
              "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
              "test.pdf",
              "application/pdf",
              4567890
            ),
            FileUploads(files = Seq(acceptedFileUpload))
          )
        ) when waitForFileVerification should thenGo(
          Summary(
            fileUploadContext,
            FileUploads(files = Seq(acceptedFileUpload))
          )
        )
      }

      "go to UploadSingleFile when waitForFileVerification and file already failed" in {
        given(
          WaitingForFileVerification(
            fileUploadContext,
            "foo-bar-ref-1",
            UploadRequest(
              href = "https://s3.bucket",
              fields = Map(
                "callbackUrl"     -> "https://foo.bar/callback",
                "successRedirect" -> "https://foo.bar/success",
                "errorRedirect"   -> "https://foo.bar/failure"
              )
            ),
            FileUpload.Failed(
              Nonce(1),
              Timestamp.Any,
              "foo-bar-ref-1",
              UpscanNotification.FailureDetails(UpscanNotification.QUARANTINE, "some reason")
            ),
            FileUploads(files =
              Seq(
                FileUpload.Failed(
                  Nonce(1),
                  Timestamp.Any,
                  "foo-bar-ref-1",
                  UpscanNotification.FailureDetails(UpscanNotification.QUARANTINE, "some reason")
                )
              )
            )
          )
        ) when waitForFileVerification should thenGo(
          UploadSingleFile(
            fileUploadContext,
            "foo-bar-ref-1",
            UploadRequest(
              href = "https://s3.bucket",
              fields = Map(
                "callbackUrl"     -> "https://foo.bar/callback",
                "successRedirect" -> "https://foo.bar/success",
                "errorRedirect"   -> "https://foo.bar/failure"
              )
            ),
            FileUploads(files =
              Seq(
                FileUpload.Failed(
                  Nonce(1),
                  Timestamp.Any,
                  "foo-bar-ref-1",
                  UpscanNotification.FailureDetails(UpscanNotification.QUARANTINE, "some reason")
                )
              )
            ),
            Some(
              FileVerificationFailed(UpscanNotification.FailureDetails(UpscanNotification.QUARANTINE, "some reason"))
            )
          )
        )
      }

      "go to Summary when upscanCallbackArrived and accepted, and reference matches" in {
        val mockPushFileUploadResult: FileUploadResultPushApi =
          _ => Future.successful(FileUploadResultPushConnector.SuccessResponse)
        given(
          WaitingForFileVerification(
            fileUploadContext,
            "foo-bar-ref-1",
            UploadRequest(
              href = "https://s3.bucket",
              fields = Map(
                "callbackUrl"     -> "https://foo.bar/callback",
                "successRedirect" -> "https://foo.bar/success",
                "errorRedirect"   -> "https://foo.bar/failure"
              )
            ),
            FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
            FileUploads(files = Seq(FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1")))
          )
        ) when upscanCallbackArrived(mockPushFileUploadResult)(Nonce(1))(
          UpscanFileReady(
            reference = "foo-bar-ref-1",
            downloadUrl = "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
            uploadDetails = UpscanNotification.UploadDetails(
              uploadTimestamp = ZonedDateTime.parse("2018-04-24T09:30:00Z"),
              checksum = "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
              fileName = "test.pdf",
              fileMimeType = "application/pdf",
              size = 4567890
            )
          )
        ) should thenGo(
          Summary(
            fileUploadContext,
            FileUploads(files = Seq(acceptedFileUpload))
          )
        )
      }

      "go to UploadSingleFile when upscanCallbackArrived and failed, and reference matches" in {
        val mockPushFileUploadResult: FileUploadResultPushApi =
          _ => Future.successful(FileUploadResultPushConnector.SuccessResponse)
        given(
          WaitingForFileVerification(
            fileUploadContext,
            "foo-bar-ref-1",
            UploadRequest(
              href = "https://s3.bucket",
              fields = Map(
                "callbackUrl"     -> "https://foo.bar/callback",
                "successRedirect" -> "https://foo.bar/success",
                "errorRedirect"   -> "https://foo.bar/failure"
              )
            ),
            FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
            FileUploads(files = Seq(FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1")))
          )
        ) when upscanCallbackArrived(mockPushFileUploadResult)(Nonce(1))(
          UpscanFileFailed(
            reference = "foo-bar-ref-1",
            failureDetails = UpscanNotification.FailureDetails(
              failureReason = UpscanNotification.QUARANTINE,
              message = "e.g. This file has a virus"
            )
          )
        ) should thenGo(
          UploadSingleFile(
            fileUploadContext,
            "foo-bar-ref-1",
            UploadRequest(
              href = "https://s3.bucket",
              fields = Map(
                "callbackUrl"     -> "https://foo.bar/callback",
                "successRedirect" -> "https://foo.bar/success",
                "errorRedirect"   -> "https://foo.bar/failure"
              )
            ),
            FileUploads(files =
              Seq(
                FileUpload.Failed(
                  Nonce(1),
                  Timestamp.Any,
                  "foo-bar-ref-1",
                  UpscanNotification.FailureDetails(UpscanNotification.QUARANTINE, "e.g. This file has a virus")
                )
              )
            ),
            Some(
              FileVerificationFailed(
                UpscanNotification.FailureDetails(UpscanNotification.QUARANTINE, "e.g. This file has a virus")
              )
            )
          )
        )
      }

      "stay at WaitingForFileVerification when upscanCallbackArrived and reference doesn't match" in {
        val mockPushFileUploadResult: FileUploadResultPushApi =
          _ => Future.successful(FileUploadResultPushConnector.SuccessResponse)
        given(
          WaitingForFileVerification(
            fileUploadContext,
            "foo-bar-ref-1",
            UploadRequest(
              href = "https://s3.bucket",
              fields = Map(
                "callbackUrl"     -> "https://foo.bar/callback",
                "successRedirect" -> "https://foo.bar/success",
                "errorRedirect"   -> "https://foo.bar/failure"
              )
            ),
            FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Posted(Nonce(2), Timestamp.Any, "foo-bar-ref-2")
              )
            )
          )
        ) when upscanCallbackArrived(mockPushFileUploadResult)(Nonce(2))(
          UpscanFileFailed(
            reference = "foo-bar-ref-2",
            failureDetails = UpscanNotification.FailureDetails(
              failureReason = UpscanNotification.QUARANTINE,
              message = "e.g. This file has a virus"
            )
          )
        ) should thenGo(
          WaitingForFileVerification(
            fileUploadContext,
            "foo-bar-ref-1",
            UploadRequest(
              href = "https://s3.bucket",
              fields = Map(
                "callbackUrl"     -> "https://foo.bar/callback",
                "successRedirect" -> "https://foo.bar/success",
                "errorRedirect"   -> "https://foo.bar/failure"
              )
            ),
            FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Failed(
                  Nonce(2),
                  Timestamp.Any,
                  "foo-bar-ref-2",
                  UpscanNotification.FailureDetails(UpscanNotification.QUARANTINE, "e.g. This file has a virus")
                )
              )
            )
          )
        )
      }

      "retreat to Summary when some files has been uploaded already" in {
        given(
          WaitingForFileVerification(
            fileUploadContext,
            "foo-bar-ref-1",
            UploadRequest(
              href = "https://s3.bucket",
              fields = Map(
                "callbackUrl"     -> "https://foo.bar/callback",
                "successRedirect" -> "https://foo.bar/success",
                "errorRedirect"   -> "https://foo.bar/failure"
              )
            ),
            FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Accepted(
                  Nonce(2),
                  Timestamp.Any,
                  "foo-bar-ref-2",
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
        ) when backToSummary should thenGo(
          Summary(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Accepted(
                  Nonce(2),
                  Timestamp.Any,
                  "foo-bar-ref-2",
                  "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                  ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                  "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                  "test.pdf",
                  "application/pdf",
                  4567890
                )
              )
            ),
            true
          )
        )
      }

      "retreat to ContinueToHost when none file has been uploaded and none accepted yet" in {
        given(
          WaitingForFileVerification(
            fileUploadContext,
            "foo-bar-ref-1",
            UploadRequest(
              href = "https://s3.bucket",
              fields = Map(
                "callbackUrl"     -> "https://foo.bar/callback",
                "successRedirect" -> "https://foo.bar/success",
                "errorRedirect"   -> "https://foo.bar/failure"
              )
            ),
            FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Posted(Nonce(2), Timestamp.Any, "foo-bar-ref-2")
              )
            )
          )
        ) when backToSummary should thenGo(
          ContinueToHost(
            fileUploadContext,
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
                FileUpload.Posted(Nonce(2), Timestamp.Any, "foo-bar-ref-2")
              )
            )
          )
        )
      }

      "go to UploadSingleFile when initiateFileUpload" in {
        val hostData = fileUploadContext
        val uploadRequest = UploadRequest(
          href = "https://s3.bucket",
          fields = Map(
            "callbackUrl"     -> "https://foo.bar/callback",
            "successRedirect" -> "https://foo.bar/success",
            "errorRedirect"   -> "https://foo.bar/failure"
          )
        )
        val fileUploads = FileUploads(files =
          Seq(
            FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1")
          )
        )
        val state = WaitingForFileVerification(
          hostData,
          "foo-bar-ref-1",
          uploadRequest,
          FileUpload.Posted(Nonce(1), Timestamp.Any, "foo-bar-ref-1"),
          fileUploads
        )
        given(state)
          .when(initiateFileUpload(testUpscanRequest)(mockUpscanInitiate))
          .thenGoes(UploadSingleFile(hostData, "foo-bar-ref-1", uploadRequest, fileUploads))
      }
    }

    "at state FileUploaded" should {
      "go to acknowledged FileUploaded when waitForFileVerification" in {
        val state = Summary(
          fileUploadContext,
          FileUploads(files =
            Seq(
              FileUpload.Accepted(
                Nonce(1),
                Timestamp.Any,
                "foo-bar-ref-1",
                "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
                ZonedDateTime.parse("2018-04-24T09:30:00Z"),
                "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
                "test.pdf",
                "application/pdf",
                4567890
              )
            )
          ),
          acknowledged = false
        )
        given(state)
          .when(waitForFileVerification)
          .thenGoes(state.copy(acknowledged = true))
      }

      "go to UploadSingleFile when initiateFileUpload and number of uploads below the limit" in {
        val hostData = fileUploadContext
        val fileUploads = FileUploads(files =
          for (i <- 0 until (maxFileUploadsNumber - 1))
            yield FileUpload.Accepted(
              Nonce(i),
              Timestamp.Any,
              s"foo-bar-ref-$i",
              "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
              ZonedDateTime.parse("2018-04-24T09:30:00Z"),
              "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
              "test.pdf",
              "application/pdf",
              4567890
            )
        )
        given(
          Summary(
            hostData,
            fileUploads,
            acknowledged = false
          )
        )
          .when(initiateFileUpload(testUpscanRequest)(mockUpscanInitiate))
          .thenGoes(
            UploadSingleFile(
              hostData,
              "foo-bar-ref",
              someUploadRequest(testUpscanRequest("foo", maximumFileSizeBytes)),
              fileUploads + FileUpload.Initiated(Nonce.Any, Timestamp.Any, "foo-bar-ref")
            )
          )
      }

      "stay when initiateFileUpload and number of uploads above the limit" in {
        val hostData = fileUploadContext
        val fileUploads = FileUploads(files =
          for (i <- 0 until maxFileUploadsNumber)
            yield FileUpload.Accepted(
              Nonce(i),
              Timestamp.Any,
              s"foo-bar-ref-$i",
              "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
              ZonedDateTime.parse("2018-04-24T09:30:00Z"),
              "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
              "test.pdf",
              "application/pdf",
              4567890
            )
        )
        given(
          Summary(
            hostData,
            fileUploads,
            acknowledged = false
          )
        )
          .when(initiateFileUpload(testUpscanRequest)(mockUpscanInitiate))
          .thenNoChange
      }

      "go to UploadSingleFile when submitedUploadAnotherFileChoice with yes and number of uploads below the limit" in {
        val hostData = fileUploadContext
        val fileUploads = FileUploads(files =
          for (i <- 0 until (maxFileUploadsNumber - 1))
            yield fileUploadAccepted.copy(reference = s"file-$i")
        )
        given(
          Summary(hostData, fileUploads)
        )
          .when(submitedUploadAnotherFileChoice(testUpscanRequest)(mockUpscanInitiate)(continueToHost)(true))
          .thenGoes(
            UploadSingleFile(
              hostData,
              "foo-bar-ref",
              someUploadRequest(testUpscanRequest("foo", maximumFileSizeBytes)),
              fileUploads + FileUpload.Initiated(Nonce.Any, Timestamp.Any, "foo-bar-ref")
            )
          )
      }

      "apply follow-up transition when submitedUploadAnotherFileChoice with yes and number of uploads above the limit" in {
        val hostData = fileUploadContext
        val fileUploads = FileUploads(files =
          for (i <- 0 until maxFileUploadsNumber)
            yield fileUploadAccepted.copy(reference = s"file-$i")
        )
        given(
          Summary(hostData, fileUploads)
        )
          .when(submitedUploadAnotherFileChoice(testUpscanRequest)(mockUpscanInitiate)(continueToHost)(true))
          .thenGoes(
            ContinueToHost(fileUploadContext, fileUploads)
          )
      }

      "apply follow-up transition when submitedUploadAnotherFileChoice with no" in {
        val hostData = fileUploadContext
        val fileUploads = FileUploads(files =
          for (i <- 0 until (maxFileUploadsNumber - 1))
            yield fileUploadAccepted.copy(reference = s"file-$i")
        )
        given(
          Summary(hostData, fileUploads)
        )
          .when(submitedUploadAnotherFileChoice(testUpscanRequest)(mockUpscanInitiate)(continueToHost)(false))
          .thenGoes(
            ContinueToHost(fileUploadContext, fileUploads)
          )
      }

      "go to UploadSingleFile when removeFileUploadByReference leaving no files" in {
        val hostData = fileUploadContext
        val fileUploads = FileUploads(Seq(fileUploadAccepted))
        val mockPushFileUploadResult: FileUploadResultPushApi =
          _ => Future.successful(FileUploadResultPushConnector.SuccessResponse)
        given(
          Summary(hostData, fileUploads)
        )
          .when(
            removeFileUploadByReference(fileUploadAccepted.reference)(testUpscanRequest)(mockUpscanInitiate)(
              mockPushFileUploadResult
            )
          )
          .thenGoes(
            UploadSingleFile(
              hostData,
              "foo-bar-ref",
              someUploadRequest(testUpscanRequest("foo", maximumFileSizeBytes)),
              FileUploads(Seq(FileUpload.Initiated(Nonce.Any, Timestamp.Any, "foo-bar-ref")))
            )
          )
      }
    }

    "at state ContinueToHost" should {
      "go to Unitialized when wipeOut transition" in
        given(ContinueToHost(fileUploadContext, nonEmptyFileUploads)).when(wipeOut).thenGoes(Uninitialized)
    }

  }
}
