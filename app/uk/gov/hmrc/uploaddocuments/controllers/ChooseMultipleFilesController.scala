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

package uk.gov.hmrc.uploaddocuments.controllers

import play.api.mvc.{Action, AnyContent, Call, Request}
import uk.gov.hmrc.uploaddocuments.models.{FileUploadContext, FileUploads, UploadRequest}
import uk.gov.hmrc.uploaddocuments.services.{FileUploadService, InitiateUpscanService, JourneyContextService}
import uk.gov.hmrc.uploaddocuments.views.html.UploadMultipleFilesView

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ChooseMultipleFilesController @Inject() (
  components: BaseControllerComponents,
  uploadMultipleFilesView: UploadMultipleFilesView,
  initiateUpscanService: InitiateUpscanService,
  override val journeyContextService: JourneyContextService,
  override val fileUploadService: FileUploadService
)(using ExecutionContext)
    extends BaseController(components) with FileUploadsControllerHelper with JourneyContextControllerHelper {

  /** An Initiated upload older than this must not be reused; its pre-signed S3 request may have expired. */
  private val initiatedUploadReuseMaxAgeMillis: Long = 30.minutes.toMillis

  // GET /choose-files
  final val showChooseMultipleFiles: Action[AnyContent] = Action.async { implicit request =>
    whenInSession { implicit journeyId =>
      whenAuthenticated {
        withJourneyContext { implicit journeyConfig =>
          withFileUploads { files =>
            val showMinimumError =
              request.getQueryString("error").contains("minimum") && journeyConfig.isBelowMinimumFiles(files)
            val showFileRequiredError = request.getQueryString("error").contains("fileRequired")
            if (files.acceptedOrPostedCount >= journeyConfig.config.maximumNumberOfFiles)
              Future.successful(
                Ok(renderView(journeyConfig, files.withoutInitiated, None, showMinimumError = false))
              )
            else
              files.findInitiatedWithRequest.filter(_.timestamp.duration < initiatedUploadReuseMaxAgeMillis) match {
                case Some(initiated) =>
                  Future.successful(
                    Ok(
                      renderView(
                        journeyConfig,
                        files.withoutInitiated,
                        initiated.uploadRequest,
                        showMinimumError,
                        showFileRequiredError
                      )
                    )
                  )
                case None =>
                  initiateUpscanService.initiateNextFileUpload().map { case (upscanResponse, updatedFiles) =>
                    Ok(
                      renderView(
                        journeyConfig,
                        updatedFiles.withoutInitiated,
                        Some(upscanResponse.uploadRequest),
                        showMinimumError,
                        showFileRequiredError
                      )
                    )
                  }
              }
          }
        }
      }
    }
  }

  private def renderView(
    context: FileUploadContext,
    files: FileUploads,
    uploadRequest: Option[UploadRequest],
    showMinimumError: Boolean,
    showFileRequiredError: Boolean = false
  )(using request: Request[_]) =
    uploadMultipleFilesView(
      maximumNumberOfFiles = context.config.maximumNumberOfFiles,
      maximumFileSizeBytes = context.config.maximumFileSizeBytes,
      allowedFileTypesHint = context.config.content.allowedFilesTypesHint
        .orElse(context.config.allowedFileExtensions)
        .getOrElse(context.config.allowedContentTypes),
      newFileDescription = context.config.newFileDescription,
      uploadRequest = uploadRequest,
      fileUploads = files,
      removeFileCall = routes.RemoveController.removeFileUploadByReference,
      previewFileCall = routes.PreviewController.previewFileUploadByReference,
      statusCall = routes.FileVerificationController.checkFileVerificationStatus,
      continueAction = routes.ContinueToHostController.continueToHost,
      uploadAnotherTypeUrl =
        if (
          context.config.features.showYesNoQuestionBeforeContinue && context.config.continueAfterYesAnswerUrl.isDefined
        )
          Some(routes.ContinueToHostController.uploadAnotherType.url)
        else None,
      filePickerAcceptFilter = context.config.getFilePickerAcceptFilter,
      backLink = context.config.backlinkUrl.map(Call("GET", _)),
      showMinimumError = showMinimumError,
      showFileRequiredError = showFileRequiredError
    )(request, context.messages, context.config.features, context.config.content)
}
