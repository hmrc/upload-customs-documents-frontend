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
import uk.gov.hmrc.uploaddocuments.forms.Forms
import uk.gov.hmrc.uploaddocuments.forms.Forms.YesNoChoiceForm
import uk.gov.hmrc.uploaddocuments.models.{FileUploadContext, FileUploads}
import uk.gov.hmrc.uploaddocuments.services.{FileUploadService, JourneyContextService}
import uk.gov.hmrc.uploaddocuments.views.html.{SummaryNoChoiceView, SummaryView}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SummaryController @Inject()(view: SummaryView,
                                  viewNoChoice: SummaryNoChoiceView,
                                  components: BaseControllerComponents,
                                  override val journeyContextService: JourneyContextService,
                                  override val fileUploadService: FileUploadService)
                                 (implicit ec: ExecutionContext)
  extends BaseController(components) with FileUploadsControllerHelper with JourneyContextControllerHelper {

  // GET /summary
  final val showSummary: Action[AnyContent] = Action.async { implicit request =>
    whenInSession { implicit journeyId =>
      whenAuthenticated {
        withJourneyContext { journeyConfig =>
          withFileUploads { files =>
            Future(Ok(renderView(YesNoChoiceForm, journeyConfig, files)))
          }
        }
      }
    }
  }

  // POST /summary
  final val submitUploadAnotherFileChoice: Action[AnyContent] = Action.async { implicit request =>
    whenInSession { implicit journeyId =>
      whenAuthenticated {
        withJourneyContext { journeyContext =>
          withFileUploads { files =>
            Forms.YesNoChoiceForm.bindFromRequest
              .fold(
                formWithErrors => Future(BadRequest(renderView(formWithErrors, journeyContext, files))), {
                  case true if files.initiatedOrAcceptedCount < journeyContext.config.maximumNumberOfFiles =>
                    Future.successful(Redirect(routes.ChooseSingleFileController.showChooseFile))
                  case _ =>
                    Future.successful(Redirect(routes.ContinueToHostController.continueToHost))
                }
              )
          }
        }
      }
    }
  }

  private def renderView(form: Form[Boolean], context: FileUploadContext, fileUploads: FileUploads)(
    implicit request: Request[_]) =
    if (fileUploads.acceptedCount < context.config.maximumNumberOfFiles)
      view(
        maxFileUploadsNumber = context.config.maximumNumberOfFiles,
        maximumFileSizeBytes = context.config.maximumFileSizeBytes,
        form                 = form,
        fileUploads          = fileUploads,
        postAction           = routes.SummaryController.submitUploadAnotherFileChoice,
        previewFileCall      = routes.PreviewController.previewFileUploadByReference,
        removeFileCall       = routes.RemoveController.removeFileUploadByReference,
        backLink             = routes.StartController.start // TODO: Back Linking needs fixing! Set to start by default for now!!!
      )(implicitly[Request[_]], context.messages, context.config.features, context.config.content)
    else
      viewNoChoice(
        maxFileUploadsNumber = context.config.maximumNumberOfFiles,
        fileUploads          = fileUploads,
        postAction           = routes.ContinueToHostController.continueToHost,
        previewFileCall      = routes.PreviewController.previewFileUploadByReference,
        removeFileCall       = routes.RemoveController.removeFileUploadByReference,
        backLink             = Call("GET", context.config.backlinkUrl)
      )(implicitly[Request[_]], context.messages, context.config.features, context.config.content)
}
