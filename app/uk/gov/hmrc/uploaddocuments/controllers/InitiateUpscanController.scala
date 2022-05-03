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
import uk.gov.hmrc.uploaddocuments.models.UploadRequest
import uk.gov.hmrc.uploaddocuments.services.{InitiateUpscanService, JourneyContextService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class InitiateUpscanController @Inject()(upscanInitiateService: InitiateUpscanService,
                                         components: BaseControllerComponents,
                                         override val journeyContextService: JourneyContextService)
                                        (implicit ec: ExecutionContext) extends BaseController(components) with JourneyContextControllerHelper {

  // POST /initiate-upscan/:uploadId
  final def initiateNextFileUpload(uploadId: String): Action[AnyContent] = Action.async { implicit request =>
    whenInSession { implicit journeyId =>
      whenAuthenticated {
        withJourneyContext { implicit journeyContext =>
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
    }
  }
}
