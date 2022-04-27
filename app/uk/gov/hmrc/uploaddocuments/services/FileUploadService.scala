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

import uk.gov.hmrc.mongo.cache.CacheItem
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.repository.JourneyCacheRepository
import uk.gov.hmrc.uploaddocuments.repository.JourneyCacheRepository.DataKeys
import uk.gov.hmrc.uploaddocuments.support.UploadLog
import uk.gov.hmrc.uploaddocuments.utils.LoggerUtil

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileUploadService @Inject()(repo: JourneyCacheRepository)
                                 (implicit ec: ExecutionContext) extends LoggerUtil with UploadLog {

  def getFiles()(implicit journeyId: String): Future[Option[FileUploads]] =
    repo.get(journeyId)(DataKeys.uploadedFiles)

  def putFiles(files: FileUploads)(implicit journeyId: String): Future[CacheItem] =
    repo.put(journeyId)(DataKeys.uploadedFiles, files)

  def withFiles[T](journeyNotFoundResult: => Future[T])(f: FileUploads => Future[T])
                  (implicit journeyId: String): Future[T] =
    getFiles flatMap {
      case None =>
        error("[withFiles] No files exist for the supplied journeyID")
        debug(s"[withFiles] journeyId: '$journeyId'")
        journeyNotFoundResult
      case Some(files) => f(files)
    }

  def markFileAsPosted(upscanKey: String)
                      (implicit journeyId: String): Future[Option[CacheItem]] =

    withFiles[Option[CacheItem]](Future.successful(None)) { files =>

      val updatedFileUploads =
        FileUploads(files.files.map {
          case file@FileUpload(nonce, key) if file.canOverwriteFileUploadStatus(Timestamp.now) && key == upscanKey =>
            FileUpload.Posted(nonce, Timestamp.now, key)
          case file => file
        })

      if(updatedFileUploads == files) {
        warn(s"[markFileAsPosted] No file with the supplied journeyID & key was updated and marked as posted")
        debug(s"[markFileAsPosted] No file with the supplied journeyID: '$journeyId' & key: '$upscanKey' was updated and marked as posted")
        Future.successful(None)
      } else {
        putFiles(updatedFileUploads).map(Some(_))
      }
    }

  def markFileAsRejected(s3UploadError: S3UploadError)
                        (implicit journeyId: String, journeyContext: FileUploadContext): Future[Option[CacheItem]] =

    withFiles[Option[CacheItem]](Future.successful(None)) { files =>

      logFailure(journeyContext, s3UploadError)

      val updatedFileUploads = FileUploads(files.files.map {
        case FileUpload(nonce, s3UploadError.key) =>
          FileUpload.Rejected(nonce, Timestamp.now, s3UploadError.key, s3UploadError)
        case file => file
      })

      if (updatedFileUploads == files) {
        warn(s"[markFileAsRejected] No file with the supplied journeyID & key was updated and marked as rejected")
        debug(s"[markFileAsRejected] No file with the supplied journeyID: '$journeyId' & key: '${s3UploadError.key}' was updated and marked as rejected")
        Future.successful(None)
      } else {
        putFiles(updatedFileUploads).map(Some(_))
      }
    }
}
