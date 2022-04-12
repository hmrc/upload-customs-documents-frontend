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

import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.uploaddocuments.journeys.{JourneyModel, State}
import uk.gov.hmrc.uploaddocuments.models.{FileUploadContext, FileUploads}
import uk.gov.hmrc.uploaddocuments.services.SessionStateService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ContinueToHostController @Inject() (
  sessionStateService: SessionStateService,
  router: Router,
  components: BaseControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController(components) {

  // GET /continue-to-host
  final val continueToHost: Action[AnyContent] =
    Action.async { implicit request =>
      whenInSession {
        whenAuthenticated {
          withJourneyContext { journeyConfig =>
            withUploadedFiles { files =>
              val sessionStateUpdate =
                JourneyModel.continueToHost
              sessionStateService
                .getCurrentOrUpdateSessionState[State.ContinueToHost](sessionStateUpdate)
                .map {
                  case (continueToHost: State.ContinueToHost, _) =>
                    // TODO: Switch to use value from 'WithUploadedFiles' once this collection is actually updated in parallel
                    Redirect(redirectRoute(continueToHost.fileUploads, journeyConfig))

                  case other =>
                    router.redirectTo(other)
                }
                .andThen { case _ => sessionStateService.cleanBreadcrumbs }
            }
          }
        }
      }
    }

  private def redirectRoute(fileUploads: FileUploads, context: FileUploadContext) =
    if (fileUploads.acceptedCount == 0)
      context.config.getContinueWhenEmptyUrl
    else if (fileUploads.acceptedCount >= context.config.maximumNumberOfFiles)
      context.config.getContinueWhenFullUrl
    else
      context.config.continueUrl

  // POST /continue-to-host
  final val continueWithYesNo: Action[AnyContent] =
    Action.async { implicit request =>
      whenInSession {
        whenAuthenticated {
          Forms.YesNoChoiceForm.bindFromRequest
            .fold(
              formWithErrors => sessionStateService.currentSessionState.map(router.redirectWithForm(formWithErrors)),
              choice => {
                val sessionStateUpdate =
                  JourneyModel.continueWithYesNo(choice)
                sessionStateService
                  .updateSessionState(sessionStateUpdate)
                  .map(router.redirectTo)
              }
            )
        }
      }
    }
}
