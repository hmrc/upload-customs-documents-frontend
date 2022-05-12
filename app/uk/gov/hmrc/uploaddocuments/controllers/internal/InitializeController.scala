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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Action
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.uploaddocuments.controllers.actions.AuthAction
import uk.gov.hmrc.uploaddocuments.controllers.{BaseController, BaseControllerComponents, routes => mainRoutes}
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.services.{FileUploadService, JourneyContextService}

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class InitializeController @Inject()(components: BaseControllerComponents,
                                     fileUploadService: FileUploadService,
                                     journeyContextService: JourneyContextService,
                                     @Named("backChannelAuthentication") backChannelAuth: AuthAction)
                                    (implicit ec: ExecutionContext) extends BaseController(components) {

  // POST /internal/initialize
  final val initialize: Action[JsValue] = backChannelAuth.async(parse.tolerantJson) { implicit request =>
    withJsonBody[FileUploadInitializationRequest] { payload =>
      Logger.debug(s"[initialize] Call to initiate journey for journeyId: '${request.journeyId}', body: \n${Json.prettyPrint(Json.toJson(payload)(FileUploadInitializationRequest.writeNoDownloadUrl))}")
      for {
        _ <- journeyContextService.putJourneyContext(FileUploadContext(payload.config, HostService(request)))(request.journeyId)
        _ <- fileUploadService.putFiles(FileUploads(payload))(request.journeyId)
      } yield
        Created.withHeaders(HeaderNames.LOCATION -> mainRoutes.StartController.start.url)
    }
  }
}
