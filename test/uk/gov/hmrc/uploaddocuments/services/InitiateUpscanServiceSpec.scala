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

package uk.gov.hmrc.uploaddocuments.services

import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.mvc.Cookie
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.CacheItem
import uk.gov.hmrc.uploaddocuments.connectors.UpscanInitiateConnector
import uk.gov.hmrc.uploaddocuments.models.FileUploadSessionConfig.defaultMaximumFileSizeBytes
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.support.JsEnabled.COOKIE_JSENABLED
import uk.gov.hmrc.uploaddocuments.support.UnitSpec
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

import java.time.{Instant, ZonedDateTime}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class InitiateUpscanServiceSpec extends UnitSpec with MockFactory with GuiceOneAppPerSuite with Injecting {
  val mockUpscanInitiateConnector = mock[UpscanInitiateConnector]
  val mockFileUploadService       = mock[FileUploadService]
  val appConfig                   = inject[AppConfig]

  val fakeRequest          = FakeRequest()
  val fakeRequestJsEnabled = FakeRequest().withCookies(Cookie(COOKIE_JSENABLED, "true"))

  val testService = new InitiateUpscanService(mockUpscanInitiateConnector, mockFileUploadService, appConfig) {
    override val randomNonce: Nonce = Nonce(1)
  }
  val nonce: Nonce     = Nonce(1)
  val maximumFileBytes = 2
  val journeyId        = "testJourneyId"
  val uploadId         = "uploadId"
  val cacheItem        = CacheItem("id", Json.obj(), Instant.now, Instant.now)
  val upscanResponse   = UpscanInitiateResponse("reference", UploadRequest("href", Map.empty[String, String]))

  val fileUploadContext =
    FileUploadContext(FileUploadSessionConfig(Nonce(0), "/continue-url", "/backlink-url", "callback-url"))
  val fileUploadRejected =
    FileUpload.Rejected(Nonce(5), Timestamp.Any, "foo-bar-ref-5", S3UploadError("a", "b", "c"))
  val fileUploadPosted = FileUpload.Posted(Nonce(2), Timestamp.Any, "foo-bar-ref-2")
  val acceptedFileUpload =
    FileUpload.Accepted(
      Nonce(1),
      Timestamp.Any,
      "foo-bar-ref-3",
      "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
      ZonedDateTime.parse("2018-04-24T09:30:00Z"),
      "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
      "test.pdf",
      "application/pdf",
      4567890
    )

  "callbackFromUpscan" should {
    "redirect to Callback From Upscan page" in {
      testService.callbackFromUpscan(nonce)(journeyId) shouldBe
        s"http://localhost:10110/internal/callback-from-upscan/journey/$journeyId/$nonce"
    }
  }
  "successRedirect" when {
    "JS is enabled" should {
      "redirect to Async Waiting For File Verification page" in {
        testService.successRedirect()(journeyId, fakeRequestJsEnabled) shouldBe
          s"http://localhost:10110/upload-customs-documents/journey/$journeyId/file-verification"
      }
    }
    "JS is not enabled" should {
      "redirect to Show Waiting For File Verification page" in {
        testService.successRedirect()(journeyId, fakeRequest) shouldBe
          "http://localhost:10110/upload-customs-documents/file-verification"
      }
    }
  }
  "successRedirectWhenUploadingMultipleFiles" should {
    "redirect to Async Mark File Upload As Posted page" in {
      testService.successRedirectWhenUploadingMultipleFiles()(journeyId) shouldBe
        s"http://localhost:10110/upload-customs-documents/journey/$journeyId/file-posted"
    }
  }
  "errorRedirect" when {
    "JS is enabled" should {
      "redirect to Async Mark File Upload As Rejected page" in {
        testService.errorRedirect()(journeyId, fakeRequestJsEnabled) shouldBe
          s"http://localhost:10110/upload-customs-documents/journey/$journeyId/file-rejected"
      }
    }
    "JS is not enabled" should {
      "redirect to Mark File Upload As Rejected page" in {
        testService.errorRedirect()(journeyId, fakeRequest) shouldBe
          "http://localhost:10110/upload-customs-documents/file-rejected"
      }
    }
  }
  "upscanRequestWhenUploadingMultipleFiles" should {
    "return UpscanInitiateRequest" in {
      testService.upscanRequestWhenUploadingMultipleFiles(nonce, maximumFileBytes)(journeyId, fakeRequest) shouldBe
        UpscanInitiateRequest(
          callbackUrl     = testService.callbackFromUpscan(nonce)(journeyId),
          successRedirect = Some(testService.successRedirectWhenUploadingMultipleFiles()(journeyId)),
          errorRedirect   = Some(testService.errorRedirect()(journeyId, fakeRequest)),
          minimumFileSize = Some(1),
          maximumFileSize = Some(maximumFileBytes)
        )
    }
  }
  "upscanRequest" should {
    "return UpscanInitiateRequest" in {
      testService.upscanRequest(nonce, maximumFileBytes)(journeyId, fakeRequest) shouldBe
        UpscanInitiateRequest(
          callbackUrl     = testService.callbackFromUpscan(nonce)(journeyId),
          successRedirect = Some(testService.successRedirect()(journeyId, fakeRequest)),
          errorRedirect   = Some(testService.errorRedirect()(journeyId, fakeRequest)),
          minimumFileSize = Some(1),
          maximumFileSize = Some(maximumFileBytes)
        )
    }
  }

  def mockGetFiles(response: Future[Option[FileUploads]]) =
    (mockFileUploadService.getFiles()(_: String)).expects(journeyId).returning(response)

  def mockInitiate(request: UpscanInitiateRequest, response: Future[UpscanInitiateResponse]) =
    (mockUpscanInitiateConnector
      .initiate(_: String, _: UpscanInitiateRequest)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, request, *, *)
      .returning(response)

  def mockPutFiles(request: FileUploads, response: Future[CacheItem] = Future.successful(cacheItem)) =
    (mockFileUploadService.putFiles(_: FileUploads)(_: String)).expects(request, journeyId).returning(response)

  "initiateNextSingleFileUpload" when {
    "no files present for journeyId in cache" should {
      "log and return None" in {
        mockGetFiles(Future.successful(None))
        val result =
          testService.initiateNextSingleFileUpload(fileUploadContext)(journeyId, fakeRequest, HeaderCarrier())

        await(result) shouldBe None
      }
    }
    val upscanRequest    = testService.upscanRequest(nonce, defaultMaximumFileSizeBytes)(journeyId, fakeRequest)
    val newInitiatedFile = FileUpload(nonce, None)(upscanResponse).copy(timestamp = Timestamp.Any)
    "files present for journeyId in cache" when {
      "upscan initiate succeeds and cache successfully updated" when {
        "empty file uploads" should {
          "add initiated to list" in {
            val files        = FileUploads()
            val updatedFiles = FileUploads(Seq(newInitiatedFile))

            mockGetFiles(Future.successful(Some(files)))
            mockInitiate(upscanRequest, Future.successful(upscanResponse))
            mockPutFiles(updatedFiles)

            val result =
              testService.initiateNextSingleFileUpload(fileUploadContext)(journeyId, fakeRequest, HeaderCarrier())

            await(result) shouldBe Some((upscanResponse, updatedFiles, None))
          }
        }
        "file uploads only contains Accepted File Upload" should {
          "add initiated to list" in {
            val files        = FileUploads(Seq(acceptedFileUpload))
            val updatedFiles = FileUploads(Seq(acceptedFileUpload, newInitiatedFile))

            mockGetFiles(Future.successful(Some(files)))
            mockInitiate(upscanRequest, Future.successful(upscanResponse))
            mockPutFiles(updatedFiles)

            val result =
              testService.initiateNextSingleFileUpload(fileUploadContext)(journeyId, fakeRequest, HeaderCarrier())

            await(result) shouldBe Some((upscanResponse, updatedFiles, None))
          }
        }
        "file uploads contains Accepted File Upload and File Upload Error" should {
          "remove non-accepted before adding initiated to list and return the error" in {
            val files        = FileUploads(Seq(acceptedFileUpload, fileUploadRejected))
            val updatedFiles = FileUploads(Seq(acceptedFileUpload, newInitiatedFile))

            mockGetFiles(Future.successful(Some(files)))
            mockInitiate(upscanRequest, Future.successful(upscanResponse))
            mockPutFiles(updatedFiles)

            val result =
              testService.initiateNextSingleFileUpload(fileUploadContext)(journeyId, fakeRequest, HeaderCarrier())

            await(result) shouldBe Some((upscanResponse, updatedFiles, Some(FileUploadError(fileUploadRejected))))
          }
        }
        "file uploads contains Accepted File Upload and non-error Upload" should {
          "remove non-accepted before adding initiated to list and return the error" in {
            val files        = FileUploads(Seq(acceptedFileUpload, fileUploadPosted))
            val updatedFiles = FileUploads(Seq(acceptedFileUpload, newInitiatedFile))

            mockGetFiles(Future.successful(Some(files)))
            mockInitiate(upscanRequest, Future.successful(upscanResponse))
            mockPutFiles(updatedFiles)

            val result =
              testService.initiateNextSingleFileUpload(fileUploadContext)(journeyId, fakeRequest, HeaderCarrier())

            await(result) shouldBe Some((upscanResponse, updatedFiles, None))
          }
        }
      }
      "upscan initiate fails" should {
        "return the Future.failed" in {
          mockGetFiles(Future.successful(Some(FileUploads())))
          mockInitiate(upscanRequest, Future.failed(new NumberFormatException))
          val result =
            testService.initiateNextSingleFileUpload(fileUploadContext)(journeyId, fakeRequest, HeaderCarrier())

          assertThrows[NumberFormatException](await(result))
        }
      }
      "update files for journeyId in cache fails" should {
        "return the Future.failed" in {
          mockGetFiles(Future.successful(Some(FileUploads())))
          mockInitiate(upscanRequest, Future.successful(upscanResponse))
          mockPutFiles(FileUploads(Seq(newInitiatedFile)), Future.failed(new NumberFormatException))
          val result =
            testService.initiateNextSingleFileUpload(fileUploadContext)(journeyId, fakeRequest, HeaderCarrier())

          assertThrows[NumberFormatException](await(result))
        }
      }
    }
    "get files for journeyId returns Future.failed" should {
      "return it" in {
        mockGetFiles(Future.failed(new NumberFormatException))
        val result =
          testService.initiateNextSingleFileUpload(fileUploadContext)(journeyId, fakeRequest, HeaderCarrier())

        assertThrows[NumberFormatException](await(result))
      }
    }
  }
  "initiateNextMultiFileUpload" when {
    "no files present for journeyId in cache" should {
      "log and return None" in {
        mockGetFiles(Future.successful(None))
        val result =
          testService.initiateNextMultiFileUpload(fileUploadContext, uploadId)(journeyId, fakeRequest, HeaderCarrier())

        await(result) shouldBe None
      }
    }
    val upscanRequest =
      testService.upscanRequestWhenUploadingMultipleFiles(nonce, defaultMaximumFileSizeBytes)(journeyId, fakeRequest)
    val newInitiatedFile = FileUpload(nonce, Some(uploadId))(upscanResponse).copy(timestamp = Timestamp.Any)
    "files present for journeyId in cache" when {
      "upscan initiate succeeds and cache successfully updated" when {
        "empty file uploads" should {
          "add initiated to list" in {
            val files        = FileUploads()
            val updatedFiles = FileUploads(Seq(newInitiatedFile))

            mockGetFiles(Future.successful(Some(files)))
            mockInitiate(upscanRequest, Future.successful(upscanResponse))
            mockPutFiles(updatedFiles)

            val result = testService
              .initiateNextMultiFileUpload(fileUploadContext, uploadId)(journeyId, fakeRequest, HeaderCarrier())

            await(result) shouldBe Some(upscanResponse)
          }
        }
        "file uploads contains any files" should {
          "add initiated to list and return upscan response" in {
            val files        = FileUploads(Seq(acceptedFileUpload, fileUploadRejected, fileUploadPosted))
            val updatedFiles = files + newInitiatedFile

            mockGetFiles(Future.successful(Some(files)))
            mockInitiate(upscanRequest, Future.successful(upscanResponse))
            mockPutFiles(updatedFiles)

            val result = testService
              .initiateNextMultiFileUpload(fileUploadContext, uploadId)(journeyId, fakeRequest, HeaderCarrier())

            await(result) shouldBe Some(upscanResponse)
          }
        }
      }
      "upscan initiate fails" should {
        "return the Future.failed" in {
          mockGetFiles(Future.successful(Some(FileUploads())))
          mockInitiate(upscanRequest, Future.failed(new NumberFormatException))
          val result = testService
            .initiateNextMultiFileUpload(fileUploadContext, uploadId)(journeyId, fakeRequest, HeaderCarrier())

          assertThrows[NumberFormatException](await(result))
        }
      }
      "update files for journeyId in cache fails" should {
        "return the Future.failed" in {
          mockGetFiles(Future.successful(Some(FileUploads())))
          mockInitiate(upscanRequest, Future.successful(upscanResponse))
          mockPutFiles(FileUploads(Seq(newInitiatedFile)), Future.failed(new NumberFormatException))
          val result = testService
            .initiateNextMultiFileUpload(fileUploadContext, uploadId)(journeyId, fakeRequest, HeaderCarrier())

          assertThrows[NumberFormatException](await(result))
        }
      }
    }
    "get files for journeyId returns Future.failed" should {
      "return it" in {
        mockGetFiles(Future.failed(new NumberFormatException))
        val result =
          testService.initiateNextMultiFileUpload(fileUploadContext, uploadId)(journeyId, fakeRequest, HeaderCarrier())

        assertThrows[NumberFormatException](await(result))
      }
    }
  }
}
