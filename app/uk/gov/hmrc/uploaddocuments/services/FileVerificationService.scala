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

import akka.actor.Scheduler
import play.api.i18n.Messages
import uk.gov.hmrc.uploaddocuments.controllers.routes
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.utils.LoggerUtil

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileVerificationService @Inject()(fileUploadService: FileUploadService) extends LoggerUtil {

  def waitForUpscanResponse[T](upscanReference: String,
                               intervalInMiliseconds: Long,
                               timeoutNanoTime: Long)
                              (readyResult: FileUpload => Future[T],
                               ifTimeout: => Future[T])
                              (implicit scheduler: Scheduler, ec: ExecutionContext, journeyId: JourneyId): Future[T] =

    fileUploadService.withFiles[T](throw new Exception("No JourneyID found for supplied journeyID")) {
      _.files.find(_.reference == upscanReference) match {
        case Some(file) if file.isReady =>
          Logger.info(s"[waitForUpscanResponse] Response received from Upscan for reference: '$upscanReference' and file is ready")
          readyResult(file)
        case Some(_) if System.nanoTime() > timeoutNanoTime =>
          Logger.info(s"[waitForUpscanResponse] Timed out waiting for a response from Upscan for reference: '$upscanReference'")
          ifTimeout
        case Some(_) =>
          Logger.info(s"[waitForUpscanResponse] Waiting $intervalInMiliseconds milliseconds for a response from Upscan for reference: '$upscanReference'")
          ScheduleAfter(intervalInMiliseconds) {
            waitForUpscanResponse(upscanReference, intervalInMiliseconds * 2, timeoutNanoTime)(readyResult, ifTimeout)
          }
        case None =>
          Logger.error(s"[waitForUpscanResponse] No file found for the supplied upscanReference: '$upscanReference'")
          throw new MatchError(s"No file found for the supplied upscanReference: '$upscanReference'")
      }
    }

  def getFileVerificationStatus(reference: String)
                               (implicit journeyContext: FileUploadContext, messages: Messages, journeyId: JourneyId): Future[Option[FileVerificationStatus]] =
    fileUploadService.withFiles[Option[FileVerificationStatus]](Future.successful(None)) { files =>
      Future.successful(files.files.find(_.reference == reference) map { file =>
        FileVerificationStatus(
          fileUpload = file,
          filePreviewUrl = routes.PreviewController.previewFileUploadByReference,
          maximumFileSizeBytes = journeyContext.config.maximumFileSizeBytes.toInt,
          allowedFileTypesHint = journeyContext.config.content.allowedFilesTypesHint
            .orElse(journeyContext.config.allowedFileExtensions)
            .getOrElse(journeyContext.config.allowedContentTypes)
        )
      })
    }
}
