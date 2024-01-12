/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Action
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.uploaddocuments.controllers.{BaseController, BaseControllerComponents, routes => mainRoutes}
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.services.{FileUploadService, JourneyContextService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.play.http.HeaderCarrierConverter

@Singleton
class InitializeController @Inject() (
  components: BaseControllerComponents,
  fileUploadService: FileUploadService,
  journeyContextService: JourneyContextService
)(implicit ec: ExecutionContext)
    extends BaseController(components) {

  // POST /internal/initialize
  final val initialize: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    implicit val hc = HeaderCarrierConverter.fromRequest(request) // required to process Session-ID from the cookie
    withJsonBody[FileUploadInitializationRequest] { payload =>
      whenInSession { implicit journeyId =>
        Logger.debug(s"[initialize] Call to initiate journey for journeyId: '$journeyId', body: \n${Json
          .prettyPrint(Json.toJson(payload)(FileUploadInitializationRequest.writeNoDownloadUrl))}")
        whenAuthenticatedInBackchannel {
          for {
            maybeExistingContext <- journeyContextService.getJourneyContext()
            _ <- journeyContextService.putJourneyContext(
                   FileUploadContext(
                     payload.config,
                     HostService(request),
                     userWantsToUploadNextFile = maybeExistingContext
                       .map(_.userWantsToUploadNextFile)
                       .getOrElse(false)
                   )
                 )
            _ <- fileUploadService.putFiles(FileUploads(payload))
          } yield Created.withHeaders(HeaderNames.LOCATION -> mainRoutes.StartController.start.url)
        }
      }
    }
  }
}
