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

package uk.gov.hmrc.uploaddocuments.controllers.internal

import play.api.libs.json.JsValue
import play.api.mvc.Action
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.uploaddocuments.controllers.{BaseController, BaseControllerComponents, Renderer, Router, routes => mainRoutes}
import uk.gov.hmrc.uploaddocuments.journeys.{JourneyModel, State}
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.repository.NewJourneyCacheRepository
import uk.gov.hmrc.uploaddocuments.repository.NewJourneyCacheRepository.DataKeys
import uk.gov.hmrc.uploaddocuments.services.SessionStateService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class InitializeController @Inject() (
  sessionStateService: SessionStateService,
  renderer: Renderer,
  router: Router,
  components: BaseControllerComponents,
  newJourneyCacheRepository: NewJourneyCacheRepository
)(implicit ec: ExecutionContext)
    extends BaseController(components) {

  // POST /internal/initialize
  final val initialize: Action[JsValue] =
    Action.async(parse.tolerantJson) { implicit request =>
      withJsonBody[FileUploadInitializationRequest] { payload =>
        whenInSession {
          whenAuthenticatedInBackchannel {
            val host = HostService.from(request)
            val journeyConfig = FileUploadContext(payload.config, host)
            val previouslyUploadedFiles = payload.toFileUploads
            for {
              _ <- newJourneyCacheRepository.put(currentJourneyId)(DataKeys.journeyContextDataKey, journeyConfig)
              _ <- newJourneyCacheRepository.put(currentJourneyId)(DataKeys.uploadedFiles, previouslyUploadedFiles)
              // Store parallel Session State for now - until we can remove the session state service
              session <- sessionStateService.updateSessionState(JourneyModel.initialize(host)(payload))
            } yield initializationResponse(session)
          }
        }
      }
    }

  private def initializationResponse =
    renderer.resultOf {
      case State.Initialized(context, _) =>
        Created.withHeaders(
          HeaderNames.LOCATION ->
            (
              if (!context.config.features.showUploadMultiple)
                router.showChooseSingleFile
              else
                mainRoutes.StartController.start
            ).url
        )
      case _ => BadRequest
    }

}
