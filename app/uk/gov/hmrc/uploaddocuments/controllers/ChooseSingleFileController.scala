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
class ChooseSingleFileController @Inject() (
  sessionStateService: SessionStateService,
  upscanInitiateConnector: UpscanInitiateConnector,
  val router: Router,
  renderer: Renderer,
  components: BaseControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController(components) with UpscanRequestSupport {

  // GET /choose-file
  final val showChooseFile: Action[AnyContent] =
    Action.async { implicit request =>
      whenInSession {
        whenAuthenticated {
          val sessionStateUpdate =
            JourneyModel
              .initiateFileUpload(upscanRequest(currentJourneyId))(upscanInitiateConnector.initiate(_, _))
          sessionStateService
            .updateSessionState(sessionStateUpdate)
            .map {
              case (uploadSingleFile: State.UploadSingleFile, breadcrumbs) =>
                renderer.display(uploadSingleFile, breadcrumbs, None)

              case other =>
                router.redirectTo(other)
            }
        }
      }
    }
}
