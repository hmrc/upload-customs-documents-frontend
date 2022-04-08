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
import uk.gov.hmrc.uploaddocuments.connectors.UpscanInitiateConnector
import uk.gov.hmrc.uploaddocuments.journeys.JourneyModel
import uk.gov.hmrc.uploaddocuments.journeys.State
import uk.gov.hmrc.uploaddocuments.services.SessionStateService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class SummaryController @Inject() (
  sessionStateService: SessionStateService,
  upscanInitiateConnector: UpscanInitiateConnector,
  val router: Router,
  renderer: Renderer,
  components: BaseControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController(components) with UpscanRequestSupport {

  // GET /summary
  final val showSummary: Action[AnyContent] =
    Action.async { implicit request =>
      whenInSession {
        whenAuthenticated {
          val sessionStateUpdate = JourneyModel.backToSummary
          sessionStateService
            .getCurrentOrUpdateSessionState[State.Summary](sessionStateUpdate)
            .map {
              case (summary: State.Summary, breadcrumbs) =>
                renderer.display(summary, breadcrumbs, None)

              case other =>
                router.redirectTo(other)
            }
        }
      }
    }

  // POST /summary
  final val submitUploadAnotherFileChoice: Action[AnyContent] =
    Action.async { implicit request =>
      whenInSession {
        whenAuthenticated {
          Forms.YesNoChoiceForm.bindFromRequest
            .fold(
              formWithErrors => sessionStateService.currentSessionState.map(router.redirectWithForm(formWithErrors)),
              choice => {
                val sessionStateUpdate =
                  JourneyModel.submitedUploadAnotherFileChoice(upscanRequest(currentJourneyId))(
                    upscanInitiateConnector.initiate(_, _)
                  )(JourneyModel.continueToHost)(choice)
                sessionStateService
                  .updateSessionState(sessionStateUpdate)
                  .map(router.redirectTo)
              }
            )
        }
      }
    }
}
