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

import play.api.http.HeaderNames
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.uploaddocuments.journeys.JourneyModel
import uk.gov.hmrc.uploaddocuments.services.SessionStateService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class FileRejectedController @Inject() (
  sessionStateService: SessionStateService,
  router: Router,
  renderer: Renderer,
  components: BaseControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController(components) {

  // GET /file-rejected
  final val markFileUploadAsRejected: Action[AnyContent] =
    Action.async { implicit request =>
      whenInSession {
        whenAuthenticated {
          Forms.UpscanUploadErrorForm.bindFromRequest
            .fold(
              formWithErrors => sessionStateService.currentSessionState.map(router.redirectWithForm(formWithErrors)),
              s3UploadError => {
                val sessionStateUpdate =
                  JourneyModel.markUploadAsRejected(s3UploadError)
                sessionStateService
                  .updateSessionState(sessionStateUpdate)
                  .map(router.redirectTo)
              }
            )
        }
      }
    }

  // POST /file-rejected
  final val markFileUploadAsRejectedAsync: Action[AnyContent] =
    Action.async { implicit request =>
      whenInSession {
        whenAuthenticated {
          Forms.UpscanUploadErrorForm.bindFromRequest
            .fold(
              formWithErrors => sessionStateService.currentSessionState.map(router.redirectWithForm(formWithErrors)),
              s3UploadError => {
                val sessionStateUpdate =
                  JourneyModel.markUploadAsRejected(s3UploadError)
                sessionStateService
                  .updateSessionState(sessionStateUpdate)
                  .map(renderer.acknowledgeFileUploadRedirect)
              }
            )
        }
      }
    }

  // GET /journey/:journeyId/file-rejected
  final def asyncMarkFileUploadAsRejected(journeyId: String): Action[AnyContent] =
    Action.async { implicit request =>
      whenInSession {
        Forms.UpscanUploadErrorForm.bindFromRequest
          .fold(
            formWithErrors => sessionStateService.currentSessionState.map(router.redirectWithForm(formWithErrors)),
            s3UploadError => {
              val sessionStateUpdate =
                JourneyModel.markUploadAsRejected(s3UploadError)
              sessionStateService
                .updateSessionState(sessionStateUpdate)
                .map(renderer.acknowledgeFileUploadRedirect)
            }
          )
      }
    }

  // OPTIONS /journey/:journeyId/file-rejected
  final def preflightUpload(journeyId: String): Action[AnyContent] =
    Action {
      Created.withHeaders(HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
    }

}
