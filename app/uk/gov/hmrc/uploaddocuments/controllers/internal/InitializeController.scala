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
import uk.gov.hmrc.uploaddocuments.controllers.{BaseController, BaseControllerComponents, routes => mainRoutes}
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.repository.JourneyCacheRepository.DataKeys

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class InitializeController @Inject()(components: BaseControllerComponents)(implicit ec: ExecutionContext)
    extends BaseController(components) {

  // POST /internal/initialize
  final val initialize: Action[JsValue] =
    Action.async(parse.tolerantJson) { implicit request =>
      withJsonBody[FileUploadInitializationRequest] { payload =>
        whenInSession { implicit journeyId =>
          whenAuthenticatedInBackchannel {
            val host           = HostService(request)
            val journeyContext = FileUploadContext(payload.config, host)
            for {
              _ <- components.newJourneyCacheRepository.put(journeyId.value)(DataKeys.journeyContext, journeyContext)
              _ <- components.newJourneyCacheRepository.put(journeyId.value)(DataKeys.uploadedFiles, FileUploads(payload))
            } yield {
              val url = if (!journeyContext.config.features.showUploadMultiple) {
                mainRoutes.ChooseSingleFileController.showChooseFile.url
              } else { mainRoutes.StartController.start.url }
              Created.withHeaders(HeaderNames.LOCATION -> url)
            }
          }
        }
      }
    }

}
