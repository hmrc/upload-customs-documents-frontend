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
import uk.gov.hmrc.uploaddocuments.models.{FileUpload, FileUploads, Timestamp}
import uk.gov.hmrc.uploaddocuments.repository.JourneyCacheRepository
import uk.gov.hmrc.uploaddocuments.repository.JourneyCacheRepository.DataKeys
import uk.gov.hmrc.uploaddocuments.utils.LoggerUtil

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileUploadService @Inject()(repo: JourneyCacheRepository)
                                 (implicit ec: ExecutionContext) extends LoggerUtil {

  private[services] def getFiles()(implicit journeyId: String): Future[Option[FileUploads]] =
    repo.get(journeyId)(DataKeys.uploadedFiles)

  private[services] def putFiles(files: FileUploads)
                                (implicit journeyId: String): Future[CacheItem] =
    repo.put(journeyId)(DataKeys.uploadedFiles, files)

  def markFileAsPosted(upscanKey: String)
                      (implicit journeyId: String): Future[Option[CacheItem]] = {
    getFiles() flatMap {
      case None =>
        error("[markFileAsPosted] No files exist for the supplied journeyID")
        debug(s"[markFileAsPosted] journeyId: '$journeyId'")
        Future.successful(None)
      case Some(files) =>
        val now = Timestamp.now
        val updatedFileUploads =
          FileUploads(files.files.map {
            case fu@FileUpload(nonce, key) if fu.canOverwriteFileUploadStatus(now) && key == upscanKey =>
              FileUpload.Posted(nonce, Timestamp.now, key)
            case u => u
          })

        if(updatedFileUploads == files) {
          warn(s"[markFileAsPosted] No file with the supplied journeyID & key was updated and marked as posted")
          debug(s"[markFileAsPosted] No file with the supplied journeyID: '$journeyId' & key: '$upscanKey' was updated and marked as posted")
          Future.successful(None)
        } else {
          putFiles(updatedFileUploads).map(Some(_))
        }
    }
  }
}
