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
import uk.gov.hmrc.mongo.cache.CacheItem
import uk.gov.hmrc.uploaddocuments.models.{FileUpload, FileUploadContext, FileUploads, Nonce, Timestamp}
import uk.gov.hmrc.uploaddocuments.repository.JourneyCacheRepository
import uk.gov.hmrc.uploaddocuments.repository.JourneyCacheRepository.DataKeys
import uk.gov.hmrc.uploaddocuments.support.TestData._
import uk.gov.hmrc.uploaddocuments.support.{AppISpec, LogCapturing}

import scala.concurrent.Future

class FileUploadServiceISpec extends AppISpec with LogCapturing with BeforeAndAfterEach {

  implicit lazy val ec = scala.concurrent.ExecutionContext.Implicits.global

  lazy val repo = app.injector.instanceOf[JourneyCacheRepository]
  lazy val testFileUploadService = app.injector.instanceOf[FileUploadService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repo.collection.deleteMany(BsonDocument()).toFuture())
    await(repo.collection.countDocuments().toFuture()) shouldBe 0
  }

  "FileUploadService" when {

    "calling .putFiles()" should {

      "insert when no existing files present" in {

        await(repo.collection.countDocuments().toFuture()) shouldBe 0

        await(testFileUploadService.putFiles(nonEmptyFileUploads)(journeyId))

        await(repo.collection.countDocuments().toFuture()) shouldBe 1
        await(repo.get(journeyId)(DataKeys.uploadedFiles)) shouldBe Some(nonEmptyFileUploads)
      }

      "update when existing file present" in {

        await(repo.collection.countDocuments().toFuture()) shouldBe 0

        await(testFileUploadService.putFiles(nonEmptyFileUploads)(journeyId))
        await(repo.collection.countDocuments().toFuture()) shouldBe 1

        val updatedRecord = nonEmptyFileUploads.copy(files = nonEmptyFileUploads.files :+ acceptedFileUpload)

        await(testFileUploadService.putFiles(updatedRecord)(journeyId))

        await(repo.collection.countDocuments().toFuture()) shouldBe 1
        await(repo.get(journeyId)(DataKeys.uploadedFiles)) shouldBe Some(updatedRecord)
      }
    }

    "calling .getFiles()" should {

      "return None when no journey exists" in {

        await(repo.collection.countDocuments().toFuture()) shouldBe 0

        await(testFileUploadService.getFiles()(journeyId)) shouldBe None
      }

      "return an empty FileUploads when no files exist" in {

        await(repo.collection.countDocuments().toFuture()) shouldBe 0

        await(testFileUploadService.putFiles(FileUploads())(journeyId))
        await(repo.collection.countDocuments().toFuture()) shouldBe 1

        await(testFileUploadService.getFiles()(journeyId)) shouldBe Some(FileUploads())
      }

      "return non-empty FileUploads when files exist" in {

        await(repo.collection.countDocuments().toFuture()) shouldBe 0

        val files = FileUploads(Seq(fileUploadInitiated, acceptedFileUpload))

        await(testFileUploadService.putFiles(files)(journeyId))
        await(repo.collection.countDocuments().toFuture()) shouldBe 1

        await(testFileUploadService.getFiles()(journeyId)) shouldBe Some(files)
      }
    }

    "calling .withFiles()" should {

      def testWithFiles(): Option[FileUploads] = {
        await(testFileUploadService.withFiles[Option[FileUploads]](
          journeyNotFoundResult = Future.successful(None)
        )(files => Future.successful(Some(files)))(journeyId))
      }

      "execute the journeyNotFound result when no journey found" in {

        withCaptureOfLoggingFrom(testFileUploadService.logger) { logs =>

          testWithFiles shouldBe None

          logExists("[withFiles] No files exist for the supplied journeyID")(logs)
          logExists(s"[withFiles] journeyId: '$journeyId'")(logs)
        }
      }

      "execute f() when no journey is found" in {

        await(testFileUploadService.putFiles(FileUploads())(journeyId))
        testWithFiles shouldBe Some(FileUploads())
      }
    }

    "calling .markFileAsPosted()" should {

      "when a file exists with the supplied key" must {

        "update the file and mark its state as POSTED" in {

          val files = FileUploads(Seq(acceptedFileUpload, fileUploadInitiated))
          val key = fileUploadInitiated.reference

          await(testFileUploadService.putFiles(files)(journeyId))
          await(repo.collection.countDocuments().toFuture()) shouldBe 1

          await(testFileUploadService.markFileAsPosted(key)(journeyId)) shouldBe a[Some[CacheItem]]
          await(repo.collection.countDocuments().toFuture()) shouldBe 1

          await(testFileUploadService.getFiles()(journeyId)) shouldBe Some(
            FileUploads(Seq(
              acceptedFileUpload,
              FileUpload.Posted(Nonce.Any, Timestamp.Any, key)
            ))
          )
        }
      }

      "when a file DOES NOT exist with the supplied key" must {

        "update nothing and keep the state unchanged (output a warning log)" in {

          val files = FileUploads(Seq(acceptedFileUpload, fileUploadInitiated))

          await(testFileUploadService.putFiles(files)(journeyId))
          await(repo.collection.countDocuments().toFuture()) shouldBe 1


          withCaptureOfLoggingFrom(testFileUploadService.logger) { logs =>

            await(testFileUploadService.markFileAsPosted("invalidKey")(journeyId)) shouldBe None

            logExists("[markFileAsPosted] No file with the supplied journeyID & key was updated and marked as posted")(logs)
            logExists(s"[markFileAsPosted] No file with the supplied journeyID: '$journeyId' & key: 'invalidKey' was updated and marked as posted")(logs)
          }

          await(repo.collection.countDocuments().toFuture()) shouldBe 1

          await(testFileUploadService.getFiles()(journeyId)) shouldBe Some(files)
        }
      }

      "when a journeyID DOES NOT exist with" must {

        "do nothing but log error" in {

          withCaptureOfLoggingFrom(testFileUploadService.logger) { logs =>

            await(testFileUploadService.markFileAsPosted("invalidKey")("invalidJourneyId")) shouldBe None

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
          val key = fileUploadPosted.reference

          await(testFileUploadService.putFiles(files)(journeyId))
          await(repo.collection.countDocuments().toFuture()) shouldBe 1

          await(testFileUploadService.markFileAsRejected(s3Errors(key))(journeyId, FileUploadContext(fileUploadSessionConfig))) shouldBe a[Some[CacheItem]]
          await(repo.collection.countDocuments().toFuture()) shouldBe 1

          await(testFileUploadService.getFiles()(journeyId)) shouldBe Some(
            FileUploads(Seq(
              acceptedFileUpload,
              FileUpload.Rejected(Nonce.Any, Timestamp.Any, key, s3Errors(key))
            ))
          )
        }
      }

      "when a file DOES NOT exist with the supplied key" must {

        "update nothing and keep the state unchanged (output a warning log)" in {

          val files = FileUploads(Seq(acceptedFileUpload, fileUploadPosted))

          await(testFileUploadService.putFiles(files)(journeyId))
          await(repo.collection.countDocuments().toFuture()) shouldBe 1

          withCaptureOfLoggingFrom(testFileUploadService.logger) { logs =>

            await(testFileUploadService.markFileAsRejected(s3Errors("invalidKey"))(journeyId, FileUploadContext(fileUploadSessionConfig))) shouldBe None

            logExists("[markFileAsRejected] No file with the supplied journeyID & key was updated and marked as rejected")(logs)
            logExists(s"[markFileAsRejected] No file with the supplied journeyID: '$journeyId' & key: 'invalidKey' was updated and marked as rejected")(logs)
          }

          await(repo.collection.countDocuments().toFuture()) shouldBe 1

          await(testFileUploadService.getFiles()(journeyId)) shouldBe Some(files)
        }
      }

      "when a journeyID DOES NOT exist with" must {

        "do nothing but log error" in {

          withCaptureOfLoggingFrom(testFileUploadService.logger) { logs =>

            await(testFileUploadService.markFileAsRejected(s3Errors("invalidKey"))("invalidJourneyId", FileUploadContext(fileUploadSessionConfig))) shouldBe None

            logExists("[withFiles] No files exist for the supplied journeyID")(logs)
            logExists("[withFiles] journeyId: 'invalidJourneyId'")(logs)
          }
        }
      }
    }
  }
}
