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
import uk.gov.hmrc.uploaddocuments.services.{FileUploadService, JourneyContextService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class RemoveController @Inject()(components: BaseControllerComponents,
                                 override val journeyContextService: JourneyContextService,
                                 fileUploadService: FileUploadService)
                                (implicit ec: ExecutionContext) extends BaseController(components) with JourneyContextControllerHelper {

  // GET /uploaded/:reference/remove
  final def removeFileUploadByReference(reference: String): Action[AnyContent] = Action.async { implicit request =>
    whenInSession { implicit journeyId =>
      whenAuthenticated {
        withJourneyContext { implicit journeyContext =>
          fileUploadService.removeFile(reference).map {
            case Some((Left(_), _)) | None => InternalServerError
            case Some((Right(_), updatedFilesWithFileRemoved)) =>
              if (updatedFilesWithFileRemoved.isEmpty) {
                Redirect(routes.ChooseSingleFileController.showChooseFile)
              } else {
                Redirect(routes.SummaryController.showSummary)
              }
          }
        }
      }
    }
  }

  // POST /uploaded/:reference/remove
  final def removeFileUploadByReferenceAsync(reference: String): Action[AnyContent] = Action.async { implicit request =>
    whenInSession { implicit journeyId =>
      whenAuthenticated {
        withJourneyContext { implicit journeyContext =>
          fileUploadService.removeFile(reference).map { _ => NoContent }
        }
      }
    }
  }
}
