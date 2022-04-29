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

import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.uploaddocuments.models.{FileUploadContext, FileUploads, JourneyId}
import uk.gov.hmrc.uploaddocuments.services.{FileUploadService, JourneyContextService}
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

import scala.concurrent.{ExecutionContext, Future}

trait JourneyContextControllerHelper { baseController: BaseController =>

  val journeyContextService: JourneyContextService

  def withJourneyContext(body: FileUploadContext => Future[Result])
                        (implicit ec: ExecutionContext, journeyId: JourneyId): Future[Result] =
    journeyContextService.withJourneyContext(Future.successful(Redirect(baseController.components.appConfig.govukStartUrl)))(body)

}