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
                                    (implicit ec: ExecutionContext) extends BaseController(components) {

  // GET /journey/:journeyId/file-posted
  final def asyncMarkFileUploadAsPosted(implicit journeyId: JourneyId): Action[AnyContent] = Action.async { implicit request =>
    Forms.UpscanUploadSuccessForm
      .bindFromRequest
      .fold(
        _ => {
          Logger.error("[asyncMarkFileUploadAsPosted] Query Parameters from Upscan could not be bound to form")
          Logger.debug(s"[asyncMarkFileUploadAsPosted] Query Params Received: ${request.queryString}")
          Future.successful(BadRequest)
        },
        s3UploadSuccess =>
          fileUploadService.markFileAsPosted(s3UploadSuccess.key).map(_ => Created)
      )
  }
}
