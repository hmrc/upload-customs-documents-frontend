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

import play.api.data.Form
import play.api.mvc.{Action, AnyContent, Call, Request}
import uk.gov.hmrc.uploaddocuments.forms.Forms
import uk.gov.hmrc.uploaddocuments.forms.Forms.YesNoChoiceForm
import uk.gov.hmrc.uploaddocuments.models.{FileUploadContext, FileUploads}
import uk.gov.hmrc.uploaddocuments.services.{FileUploadService, JourneyContextService}
import uk.gov.hmrc.uploaddocuments.views.html.UploadMultipleFilesView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ChooseMultipleFilesController @Inject() (
  components: BaseControllerComponents,
  uploadMultipleFilesView: UploadMultipleFilesView,
  override val journeyContextService: JourneyContextService,
  override val fileUploadService: FileUploadService
)(implicit ec: ExecutionContext)
    extends BaseController(components) with FileUploadsControllerHelper with JourneyContextControllerHelper {

  // GET /choose-files
  final val showChooseMultipleFiles: Action[AnyContent] = Action.async { implicit request =>
    whenInSession { implicit journeyId =>
      whenAuthenticated {
        withJourneyContext { journeyConfig =>
          withFileUploads { files =>
            val userWantsToUploadNextFile = journeyConfig.userWantsToUploadNextFile
            journeyContextService
              .putJourneyContext(journeyConfig.copy(userWantsToUploadNextFile = false))
              .map { _ =>
                if (preferUploadMultipleFiles && journeyConfig.config.features.showUploadMultiple) {
                  Ok(renderView(journeyConfig, files, YesNoChoiceForm))
                } else {
                  if (files.acceptedCount > 0 && !userWantsToUploadNextFile) {
                    Redirect(routes.SummaryController.showSummary)
                  } else {
                    Redirect(routes.ChooseSingleFileController.showChooseFile(None))
                  }
                }
              }
          }
        }
      }
    }
  }

  // POST /choose-files
  final val continueWithYesNo: Action[AnyContent] = Action.async { implicit request =>
    whenInSession { implicit journeyId =>
      whenAuthenticated {
        withJourneyContext { journeyConfig =>
          withFileUploads { files =>
            Forms.YesNoChoiceForm
              .bindFromRequest()
              .fold(
                formWithErrors => Future.successful(BadRequest(renderView(journeyConfig, files, formWithErrors))),
                {
                  case true =>
                    journeyContextService
                      .putJourneyContext(journeyConfig.copy(userWantsToUploadNextFile = true))
                      .map { _ =>
                        journeyConfig.config.continueAfterYesAnswerUrl
                          .orElse(journeyConfig.config.backlinkUrl) match {
                          case Some(location) => Redirect(location)
                          case None           => Ok(renderView(journeyConfig, files, Forms.YesNoChoiceForm))
                        }
                      }
                  case false =>
                    Future.successful(Redirect(routes.ContinueToHostController.continueToHost))
                }
              )
          }
        }
      }
    }
  }

  private def renderView(context: FileUploadContext, files: FileUploads, form: Form[Boolean])(implicit
    request: Request[_]
  ) =
    uploadMultipleFilesView(
      minimumNumberOfFiles = context.config.minimumNumberOfFiles,
      maximumNumberOfFiles = context.config.maximumNumberOfFiles,
      initialNumberOfEmptyRows = context.config.initialNumberOfEmptyRows,
      maximumFileSizeBytes = context.config.maximumFileSizeBytes,
      filePickerAcceptFilter = context.config.getFilePickerAcceptFilter,
      allowedFileTypesHint = context.config.content.allowedFilesTypesHint
        .orElse(context.config.allowedFileExtensions)
        .getOrElse(context.config.allowedContentTypes),
      context.config.newFileDescription,
      initialFileUploads = files.files,
      initiateNextFileUpload = routes.InitiateUpscanController.initiateNextFileUpload,
      checkFileVerificationStatus = routes.FileVerificationController.checkFileVerificationStatus,
      removeFile = routes.RemoveController.removeFileUploadByReferenceAsync,
      previewFile = routes.PreviewController.previewFileUploadByReference,
      markFileRejected = routes.FileRejectedController.markFileUploadAsRejectedAsync,
      continueAction = if (context.config.features.showYesNoQuestionBeforeContinue) {
        routes.ChooseMultipleFilesController.continueWithYesNo
      } else { routes.ContinueToHostController.continueToHost },
      backLink = context.config.backlinkUrl.map(Call("GET", _)),
      context.config.features.showYesNoQuestionBeforeContinue,
      context.config.content.getYesNoQuestionText,
      form
    )(request, context.messages, context.config.features, context.config.content)
}
