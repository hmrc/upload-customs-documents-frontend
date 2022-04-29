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
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.uploaddocuments.forms.Forms
import uk.gov.hmrc.uploaddocuments.models.JourneyId
import uk.gov.hmrc.uploaddocuments.services.FileUploadService
import uk.gov.hmrc.uploaddocuments.utils.LoggerUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FilePostedController @Inject()(components: BaseControllerComponents,
                                     fileUploadService: FileUploadService)
                                    (implicit ec: ExecutionContext) extends BaseController(components) with LoggerUtil {

  // GET /journey/:journeyId/file-posted
  final def asyncMarkFileUploadAsPosted(implicit journeyId: JourneyId): Action[AnyContent] =
    Action.async { implicit request =>
      Forms.UpscanUploadSuccessForm
        .bindFromRequest
        .fold(
          formWithErrors =>
            Future.successful(Redirect(routes.ChooseMultipleFilesController.showChooseMultipleFiles).withFormError(formWithErrors))
          ,
          s3UploadSuccess =>
            fileUploadService.markFileAsPosted(s3UploadSuccess.key).map { _ =>
              Created.withHeaders(HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
            }
        )
    }

  // OPTIONS /journey/:journeyId/file-posted
  final def preflightUpload(journeyId: JourneyId): Action[AnyContent] =
    Action {
      Created.withHeaders(HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
    }

}
