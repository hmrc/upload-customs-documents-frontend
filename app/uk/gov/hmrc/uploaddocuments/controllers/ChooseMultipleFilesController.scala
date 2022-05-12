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

package uk.gov.hmrc.uploaddocuments.controllers

import play.api.data.Form
import play.api.mvc.{Action, AnyContent, Call, Request}
import uk.gov.hmrc.uploaddocuments.controllers.actions.{AuthAction, AuthenticatedAction, JourneyContextAction}
import uk.gov.hmrc.uploaddocuments.forms.Forms
import uk.gov.hmrc.uploaddocuments.forms.Forms.YesNoChoiceForm
import uk.gov.hmrc.uploaddocuments.models.requests.JourneyContextRequest
import uk.gov.hmrc.uploaddocuments.models.{FileUploadContext, FileUploads}
import uk.gov.hmrc.uploaddocuments.services.{FileUploadService, JourneyContextService}
import uk.gov.hmrc.uploaddocuments.views.html.UploadMultipleFilesView

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ChooseMultipleFilesController @Inject()(components: BaseControllerComponents,
                                              uploadMultipleFilesView: UploadMultipleFilesView,
                                              override val fileUploadService: FileUploadService,
                                              @Named("authenticated") auth: AuthAction,
                                              journeyContext: JourneyContextAction)
                                             (implicit ec: ExecutionContext) extends BaseController(components) with FileUploadsControllerHelper {

  // GET /choose-files
  final val showChooseMultipleFiles: Action[AnyContent] = (auth andThen journeyContext).async { implicit request =>
    withFileUploads { files =>
      Future.successful {
        if (preferUploadMultipleFiles && request.journeyContext.config.features.showUploadMultiple) {
          Ok(renderView(files, YesNoChoiceForm))
        } else {
          if (files.acceptedCount > 0) {
            Redirect(routes.SummaryController.showSummary)
          } else {
            Redirect(routes.ChooseSingleFileController.showChooseFile(None))
          }
        }
      }
    }
  }

  // POST /choose-files
  final val continueWithYesNo: Action[AnyContent] = (auth andThen journeyContext).async { implicit request =>
    withFileUploads { files =>
      Forms.YesNoChoiceForm.bindFromRequest
        .fold(
          formWithErrors => Future.successful(BadRequest(renderView(files, formWithErrors))),
          {
            case true =>
              Future.successful(Redirect(request.journeyContext.config.continueAfterYesAnswerUrl.getOrElse(request.journeyContext.config.backlinkUrl)))
            case false =>
              Future.successful(Redirect(routes.ContinueToHostController.continueToHost))
          }
        )
    }
  }

  private def renderView(files: FileUploads, form: Form[Boolean])(
    implicit request: JourneyContextRequest[_]) = {
    val config = request.journeyContext.config
    uploadMultipleFilesView(
      minimumNumberOfFiles     = config.minimumNumberOfFiles,
      maximumNumberOfFiles     = config.maximumNumberOfFiles,
      initialNumberOfEmptyRows = config.initialNumberOfEmptyRows,
      maximumFileSizeBytes     = config.maximumFileSizeBytes,
      filePickerAcceptFilter   = config.getFilePickerAcceptFilter,
      allowedFileTypesHint = config.content.allowedFilesTypesHint
        .orElse(config.allowedFileExtensions)
        .getOrElse(config.allowedContentTypes),
      config.newFileDescription,
      initialFileUploads          = files.files,
      initiateNextFileUpload      = routes.InitiateUpscanController.initiateNextFileUpload,
      checkFileVerificationStatus = routes.FileVerificationController.checkFileVerificationStatus,
      removeFile                  = routes.RemoveController.removeFileUploadByReferenceAsync,
      previewFile                 = routes.PreviewController.previewFileUploadByReference,
      markFileRejected            = routes.FileRejectedController.markFileUploadAsRejectedAsync,
      continueAction = if (config.features.showYesNoQuestionBeforeContinue) {
        routes.ChooseMultipleFilesController.continueWithYesNo
      } else { routes.ContinueToHostController.continueToHost },
      backLink = Call("GET", config.backlinkUrl),
      config.features.showYesNoQuestionBeforeContinue,
      config.content.yesNoQuestionText,
      form
    )(request, request.journeyContext.messages, config.features, config.content)
  }
}
