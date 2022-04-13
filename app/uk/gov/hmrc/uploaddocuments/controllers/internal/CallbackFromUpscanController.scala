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

package uk.gov.hmrc.uploaddocuments.controllers.internal

import play.api.libs.json.JsValue
import play.api.mvc.Action
import uk.gov.hmrc.uploaddocuments.connectors.FileUploadResultPushConnector
import uk.gov.hmrc.uploaddocuments.controllers.{BaseController, BaseControllerComponents}
import uk.gov.hmrc.uploaddocuments.journeys.JourneyModel.canOverwriteFileUploadStatus
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.repository.NewJourneyCacheRepository.DataKeys
import uk.gov.hmrc.uploaddocuments.support.UploadLog
import uk.gov.hmrc.uploaddocuments.utils.LoggerUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CallbackFromUpscanController @Inject()(fileUploadResultPushConnector: FileUploadResultPushConnector,
                                             components: BaseControllerComponents)
                                            (implicit ec: ExecutionContext) extends BaseController(components) with LoggerUtil {

  // POST /callback-from-upscan/journey/:journeyId/:nonce
  final def callbackFromUpscan(journeyId: String, nonce: String): Action[JsValue] =
    Action.async(parse.tolerantJson) { implicit request =>
      withJsonBody[UpscanNotification] { payload =>
        whenInSession {
          withJourneyContext { journeyContext =>
            withUploadedFiles { files =>
              // TODO: Hard Coded Boolean - need to investigate if this is actually needed...
              //      My assumption would be, that whenever we get a response from Upscan it should udpate the stored record.
              updateFileUploads(payload, Nonce(nonce), files, true, journeyContext) match {
                case (uploads, newlyAccepted) =>
                  for {
                    _ <- components.newJourneyCacheRepository.put(currentJourneyId)(DataKeys.uploadedFiles, uploads)
                    _ <- if (newlyAccepted) {
                      fileUploadResultPushConnector.push(FileUploadResultPushConnector.Request.from(journeyContext, uploads))
                    } else Future.successful(Right())
                  } yield NoContent
              }
            }
          }
        }
      }
    }

  // TODO: This may want refactoring - it was lifted from the JourneyModel
  def updateFileUploads(notification: UpscanNotification,
                        requestNonce: Nonce,
                        fileUploads: FileUploads,
                        allowStatusOverwrite: Boolean,
                        context: FileUploadContext): (FileUploads, Boolean) = {
    val now = Timestamp.now
    val modifiedFileUploads = fileUploads.copy(files = fileUploads.files.map {
      // update status of the file with matching nonce
      case fileUpload @ FileUpload(nonce, reference, _)
        if nonce.value == requestNonce.value && canOverwriteFileUploadStatus(
          fileUpload,
          allowStatusOverwrite,
          now
        ) =>
        notification match {
          case UpscanFileReady(_, url, uploadDetails) =>
            // check for existing file uploads with duplicated checksum
            val modifiedFileUpload: FileUpload = fileUploads.files
              .find(file =>
                file.checksumOpt.contains(uploadDetails.checksum) && file.reference != notification.reference
              ) match {
              case Some(existingFileUpload: FileUpload.Accepted) =>
                FileUpload.Duplicate(
                  nonce,
                  Timestamp.now,
                  reference,
                  uploadDetails.checksum,
                  existingFileName = existingFileUpload.fileName,
                  duplicateFileName = uploadDetails.fileName
                )
              case _ =>
                UploadLog.success(context, uploadDetails, fileUpload.timestamp)
                FileUpload.Accepted(
                  nonce,
                  Timestamp.now,
                  reference,
                  url,
                  uploadDetails.uploadTimestamp,
                  uploadDetails.checksum,
                  FileUpload.sanitizeFileName(uploadDetails.fileName),
                  uploadDetails.fileMimeType,
                  uploadDetails.size,
                  description = context.config.newFileDescription
                )
            }
            modifiedFileUpload

          case UpscanFileFailed(_, failureDetails) =>
            UploadLog.failure(context, failureDetails, fileUpload.timestamp)
            FileUpload.Failed(
              nonce,
              Timestamp.now,
              reference,
              failureDetails
            )
        }
      case u => u
    })
    (modifiedFileUploads, modifiedFileUploads.acceptedCount != fileUploads.acceptedCount)
  }

}
