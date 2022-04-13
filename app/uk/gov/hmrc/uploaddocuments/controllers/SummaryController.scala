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
import uk.gov.hmrc.uploaddocuments.controllers.Forms.YesNoChoiceForm
import uk.gov.hmrc.uploaddocuments.journeys.State
import uk.gov.hmrc.uploaddocuments.models.{FileUploadContext, FileUploads}
import uk.gov.hmrc.uploaddocuments.views.html.{SummaryNoChoiceView, SummaryView}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SummaryController @Inject() (
  val router: Router,
  renderer: Renderer,
  view: SummaryView,
  viewNoChoice: SummaryNoChoiceView,
  components: BaseControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController(components) with UpscanRequestSupport {

  // GET /summary
  final val showSummary: Action[AnyContent] =
    Action.async { implicit request =>
      whenInSession {
        whenAuthenticated {
          withJourneyContext { journeyConfig =>
            withUploadedFiles { files =>
              Future(Ok(renderView(YesNoChoiceForm, journeyConfig, files, List())))
            }
          }
        }
      }
    }

  private def renderView(
    form: Form[Boolean],
    context: FileUploadContext,
    fileUploads: FileUploads,
    breadcrumbs: List[State]
  )(implicit request: Request[_]) =
    if (fileUploads.acceptedCount < context.config.maximumNumberOfFiles)
      view(
        maxFileUploadsNumber = context.config.maximumNumberOfFiles,
        maximumFileSizeBytes = context.config.maximumFileSizeBytes,
        form = form,
        fileUploads = fileUploads,
        postAction = routes.SummaryController.submitUploadAnotherFileChoice,
        previewFileCall = routes.PreviewController.previewFileUploadByReference,
        removeFileCall = routes.RemoveController.removeFileUploadByReference,
        backLink = renderer.backlink(breadcrumbs)
      )(implicitly[Request[_]], context.messages, context.config.features, context.config.content)
    else
      viewNoChoice(
        maxFileUploadsNumber = context.config.maximumNumberOfFiles,
        fileUploads = fileUploads,
        postAction = routes.ContinueToHostController.continueToHost,
        previewFileCall = routes.PreviewController.previewFileUploadByReference,
        removeFileCall = routes.RemoveController.removeFileUploadByReference,
        backLink = Call("GET", context.config.backlinkUrl)
      )(implicitly[Request[_]], context.messages, context.config.features, context.config.content)

  // POST /summary
  final val submitUploadAnotherFileChoice: Action[AnyContent] =
    Action.async { implicit request =>
      whenInSession {
        whenAuthenticated {
          withJourneyContext { journeyContext =>
            withUploadedFiles { files =>
              Forms.YesNoChoiceForm.bindFromRequest
                .fold(
                  formWithErrors => Future(BadRequest(renderView(formWithErrors, journeyContext, files, List()))),
                  {
                    case true if files.initiatedOrAcceptedCount < journeyContext.config.maximumNumberOfFiles =>
                      Future(Redirect(routes.ChooseSingleFileController.showChooseFile))
                    case _ =>
                      Future(Redirect(routes.ContinueToHostController.continueToHost))
                  }
                )
            }
          }
        }
      }
    }
}
