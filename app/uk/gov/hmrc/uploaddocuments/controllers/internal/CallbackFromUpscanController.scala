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
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.repository.JourneyCacheRepository.DataKeys
import uk.gov.hmrc.uploaddocuments.support.UploadLog

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CallbackFromUpscanController @Inject()(
  fileUploadResultPushConnector: FileUploadResultPushConnector,
  components: BaseControllerComponents)(implicit ec: ExecutionContext)
    extends BaseController(components) with UploadLog {

  // POST /callback-from-upscan/journey/:journeyId/:nonce
  final def callbackFromUpscan(journeyId: JourneyId, nonce: String): Action[JsValue] =
    Action.async(parse.tolerantJson) { implicit request =>
      withJsonBody[UpscanNotification] { payload =>
        implicit val journey: JourneyId = journeyId
        withJourneyContext { journeyContext =>
          withUploadedFiles { files =>
            // TODO: Hard Coded Boolean - need to investigate if this is actually needed...
            //      My assumption would be, that whenever we get a response from Upscan it should udpate the stored record.
            val uploads = updateFileUploads(payload, Nonce(nonce), files, journeyContext)
            for {
              _ <- components.newJourneyCacheRepository.put(journeyId.value)(DataKeys.uploadedFiles, uploads)
              _ <- if (uploads.acceptedCount != files.acceptedCount) {
                    fileUploadResultPushConnector.push(FileUploadResultPushConnector.Request(journeyContext, uploads))
                  } else Future.successful(Right((): Unit))
            } yield NoContent
          }
        }
      }
    }

  def updateFileUploads(
    notification: UpscanNotification,
    requestNonce: Nonce,
    fileUploads: FileUploads,
    context: FileUploadContext): FileUploads = {
    val now = Timestamp.now
    FileUploads(fileUploads.files.map {
      case fileUpload if fileUpload.nonce == requestNonce && fileUpload.canOverwriteFileUploadStatus(now) =>
        notification match {
          case UpscanFileReady(_, url, uploadDetails) =>
            fileUploads.files
              .collectFirst {
                // check for existing Accepted file uploads with duplicated checksum
                case file: FileUpload.Accepted
                    if file.checksum == uploadDetails.checksum && file.reference != notification.reference =>
                  FileUpload.Duplicate(
                    fileUpload.nonce,
                    Timestamp.now,
                    fileUpload.reference,
                    uploadDetails.checksum,
                    existingFileName  = file.fileName,
                    duplicateFileName = uploadDetails.fileName)
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
            // update status of the file with matching nonce
            logFailure(context, failureDetails, fileUpload.timestamp)
            FileUpload.Failed(fileUpload.nonce, Timestamp.now, fileUpload.reference, failureDetails)
        }
      case u => u
    })
  }

}
