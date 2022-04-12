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
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.uploaddocuments.controllers.{BaseController, BaseControllerComponents, Renderer}
import uk.gov.hmrc.uploaddocuments.journeys.JourneyModel
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
              _ <- newJourneyCacheRepository.put(currentJourneyId)(DataKeys.journeyConfigDataKey, journeyConfig)
              _ <- newJourneyCacheRepository.put(currentJourneyId)(DataKeys.uploadedFiles, previouslyUploadedFiles)
              session <- sessionStateService.updateSessionState(
                           JourneyModel.initialize(host)(payload)
                         ) // Parallel Session State for now
            } yield renderer.initializationResponse(session)
          }
        }
      }
    }

}
