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

import play.api.mvc.{Result, Results}
import uk.gov.hmrc.uploaddocuments.models.{FileUploadContext, JourneyId}
import uk.gov.hmrc.uploaddocuments.services.JourneyContextService

import scala.concurrent.Future

trait JourneyContextControllerHelper {

  val journeyContextService: JourneyContextService
  def govukStartUrl: String

  def withJourneyContext(body: FileUploadContext => Future[Result])(implicit journeyId: JourneyId): Future[Result] =
    journeyContextService.withJourneyContext(
      Future.successful(Results.Redirect(govukStartUrl))
    )(c =>
      Future.successful(
        Results.Redirect(
          c.config.sendoffUrl
            .orElse(c.content.serviceUrl)
            .getOrElse(govukStartUrl)
        )
      )
    )(body)

  def withJourneyContextWithErrorHandler(
    journeyNotFoundResult: => Future[Result]
  )(body: FileUploadContext => Future[Result])()(implicit
    journeyId: JourneyId
  ): Future[Result] =
    journeyContextService
      .withJourneyContext(journeyNotFoundResult)(_ => journeyNotFoundResult)(body)

}
