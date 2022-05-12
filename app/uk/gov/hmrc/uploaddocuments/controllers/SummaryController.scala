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
import play.api.mvc.{Action, AnyContent, Call}
import uk.gov.hmrc.uploaddocuments.controllers.actions.{AuthAction, JourneyContextAction}
import uk.gov.hmrc.uploaddocuments.forms.Forms
import uk.gov.hmrc.uploaddocuments.forms.Forms.YesNoChoiceForm
import uk.gov.hmrc.uploaddocuments.models.FileUploads
import uk.gov.hmrc.uploaddocuments.models.requests.JourneyContextRequest
import uk.gov.hmrc.uploaddocuments.services.FileUploadService
import uk.gov.hmrc.uploaddocuments.views.html.{SummaryNoChoiceView, SummaryView}

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SummaryController @Inject()(view: SummaryView,
                                  viewNoChoice: SummaryNoChoiceView,
                                  components: BaseControllerComponents,
                                  @Named("authenticated") auth: AuthAction,
                                  journeyContext: JourneyContextAction,
                                  override val fileUploadService: FileUploadService)
                                 (implicit ec: ExecutionContext) extends BaseController(components) with FileUploadsControllerHelper {

  // GET /summary
  final val showSummary: Action[AnyContent] = (auth andThen journeyContext).async { implicit request =>
    withFileUploads { files =>
      Future.successful(Ok(renderView(YesNoChoiceForm, files)))
    }
  }

  // POST /summary
  final val submitUploadAnotherFileChoice: Action[AnyContent] = (auth andThen journeyContext).async { implicit request =>
    withFileUploads { files =>
      Future.successful(Forms.YesNoChoiceForm.bindFromRequest.fold(
        formWithErrors => BadRequest(renderView(formWithErrors, files)), {
          case true if files.initiatedOrAcceptedCount < request.journeyContext.config.maximumNumberOfFiles =>
            val redirect = request.journeyContext.config.continueAfterYesAnswerUrl.getOrElse(routes.ChooseSingleFileController.showChooseFile(Some(true)).url)
            Redirect(redirect)
          case _ =>
            Redirect(routes.ContinueToHostController.continueToHost)
        }
      ))
    }
  }

  private def renderView(form: Form[Boolean], fileUploads: FileUploads)
                        (implicit request: JourneyContextRequest[_]) = {

    val config = request.journeyContext.config

    if (fileUploads.acceptedCount < config.maximumNumberOfFiles)
      view(
        maxFileUploadsNumber = config.maximumNumberOfFiles,
        maximumFileSizeBytes = config.maximumFileSizeBytes,
        form                 = form,
        fileUploads          = fileUploads,
        postAction           = routes.SummaryController.submitUploadAnotherFileChoice,
        previewFileCall      = routes.PreviewController.previewFileUploadByReference,
        removeFileCall       = routes.RemoveController.removeFileUploadByReference,
        backLink             = routes.ChooseSingleFileController.showChooseFile(None)
      )(request, request.journeyContext.messages, config.features, config.content)
    else
      viewNoChoice(
        maxFileUploadsNumber = config.maximumNumberOfFiles,
        fileUploads          = fileUploads,
        postAction           = routes.ContinueToHostController.continueToHost,
        previewFileCall      = routes.PreviewController.previewFileUploadByReference,
        removeFileCall       = routes.RemoveController.removeFileUploadByReference,
        backLink             = Call("GET", config.backlinkUrl)
      )(request, request.journeyContext.messages, config.features, config.content)
  }
}
