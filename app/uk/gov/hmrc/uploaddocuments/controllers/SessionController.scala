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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.binders.{OnlyRelative, RedirectUrl, SafeRedirectUrl}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.uploaddocuments.views.html.TimedOutView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import uk.gov.hmrc.uploaddocuments.models.UrlValidator
import uk.gov.hmrc.uploaddocuments.utils.LoggerUtil

@Singleton
class SessionController @Inject() (controllerComponents: MessagesControllerComponents, timedOutView: TimedOutView)
    extends FrontendController(controllerComponents) with LoggerUtil {

  final val showTimeoutPage: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(timedOutView()))
  }

  final def keepAlive(continueUrl: Option[RedirectUrl]): Action[AnyContent] = Action.async { _ =>
    Future.successful(
      continueUrl.flatMap( url =>
        url.getEither(OnlyRelative) match {
          case Right(safeUrl: SafeRedirectUrl) =>
            Some(Redirect(safeUrl.url))
          case Left(e) =>
            if (UrlValidator.isValidFrontendUrl(url.unsafeValue)) {
              Logger.info(s"[SessionController.keepAlive] Using unsafeUrl, $e")
              Some(Redirect(url.unsafeValue))
            } else {
              None
            }
        }
      ).getOrElse(Ok("{}"))
    )
  }
}
