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

import org.mongodb.scala.bson.BsonDocument
import org.scalatest.BeforeAndAfterEach
import play.api.test.FakeRequest
import uk.gov.hmrc.uploaddocuments.models.fileUploadResultPush._
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.repository.JourneyCacheRepository
import uk.gov.hmrc.uploaddocuments.repository.JourneyCacheRepository.DataKeys
import uk.gov.hmrc.uploaddocuments.stubs.ExternalApiStubs
import uk.gov.hmrc.uploaddocuments.support.TestData._
import uk.gov.hmrc.uploaddocuments.support.{AppISpec, LogCapturing}
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import org.mongodb.scala.SingleObservableFuture

class FileUploadServiceISpec extends AppISpec with ExternalApiStubs with LogCapturing with BeforeAndAfterEach {

  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  lazy val repo                  = app.injector.instanceOf[JourneyCacheRepository]
  lazy val testFileUploadService = app.injector.instanceOf[FileUploadService]
  lazy val appConfig             = app.injector.instanceOf[AppConfig]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repo.collection.deleteMany(BsonDocument()).toFuture())
    await(repo.collection.countDocuments().toFuture()) shouldBe 0
  }

  "FileUploadService" when {

    "calling .putFiles()" should {

      "insert when no existing files present" in {

        await(repo.collection.countDocuments().toFuture()) shouldBe 0

        await(testFileUploadService.putFiles(nonEmptyFileUploads)(journeyId)) shouldBe nonEmptyFileUploads

        await(repo.collection.countDocuments().toFuture()) shouldBe 1
        await(repo.get(journeyId.value)(DataKeys.uploadedFiles)) shouldBe Some(nonEmptyFileUploads)
      }

      "update when existing file present" in {

        await(repo.collection.countDocuments().toFuture()) shouldBe 0

        await(testFileUploadService.putFiles(nonEmptyFileUploads)(journeyId))
        await(repo.collection.countDocuments().toFuture()) shouldBe 1

        val updatedRecord = nonEmptyFileUploads.copy(files = nonEmptyFileUploads.files :+ acceptedFileUpload)

        await(testFileUploadService.putFiles(updatedRecord)(journeyId)) shouldBe updatedRecord

        await(repo.collection.countDocuments().toFuture()) shouldBe 1
        await(repo.get(journeyId.value)(DataKeys.uploadedFiles)) shouldBe Some(updatedRecord)
      }
    }

    "calling .getFiles()" should {

      "return None when no journey exists" in {

        await(repo.collection.countDocuments().toFuture()) shouldBe 0

        await(testFileUploadService.getFiles(journeyId)) shouldBe None
      }

      "return an empty FileUploads when no files exist" in {

        await(repo.collection.countDocuments().toFuture()) shouldBe 0

        await(testFileUploadService.putFiles(FileUploads())(journeyId))
        await(repo.collection.countDocuments().toFuture()) shouldBe 1

        await(testFileUploadService.getFiles(journeyId)) shouldBe Some(FileUploads())
      }

      "return non-empty FileUploads when files exist" in {

        await(repo.collection.countDocuments().toFuture()) shouldBe 0

        val files = FileUploads(Seq(fileUploadInitiated, acceptedFileUpload))

        await(testFileUploadService.putFiles(files)(journeyId))
        await(repo.collection.countDocuments().toFuture()) shouldBe 1

        await(testFileUploadService.getFiles(journeyId)) shouldBe Some(files)
      }
    }

    "calling .withFiles()" should {

      def testWithFiles(): Option[FileUploads] =
        await(
          testFileUploadService.withFiles[Option[FileUploads]](
            journeyNotFoundResult = Future.successful(None)
          )(files => Future.successful(Some(files)))(journeyId)
        )

      "execute the journeyNotFound result when no journey found" in {

        withCaptureOfLoggingFrom(testFileUploadService.logger) { logs =>
          testWithFiles() shouldBe None

          logExists("[withFiles] No files exist for the supplied journeyID")(logs)
          logExists(s"[withFiles] journeyId: '$journeyId'")(logs)
        }
      }

      "execute f() when no journey is found" in {

        await(testFileUploadService.putFiles(FileUploads())(journeyId))
        testWithFiles() shouldBe Some(FileUploads())
      }
    }

    "calling .markFileAsPosted()" should {

      "when a file exists with the supplied key" must {

        "update the file and mark its state as POSTED" in {

          val files = FileUploads(Seq(acceptedFileUpload, fileUploadInitiated))
          val key   = fileUploadInitiated.reference

          await(testFileUploadService.putFiles(files)(journeyId))
          await(repo.collection.countDocuments().toFuture()) shouldBe 1

          val updatedFiles = FileUploads(
            Seq(
              acceptedFileUpload,
              FileUpload.Posted(Nonce.Any, Timestamp.Any, key)
            )
          )

          await(testFileUploadService.markFileAsPosted(key)(journeyId)) shouldBe Some(updatedFiles)
          await(repo.collection.countDocuments().toFuture()) shouldBe 1

          await(testFileUploadService.getFiles(journeyId)) shouldBe Some(updatedFiles)
        }
      }

      "when a file DOES NOT exist with the supplied key" must {

        "update nothing and keep the state unchanged (output a warning log)" in {

          val files = FileUploads(Seq(acceptedFileUpload, fileUploadInitiated))

          await(testFileUploadService.putFiles(files)(journeyId))
          await(repo.collection.countDocuments().toFuture()) shouldBe 1

          withCaptureOfLoggingFrom(testFileUploadService.logger) { logs =>
            await(testFileUploadService.markFileAsPosted("invalidKey")(journeyId)) shouldBe None

            logExists("[markFileAsPosted] No file with the supplied journeyID & key was updated and marked as posted")(
              logs
            )
            logExists(
              s"[markFileAsPosted] No file with the supplied journeyID: '$journeyId' & key: 'invalidKey' was updated and marked as posted"
            )(logs)
          }

          await(repo.collection.countDocuments().toFuture()) shouldBe 1

          await(testFileUploadService.getFiles(journeyId)) shouldBe Some(files)
        }
      }

      "when a journeyID DOES NOT exist with" must {

        "do nothing but log error" in {

          withCaptureOfLoggingFrom(testFileUploadService.logger) { logs =>
            await(testFileUploadService.markFileAsPosted("invalidKey")(JourneyId("invalidJourneyId"))) shouldBe None

            logExists("[withFiles] No files exist for the supplied journeyID")(logs)
            logExists("[withFiles] journeyId: 'invalidJourneyId'")(logs)
          }
        }
      }
    }

    "calling .markFileAsRejected()" should {

      "when a file exists with the supplied key" must {

        "update the file and mark its state as REJECTED" in {

          val files = FileUploads(Seq(acceptedFileUpload, fileUploadPosted))
          val key   = fileUploadPosted.reference

          await(testFileUploadService.putFiles(files)(journeyId))
          await(repo.collection.countDocuments().toFuture()) shouldBe 1

          val updatedFiles = FileUploads(
            Seq(
              acceptedFileUpload,
              FileUpload.Rejected(Nonce.Any, Timestamp.Any, key, s3Errors(key))
            )
          )

          await(
            testFileUploadService
              .markFileAsRejected(s3Errors(key))(journeyId, FileUploadContext(fileUploadSessionConfig))
          ) shouldBe Some(updatedFiles)
          await(repo.collection.countDocuments().toFuture()) shouldBe 1

          await(testFileUploadService.getFiles(journeyId)) shouldBe Some(updatedFiles)
        }
      }

      "when a file DOES NOT exist with the supplied key" must {

        "update nothing and keep the state unchanged (output a warning log)" in {

          val files = FileUploads(Seq(acceptedFileUpload, fileUploadPosted))

          await(testFileUploadService.putFiles(files)(journeyId))
          await(repo.collection.countDocuments().toFuture()) shouldBe 1

          withCaptureOfLoggingFrom(testFileUploadService.logger) { logs =>
            await(
              testFileUploadService
                .markFileAsRejected(s3Errors("invalidKey"))(journeyId, FileUploadContext(fileUploadSessionConfig))
            ) shouldBe None

            logExists(
              "[markFileAsRejected] No file with the supplied journeyID & key was updated and marked as rejected"
            )(logs)
            logExists(
              s"[markFileAsRejected] No file with the supplied journeyID: '$journeyId' & key: 'invalidKey' was updated and marked as rejected"
            )(logs)
          }

          await(repo.collection.countDocuments().toFuture()) shouldBe 1

          await(testFileUploadService.getFiles(journeyId)) shouldBe Some(files)
        }
      }

      "when a journeyID DOES NOT exist with" must {

        "do nothing but log error" in {

          withCaptureOfLoggingFrom(testFileUploadService.logger) { logs =>
            await(
              testFileUploadService.markFileAsRejected(s3Errors("invalidKey"))(
                JourneyId("invalidJourneyId"),
                FileUploadContext(fileUploadSessionConfig)
              )
            ) shouldBe None

            logExists("[withFiles] No files exist for the supplied journeyID")(logs)
            logExists("[withFiles] journeyId: 'invalidJourneyId'")(logs)
          }
        }
      }
    }

    "calling .markFileWithUpscanResponseAndNotifyHost()" when {

      "a file exists with the supplied Nonce" when {

        "the response from Upscan is Ready" when {

          "the response is ACCEPTED" when {

            "the file uploaded was a DUPLICATE (same checksum)" must {

              "update the files to mark it as a Duplicate and NOT push a notification to the host service" in {

                val files = FileUploads(Seq(acceptedFileUpload, fileUploadPosted))

                await(testFileUploadService.putFiles(files)(journeyId))
                await(repo.collection.countDocuments().toFuture()) shouldBe 1

                val updatedFiles = FileUploads(
                  Seq(
                    acceptedFileUpload,
                    FileUpload.Duplicate(
                      nonce = fileUploadPosted.nonce,
                      timestamp = Timestamp.Any,
                      reference = fileUploadPosted.reference,
                      checksum = acceptedFileUpload.checksum,
                      existingFileName = acceptedFileUpload.fileName,
                      duplicateFileName = "file.png"
                    )
                  )
                )

                await(
                  testFileUploadService.markFileWithUpscanResponseAndNotifyHost(
                    notification = upscanFileReady(fileUploadPosted.reference, acceptedFileUpload.checksum),
                    requestNonce = fileUploadPosted.nonce
                  )(FileUploadContext(fileUploadSessionConfig), journeyId, hc(FakeRequest()))
                ) shouldBe Some(updatedFiles)

                await(repo.collection.countDocuments().toFuture()) shouldBe 1

                await(testFileUploadService.getFiles(journeyId)) shouldBe Some(updatedFiles)
              }
            }

            "the file uploaded is NOT a DUPLICATE (different checksum)" must {

              "update the files to mark it as ACCEPTED and push a notification to inform the host service" in {

                val files             = FileUploads(Seq(acceptedFileUpload, fileUploadPosted))
                val fileUploadContext = FileUploadContext(fileUploadSessionConfig)

                await(testFileUploadService.putFiles(files)(journeyId))
                await(repo.collection.countDocuments().toFuture()) shouldBe 1

                val upscanNotification = upscanFileReady(fileUploadPosted.reference)
                val updatedFiles = FileUploads(
                  Seq(
                    acceptedFileUpload,
                    FileUpload.Accepted(
                      nonce = fileUploadPosted.nonce,
                      timestamp = Timestamp.Any,
                      reference = fileUploadPosted.reference,
                      checksum = upscanNotification.uploadDetails.checksum,
                      url = upscanNotification.downloadUrl,
                      uploadTimestamp = upscanNotification.uploadDetails.uploadTimestamp,
                      fileName = upscanNotification.uploadDetails.fileName,
                      fileMimeType = upscanNotification.uploadDetails.fileMimeType,
                      fileSize = upscanNotification.uploadDetails.size
                    )
                  )
                )

                givenResultPushEndpoint(
                  path = "/result-post-url",
                  payload = Payload(Request(fileUploadContext, updatedFiles), appConfig.baseExternalCallbackUrl),
                  status = 204
                )

                await(
                  testFileUploadService.markFileWithUpscanResponseAndNotifyHost(
                    notification = upscanNotification,
                    requestNonce = fileUploadPosted.nonce
                  )(fileUploadContext, journeyId, hc(FakeRequest()))
                ) shouldBe Some(updatedFiles)

                await(repo.collection.countDocuments().toFuture()) shouldBe 1

                await(testFileUploadService.getFiles(journeyId)) shouldBe Some(updatedFiles)
              }
            }
          }

          "the response is FAILED (QUARANTINED)" must {

            "update the files to mark it as FAILED and do NOT push a notification to inform the host service" in {

              val files             = FileUploads(Seq(acceptedFileUpload, fileUploadPosted))
              val fileUploadContext = FileUploadContext(fileUploadSessionConfig)

              await(testFileUploadService.putFiles(files)(journeyId))
              await(repo.collection.countDocuments().toFuture()) shouldBe 1

              val upscanNotification = upscanFailed(fileUploadPosted.reference)
              val updatedFiles = FileUploads(
                Seq(
                  acceptedFileUpload,
                  FileUpload.Failed(
                    fileUploadPosted.nonce,
                    Timestamp.Any,
                    fileUploadPosted.reference,
                    upscanNotification.failureDetails
                  )
                )
              )

              await(
                testFileUploadService.markFileWithUpscanResponseAndNotifyHost(
                  notification = upscanNotification,
                  requestNonce = fileUploadPosted.nonce
                )(fileUploadContext, journeyId, hc(FakeRequest()))
              ) shouldBe Some(updatedFiles)

              await(repo.collection.countDocuments().toFuture()) shouldBe 1

              await(testFileUploadService.getFiles(journeyId)) shouldBe Some(updatedFiles)
            }
          }
        }
      }

      "when a file DOES NOT exist with the supplied Nonce" must {

        "update nothing and keep the FileUploads unchanged (output a warning log)" in {

          val files = FileUploads(Seq(acceptedFileUpload, fileUploadPosted))

          await(testFileUploadService.putFiles(files)(journeyId))
          await(repo.collection.countDocuments().toFuture()) shouldBe 1

          withCaptureOfLoggingFrom(testFileUploadService.logger) { logs =>
            await(
              testFileUploadService.markFileWithUpscanResponseAndNotifyHost(
                notification = upscanFileReady("invalidKey"),
                requestNonce = Nonce("notExist")
              )(FileUploadContext(fileUploadSessionConfig), journeyId, hc(FakeRequest()))
            ) shouldBe Some(files)

            logExists(
              "[markFileWithUpscanResponseAndNotifyHost] No files were updated following the callback from Upscan"
            )(logs)
            logExists(
              s"[markFileWithUpscanResponseAndNotifyHost] No files were updated following the callback from Upscan. journeyId: '$journeyId', upscanRef: 'invalidKey'"
            )(logs)
          }

          await(repo.collection.countDocuments().toFuture()) shouldBe 1

          await(testFileUploadService.getFiles(journeyId)) shouldBe Some(files)
        }
      }

      "when a journeyID DOES NOT exist" must {

        "do nothing but log error" in {

          withCaptureOfLoggingFrom(testFileUploadService.logger) { logs =>
            await(
              testFileUploadService.markFileWithUpscanResponseAndNotifyHost(
                notification = upscanFileReady("foo"),
                requestNonce = Nonce.Any
              )(FileUploadContext(fileUploadSessionConfig), JourneyId("invalidJourneyId"), hc(FakeRequest()))
            ) shouldBe None

            logExists("[withFiles] No files exist for the supplied journeyID")(logs)
            logExists("[withFiles] journeyId: 'invalidJourneyId'")(logs)
          }
        }
      }
    }
  }
}
