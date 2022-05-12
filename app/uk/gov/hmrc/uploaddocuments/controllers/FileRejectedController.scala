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

import play.api.mvc.{Action, AnyContent, Request, Result}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.uploaddocuments.controllers.actions.{AuthAction, JourneyContextAction}
import uk.gov.hmrc.uploaddocuments.forms.Forms
import uk.gov.hmrc.uploaddocuments.models.JourneyId
import uk.gov.hmrc.uploaddocuments.models.requests.{AuthRequest, JourneyContextRequest}
import uk.gov.hmrc.uploaddocuments.services.{FileUploadService, JourneyContextService}
import uk.gov.hmrc.uploaddocuments.support.UploadLog

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileRejectedController @Inject()(components: BaseControllerComponents,
                                       override val fileUploadService: FileUploadService,
                                       @Named("authenticated") auth: AuthAction,
                                       journeyContext: JourneyContextAction)
                                      (implicit ec: ExecutionContext) extends BaseController(components) with FileUploadsControllerHelper with UploadLog {

  // GET /file-rejected
  final val markFileUploadAsRejected: Action[AnyContent] = (auth andThen journeyContext).async { implicit request =>
    Forms.UpscanUploadErrorForm.bindFromRequest
      .fold(
        _ => {
          Logger.error("[markFileUploadAsRejected] Query Parameters from Upscan could not be bound to form")
          Logger.debug(s"[markFileUploadAsRejected] Query Params Received: ${request.queryString}")
          Future.successful(InternalServerError)
        },
        s3UploadError => {
          fileUploadService.markFileAsRejected(s3UploadError)(request.journeyId, request.journeyContext).map { _ =>
            Redirect(routes.ChooseSingleFileController.showChooseFile(None))
          }
        }
      )
  }

  // POST /file-rejected
  final val markFileUploadAsRejectedAsync: Action[AnyContent] = (auth andThen journeyContext).async { implicit request =>
    rejectedAsyncLogicWithStatus(Created)
  }

  // GET /journey/:journeyId/file-rejected
  final def asyncMarkFileUploadAsRejected(implicit journeyId: JourneyId): Action[AnyContent] = Action.async { implicit request =>
    journeyContext.invokeBlock(AuthRequest(request, journeyId, None), { implicit journeyContextRequest: JourneyContextRequest[AnyContent] =>
      rejectedAsyncLogicWithStatus(NoContent)
    })
  }

  private def rejectedAsyncLogicWithStatus(status: => Result)(implicit request: JourneyContextRequest[AnyContent]): Future[Result] =
    Forms.UpscanUploadErrorForm.bindFromRequest
      .fold(
        _ => {
          Logger.error("[rejectedAsyncLogicWithStatus] Query Parameters from Upscan could not be bound to form")
          Logger.debug(s"[rejectedAsyncLogicWithStatus] Query Params Received: ${request.queryString}")
          Future.successful(BadRequest)
        },
        s3UploadError =>
          fileUploadService.markFileAsRejected(s3UploadError)(request.journeyId, request.journeyContext).map(_ => status)
      )
}
