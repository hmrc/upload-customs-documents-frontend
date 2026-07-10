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

  // GET /choose-files
  final val showChooseMultipleFiles: Action[AnyContent] = Action.async { implicit request =>
    whenInSession { implicit journeyId =>
      whenAuthenticated {
        withJourneyContext { implicit journeyConfig =>
          initiateUpscanService.initiateNextFileUpload().flatMap { maybeInitiated =>
            withFileUploads { files =>
              Future.successful(
                Ok(
                  renderView(
                    journeyConfig,
                    files.withoutInitiated,
                    maybeInitiated.map(_._1.uploadRequest)
                  )
                )
              )
            }
          }
        }
      }
    }
  }

  private def renderView(
    context: FileUploadContext,
    files: FileUploads,
    uploadRequest: Option[UploadRequest]
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
        if (context.config.features.showYesNoQuestionBeforeContinue) context.config.continueAfterYesAnswerUrl else None,
      filePickerAcceptFilter = context.config.getFilePickerAcceptFilter,
      backLink = context.config.backlinkUrl.map(Call("GET", _))
    )(request, context.messages, context.config.features, context.config.content)
}
