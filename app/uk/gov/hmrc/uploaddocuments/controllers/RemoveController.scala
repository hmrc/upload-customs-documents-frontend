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
import uk.gov.hmrc.uploaddocuments.controllers.actions.{AuthAction, JourneyContextAction}
import uk.gov.hmrc.uploaddocuments.services.{FileUploadService, JourneyContextService}
import uk.gov.hmrc.uploaddocuments.utils.LoggerUtil

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class RemoveController @Inject()(components: BaseControllerComponents,
                                 fileUploadService: FileUploadService,
                                 @Named("authenticated") auth: AuthAction,
                                 journeyContext: JourneyContextAction)
                                (implicit ec: ExecutionContext) extends BaseController(components) {

  // GET /uploaded/:reference/remove
  final def removeFileUploadByReference(reference: String): Action[AnyContent] = (auth andThen journeyContext).async { implicit request =>
    fileUploadService.removeFile(reference)(hc, request.journeyId, request.journeyContext).map {
      case Some((Left(_), _)) | None =>
        Logger.error(s"[removeFileUploadByReference] Failed to remove file with reference: '$reference'")
        InternalServerError
      case Some((Right(_), updatedFilesWithFileRemoved)) =>
        if (updatedFilesWithFileRemoved.isEmpty) {
          Redirect(routes.ChooseSingleFileController.showChooseFile(None))
        } else {
          Redirect(routes.SummaryController.showSummary)
        }
    }
  }

  // POST /uploaded/:reference/remove
  final def removeFileUploadByReferenceAsync(reference: String): Action[AnyContent] = (auth andThen journeyContext).async { implicit request =>
    fileUploadService.removeFile(reference)(hc, request.journeyId, request.journeyContext).map { _ => NoContent }
  }
}
