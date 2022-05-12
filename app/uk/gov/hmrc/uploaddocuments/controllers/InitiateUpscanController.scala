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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.uploaddocuments.controllers.actions.{AuthAction, JourneyContextAction}
import uk.gov.hmrc.uploaddocuments.models.UploadRequest
import uk.gov.hmrc.uploaddocuments.services.InitiateUpscanService

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class InitiateUpscanController @Inject()(upscanInitiateService: InitiateUpscanService,
                                         components: BaseControllerComponents,
                                         @Named("authenticated") auth: AuthAction,
                                         journeyContext: JourneyContextAction)
                                        (implicit ec: ExecutionContext) extends BaseController(components) {

  // POST /initiate-upscan/:uploadId
  final def initiateNextFileUpload(uploadId: String): Action[AnyContent] = (auth andThen journeyContext).async { implicit request =>
    upscanInitiateService.initiateNextMultiFileUpload(uploadId).map {
      case Some(upscanResponse) =>
        Ok(Json.obj(fields =
          "upscanReference" -> upscanResponse.reference,
          "uploadId"        -> uploadId,
          "uploadRequest"   -> UploadRequest.formats.writes(upscanResponse.uploadRequest)
        ))
      case None => BadRequest
    }
  }
}
