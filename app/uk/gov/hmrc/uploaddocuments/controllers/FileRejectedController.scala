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
import uk.gov.hmrc.uploaddocuments.forms.Forms
import uk.gov.hmrc.uploaddocuments.models.{FileUpload, FileUploads, Timestamp}
import uk.gov.hmrc.uploaddocuments.repository.JourneyCacheRepository.DataKeys
import uk.gov.hmrc.uploaddocuments.services.FileUploadService
import uk.gov.hmrc.uploaddocuments.support.UploadLog

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileRejectedController @Inject()(components: BaseControllerComponents,
                                       fileUploadService: FileUploadService)(implicit ec: ExecutionContext)
  extends BaseController(components) with UploadLog {

  // GET /file-rejected
  final val markFileUploadAsRejected: Action[AnyContent] =
    Action.async { implicit request =>
      whenInSession { implicit journeyId =>
        whenAuthenticated {
          withJourneyContext { implicit journeyContext =>
            Forms.UpscanUploadErrorForm.bindFromRequest
              .fold(
                formWithErrors =>
                  Future(Redirect(routes.ChooseSingleFileController.showChooseFile).withFormError(formWithErrors)),
                s3UploadError => {
                  fileUploadService.markFileAsRejected(s3UploadError).map { _ =>
                    Redirect(routes.ChooseSingleFileController.showChooseFile)
                  }
                }
              )
          }
        }
      }
    }

  // POST /file-rejected
  final val markFileUploadAsRejectedAsync: Action[AnyContent] =
    Action.async { implicit request =>
      whenInSession { implicit journeyId =>
        rejectedAsyncLogicWithStatus(Created)
      }
    }

  // GET /journey/:journeyId/file-rejected
  final def asyncMarkFileUploadAsRejected(implicit journeyId: JourneyId): Action[AnyContent] =
    Action.async { implicit request =>
      rejectedAsyncLogicWithStatus(NoContent)
    }

  private def rejectedAsyncLogicWithStatus(status: => Result)(implicit request: Request[AnyContent], journeyId: JourneyId): Future[Result] =
    whenAuthenticated {
      withJourneyContext { implicit journeyContext =>
        Forms.UpscanUploadErrorForm.bindFromRequest
          .fold(
            formWithErrors =>
              Future.successful(
                Redirect(routes.ChooseMultipleFilesController.showChooseMultipleFiles).withFormError(formWithErrors)
              ),
            s3UploadError =>
              fileUploadService.markFileAsRejected(s3UploadError).map(_ =>
                status.withHeaders(HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
              )
          )
      }
    }

  // OPTIONS /journey/:journeyId/file-rejected
  final def preflightUpload(journeyId: String): Action[AnyContent] =
    Action {
      Created.withHeaders(HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
    }

}
