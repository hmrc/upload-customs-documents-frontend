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

import akka.actor.{ActorSystem, Scheduler}
import play.api.i18n.Messages
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.uploaddocuments.controllers.routes
import uk.gov.hmrc.uploaddocuments.journeys.TestData
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.services.mocks.MockFileUploadService
import uk.gov.hmrc.uploaddocuments.support.UnitSpec

import java.util.concurrent.TimeUnit
import scala.concurrent.Future
import scala.concurrent.duration._

class FileVerificationServiceSpec extends UnitSpec with MockFileUploadService with TestData {

  override implicit val defaultTimeout: FiniteDuration = 20.seconds

  implicit val ec       = scala.concurrent.ExecutionContext.Implicits.global
  implicit val hc       = HeaderCarrier()
  implicit val jid      = journeyId
  implicit val messages = mock[Messages]

  implicit val scheduler: Scheduler = ActorSystem("FileVerificationTestsActor").scheduler

  object TestFileVerificationService extends FileVerificationService(mockFileUploadService)

  class TestFixture() {

    val timeoutTime = System.nanoTime() + Duration.apply(10, TimeUnit.SECONDS).toNanos

    def actual: Future[String] =
      TestFileVerificationService.waitForUpscanResponse(acceptedFileUpload.reference, 500, timeoutTime)(
        readyResult = file => s"ResponseReceived for ${file.reference}",
        ifTimeout = "TimedOut"
      )
  }

  "FileVerificationService" when {

    "calling .waitForUpscanResponse()" when {

      "the file does NOT exist" must {

        "throw Match Error" in new TestFixture {

          mockWithFiles(journeyId)(Future.successful(Some(FileUploads())))

          intercept[MatchError](await(actual))
        }
      }

      "the file exists" when {

        "the wait for a response dos not time out" when {

          "the file is marked as ready immediately" must {

            "execute the whenReady result" in new TestFixture {

              mockWithFiles(journeyId)(Future.successful(Some(FileUploads(Seq(acceptedFileUpload)))))

              await(actual) shouldBe s"ResponseReceived for ${acceptedFileUpload.reference}"
            }
          }

          "the file is marked not ready 3 times, then becomes ready the 4th time" must {

            "eventually, execute the whenReady result" in new TestFixture {

              val postedFileSameReference = fileUploadPosted.copy(reference = acceptedFileUpload.reference)

              mockWithFiles(journeyId)(Future.successful(Some(FileUploads(Seq(postedFileSameReference))))).repeat(3)
              mockWithFiles(journeyId)(Future.successful(Some(FileUploads(Seq(acceptedFileUpload)))))

              await(actual) shouldBe s"ResponseReceived for ${acceptedFileUpload.reference}"
            }
          }
        }

        "the wait for a response times out" when {

          "the file is continually marked as not ready" must {

            "eventually, execute the timeout result" in new TestFixture {

              val postedFileSameReference = fileUploadPosted.copy(reference = acceptedFileUpload.reference)

              mockWithFiles(journeyId)(Future.successful(Some(FileUploads(Seq(postedFileSameReference))))).repeat(6)

              await(actual) shouldBe "TimedOut"
            }
          }
        }
      }
    }

    "calling .getFileVerificationStatus()" when {

      "Journey is not found" must {

        "return None" in {

          mockWithFiles(journeyId)(Future.successful(None))
          await(TestFileVerificationService.getFileVerificationStatus("foo")) shouldBe None
        }
      }

      "Journey is found" when {

        "file is not found" must {

          "return None" in {

            mockWithFiles(journeyId)(Future.successful(Some(FileUploads())))
            await(TestFileVerificationService.getFileVerificationStatus("foo")) shouldBe None
          }
        }

        "file is found" when {

          "content.allowedFilesTypesHint has been set in the context" must {

            "return FileVerificationStatus with the expected hint as the allowedFileTypesHint" in {

              val hint = "Hint"
              val context = journeyContext.copy(config =
                fileUploadSessionConfig.copy(content =
                  CustomizedServiceContent(
                    allowedFilesTypesHint = Some(hint)
                  )
                )
              )

              mockWithFiles(journeyId)(Future.successful(Some(FileUploads(Seq(acceptedFileUpload)))))

              await(
                TestFileVerificationService
                  .getFileVerificationStatus(acceptedFileUpload.reference)(context, messages, journeyId)
              ) shouldBe Some(
                FileVerificationStatus(
                  fileUpload = acceptedFileUpload,
                  filePreviewUrl = routes.PreviewController.previewFileUploadByReference,
                  maximumFileSizeBytes = journeyContext.config.maximumFileSizeBytes.toInt,
                  allowedFileTypesHint = hint
                )
              )
            }
          }

          "content.allowedFilesTypesHint has NOT been set in the context" when {

            "allowedFileExtensions has been set in the context" must {

              "return FileVerificationStatus with the expected file extensions as the allowedFileTypesHint" in {

                val extensions = "Extensions"
                val context =
                  journeyContext.copy(config = fileUploadSessionConfig.copy(allowedFileExtensions = Some(extensions)))

                mockWithFiles(journeyId)(Future.successful(Some(FileUploads(Seq(acceptedFileUpload)))))

                await(
                  TestFileVerificationService
                    .getFileVerificationStatus(acceptedFileUpload.reference)(context, messages, journeyId)
                ) shouldBe
                  Some(
                    FileVerificationStatus(
                      fileUpload = acceptedFileUpload,
                      filePreviewUrl = routes.PreviewController.previewFileUploadByReference,
                      maximumFileSizeBytes = journeyContext.config.maximumFileSizeBytes.toInt,
                      allowedFileTypesHint = extensions
                    )
                  )
              }
            }

            "allowedFileExtensions not been set in the context" must {

              "return FileVerificationStatus with the allowedContentTypes as the allowedFileTypesHint" in {

                mockWithFiles(journeyId)(Future.successful(Some(FileUploads(Seq(acceptedFileUpload)))))

                await(
                  TestFileVerificationService.getFileVerificationStatus(acceptedFileUpload.reference)
                ) shouldBe Some(
                  FileVerificationStatus(
                    fileUpload = acceptedFileUpload,
                    filePreviewUrl = routes.PreviewController.previewFileUploadByReference,
                    maximumFileSizeBytes = journeyContext.config.maximumFileSizeBytes.toInt,
                    allowedFileTypesHint = journeyContext.config.allowedContentTypes
                  )
                )
              }
            }
          }
        }
      }
    }
  }
}
