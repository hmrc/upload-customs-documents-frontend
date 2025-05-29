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

import org.apache.pekko.actor.{ActorSystem, Scheduler}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.MongoLockRepository
import uk.gov.hmrc.uploaddocuments.connectors.FileUploadResultPushConnector
import uk.gov.hmrc.uploaddocuments.connectors.FileUploadResultPushConnector.Response
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.models.fileUploadResultPush.Request
import uk.gov.hmrc.uploaddocuments.repository.JourneyCacheRepository.DataKeys
import uk.gov.hmrc.uploaddocuments.repository.{JourneyCacheRepository, JourneyLocking}
import uk.gov.hmrc.uploaddocuments.support.UploadLog
import uk.gov.hmrc.uploaddocuments.utils.LoggerUtil
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileUploadService @Inject() (
  repo: JourneyCacheRepository,
  fileUploadResultPushConnector: FileUploadResultPushConnector,
  override val lockRepositoryProvider: MongoLockRepository,
  override val appConfig: AppConfig,
  val actorSystem: ActorSystem
)(implicit ec: ExecutionContext)
    extends LoggerUtil with UploadLog with JourneyLocking {

  implicit lazy val scheduler: Scheduler = actorSystem.scheduler

  def getFiles(implicit journeyId: JourneyId): Future[Option[FileUploads]] =
    repo.get(journeyId.value)(DataKeys.uploadedFiles)

  def putFiles(files: FileUploads)(implicit journeyId: JourneyId): Future[FileUploads] =
    repo.put(journeyId.value)(DataKeys.uploadedFiles, files).map(_ => files)

  def wipeOut()(implicit journeyId: JourneyId): Future[Unit] =
    repo.delete(journeyId.value)(DataKeys.uploadedFiles)

  def withFiles[T](
    journeyNotFoundResult: => Future[T]
  )(f: FileUploads => Future[T])(implicit journeyId: JourneyId): Future[T] =
    getFiles.flatMap(_.fold {
      Logger.error("[withFiles] No files exist for the supplied journeyID")
      Logger.debug(s"[withFiles] journeyId: '$journeyId'")
      journeyNotFoundResult
    }(f))

  def removeFile(reference: String)(implicit
    hc: HeaderCarrier,
    journeyId: JourneyId,
    journeyContext: FileUploadContext
  ): Future[Option[(Response, FileUploads)]] =
    withFiles[Option[(Response, FileUploads)]](Future.successful(None)) { files =>
      for {
        updatedFiles <- putFiles(files.copy(files = files.files.filterNot(_.reference == reference)))
        result       <- fileUploadResultPushConnector.push(Request(journeyContext, updatedFiles))
      } yield Some(result -> updatedFiles)
    }

  def markFileAsPosted(key: String)(implicit journeyId: JourneyId): Future[Option[FileUploads]] =
    takeLock[Option[FileUploads]](Future.successful(None)) {
      withFiles[Option[FileUploads]](Future.successful(None)) { files =>
        val updatedFileUploads =
          FileUploads(files.files.map {
            case FileUpload.Initiated(nonce, _, `key`, _, _) => FileUpload.Posted(nonce, Timestamp.now, key)
            case file                                        => file
          })

        if (updatedFileUploads == files) {
          Logger.info(s"[markFileAsPosted] No file with the supplied journeyID & key was updated and marked as posted")
          Logger.debug(
            s"[markFileAsPosted] No file with the supplied journeyID: '$journeyId' & key: '$key' was updated and marked as posted"
          )
          Future.successful(None)
        } else {
          putFiles(updatedFileUploads).map(_ => Some(updatedFileUploads))
        }
      }
    }

  def markFileAsRejected(
    s3UploadError: S3UploadError
  )(implicit journeyId: JourneyId, journeyContext: FileUploadContext): Future[Option[FileUploads]] =
    takeLock[Option[FileUploads]](Future.successful(None)) {
      withFiles[Option[FileUploads]](Future.successful(None)) { files =>
        logFailure(journeyContext, s3UploadError)

        val updatedFileUploads = FileUploads(files.files.map {
          case FileUpload(nonce, s3UploadError.key) =>
            FileUpload.Rejected(nonce, Timestamp.now, s3UploadError.key, s3UploadError)
          case file => file
        })

        if (updatedFileUploads == files) {
          Logger.warn(
            s"[markFileAsRejected] No file with the supplied journeyID & key was updated and marked as rejected"
          )
          Logger.debug(
            s"[markFileAsRejected] No file with the supplied journeyID: '$journeyId' & key: '${s3UploadError.key}' was updated and marked as rejected"
          )
          Future.successful(None)
        } else {
          putFiles(updatedFileUploads).map(Some(_))
        }
      }
    }

  def markFileWithUpscanResponseAndNotifyHost(notification: UpscanNotification, requestNonce: Nonce)(implicit
    context: FileUploadContext,
    journeyId: JourneyId,
    hc: HeaderCarrier
  ): Future[Option[FileUploads]] =
    takeLock[Option[FileUploads]](Future.successful(None)) {
      withFiles[Option[FileUploads]](Future.successful(None)) { fileUploads =>
        for {
          updateFiles <- putFiles(updateFileUploadsWithUpscanResponse(notification, requestNonce, fileUploads))
          _ = if (fileUploads.files == updateFiles.files) {
                Logger.warn(
                  "[markFileWithUpscanResponseAndNotifyHost] No files were updated following the callback from Upscan"
                )
                Logger.debug(
                  s"[markFileWithUpscanResponseAndNotifyHost] No files were updated following the callback from Upscan. journeyId: '$journeyId', upscanRef: '${notification.reference}'"
                )
              }
          _ <- if (updateFiles.acceptedCount != fileUploads.acceptedCount) {
                 fileUploadResultPushConnector.push(Request(context, updateFiles))
               } else Future.successful(Right((): Unit))
        } yield Some(updateFiles)
      }
    }

  def markFileWithUpscanResponse(notification: UpscanNotification, requestNonce: Nonce)(implicit
    context: FileUploadContext,
    journeyId: JourneyId,
    hc: HeaderCarrier
  ): Future[Option[FileUploads]] =
    takeLock[Option[FileUploads]](Future.successful(None)) {
      withFiles[Option[FileUploads]](Future.successful(None)) { fileUploads =>
        for {
          updateFiles <- putFiles(updateFileUploadsWithUpscanResponse(notification, requestNonce, fileUploads))
          _ = if (fileUploads.files == updateFiles.files) {
                Logger.warn(
                  "[markFileWithUpscanResponse] No files were updated following the callback from Upscan"
                )
                Logger.debug(
                  s"[markFileWithUpscanResponse] No files were updated following the callback from Upscan. journeyId: '$journeyId', upscanRef: '${notification.reference}'"
                )
              }
        } yield Some(updateFiles)
      }
    }

  private def updateFileUploadsWithUpscanResponse(
    notification: UpscanNotification,
    requestNonce: Nonce,
    fileUploads: FileUploads
  )(implicit context: FileUploadContext) =
    FileUploads(fileUploads.files.map {
      case fileUpload if fileUpload.nonce == requestNonce =>
        notification match {
          case UpscanFileReady(_, url, uploadDetails) =>
            fileUploads.files
              .collectFirst {
                case file: FileUpload.Accepted
                    if file.checksum == uploadDetails.checksum && file.reference != notification.reference =>
                  FileUpload.Duplicate(
                    fileUpload.nonce,
                    Timestamp.now,
                    fileUpload.reference,
                    uploadDetails.checksum,
                    existingFileName = file.fileName,
                    duplicateFileName = uploadDetails.fileName
                  )
              }
              .getOrElse {
                logSuccess(context, uploadDetails, fileUpload.timestamp)
                FileUpload.Accepted(
                  fileUpload.nonce,
                  Timestamp.now,
                  fileUpload.reference,
                  url,
                  uploadDetails.uploadTimestamp,
                  uploadDetails.checksum,
                  FileUpload.sanitizeFileName(uploadDetails.fileName),
                  uploadDetails.fileMimeType,
                  uploadDetails.size,
                  description = context.config.newFileDescription
                )
              }
          case UpscanFileFailed(_, failureDetails) =>
            logFailure(context, failureDetails, fileUpload.timestamp)
            FileUpload.Failed(fileUpload.nonce, Timestamp.now, fileUpload.reference, failureDetails)
        }
      case u => u
    })
}
