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

package uk.gov.hmrc.uploaddocuments.services

import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.uploaddocuments.connectors.UpscanInitiateConnector
import uk.gov.hmrc.uploaddocuments.models.FileUploadSessionConfig.defaultMaximumFileSizeBytes
import uk.gov.hmrc.uploaddocuments.models.*
import uk.gov.hmrc.uploaddocuments.services.mocks.MockFileUploadService
import uk.gov.hmrc.uploaddocuments.support.UnitSpec
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

import java.time.ZonedDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class InitiateUpscanServiceSpec
    extends UnitSpec with MockFactory with GuiceOneAppPerSuite with Injecting with MockFileUploadService {

  val mockUpscanInitiateConnector = mock[UpscanInitiateConnector]
  val appConfig                   = inject[AppConfig]
  val fakeRequest                 = FakeRequest()

  val testService = new InitiateUpscanService(mockUpscanInitiateConnector, mockFileUploadService, appConfig) {
    override val randomNonce: Nonce = Nonce(1)
  }

  val nonce: Nonce   = Nonce(1)
  val journeyId      = JourneyId("testJourneyId")
  val upscanResponse = UpscanInitiateResponse("reference", UploadRequest("href", Map.empty[String, String]))

  val fileUploadContext =
    FileUploadContext(FileUploadSessionConfig(Nonce(0), "/continue-url", None, "callback-url"))

  val fileUploadPosted = FileUpload.Posted(Nonce(2), Timestamp.Any, "foo-bar-ref-2")
  val acceptedFileUpload = FileUpload.Accepted(
    Nonce(3),
    Timestamp.Any,
    "foo-bar-ref-3",
    "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
    ZonedDateTime.parse("2018-04-24T09:30:00Z"),
    "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
    "test.pdf",
    "application/pdf",
    4567890
  )

  def mockInitiate(request: UpscanInitiateRequest, response: Future[UpscanInitiateResponse]) =
    (mockUpscanInitiateConnector
      .initiate(_: String, _: UpscanInitiateRequest)(using _: HeaderCarrier, _: ExecutionContext))
      .expects(*, request, *, *)
      .returning(response)

  "initiateNextFileUpload" when {
    "the upscan call succeeds" should {
      "initiate upscan first, then atomically create the new Initiated file via putInitiatedFile" in {
        val upscanRequest    = testService.upscanRequest(nonce, defaultMaximumFileSizeBytes)(using journeyId)
        val newInitiatedFile = FileUpload(nonce, None)(upscanResponse).copy(timestamp = Timestamp.Any)
        val updatedFiles     = FileUploads(Seq(fileUploadPosted, acceptedFileUpload, newInitiatedFile))

        mockInitiate(upscanRequest, Future.successful(upscanResponse))
        mockPutInitiatedFile(nonce, upscanResponse, journeyId)(Future.successful(Some(updatedFiles)))

        val result = testService.initiateNextFileUpload()(using fileUploadContext, journeyId, HeaderCarrier())

        await(result) shouldBe ((upscanResponse, updatedFiles))
      }
    }

    "the upscan call fails" should {
      "propagate the failure as a failed Future" in {
        val upscanRequest = testService.upscanRequest(nonce, defaultMaximumFileSizeBytes)(using journeyId)
        val exception     = new RuntimeException("upscan-initiate is down")

        mockInitiate(upscanRequest, Future.failed(exception))

        val result = testService.initiateNextFileUpload()(using fileUploadContext, journeyId, HeaderCarrier())

        an[RuntimeException] should be thrownBy await(result)
      }
    }

    "the journey has no files" should {
      "fail with an IllegalStateException when putInitiatedFile finds nothing to update" in {
        val upscanRequest = testService.upscanRequest(nonce, defaultMaximumFileSizeBytes)(using journeyId)

        mockInitiate(upscanRequest, Future.successful(upscanResponse))
        mockPutInitiatedFile(nonce, upscanResponse, journeyId)(Future.successful(None))

        val result = testService.initiateNextFileUpload()(using fileUploadContext, journeyId, HeaderCarrier())

        an[IllegalStateException] should be thrownBy await(result)
      }
    }
  }
}
