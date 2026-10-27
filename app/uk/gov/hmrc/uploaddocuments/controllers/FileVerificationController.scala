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

package uk.gov.hmrc.uploaddocuments.controllers

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.uploaddocuments.services.{FileVerificationService, JourneyContextService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class FileVerificationController @Inject() (
  components: BaseControllerComponents,
  fileVerificationService: FileVerificationService,
  override val journeyContextService: JourneyContextService
)(using ExecutionContext)
    extends BaseController(components) with JourneyContextControllerHelper {

  // GET /file-verification/:reference/status
  final def checkFileVerificationStatus(reference: String): Action[AnyContent] = Action.async { implicit request =>
    whenInSession { implicit journeyId =>
      whenAuthenticated {
        withJourneyContext { implicit journeyContext =>
          fileVerificationService.getFileVerificationStatus(reference).map {
            case Some(verificationStatus) =>
              Logger.info(
                s"[checkFileVerificationStatus] UpscanRef: '$reference', Status: ${verificationStatus.fileStatus}"
              )
              Ok(Json.toJson(verificationStatus))
            case None =>
              Logger.error(s"[checkFileVerificationStatus] No File exists for UpscanRef: '$reference'")
              NotFound
          }
        }
      }
    }
  }
}
