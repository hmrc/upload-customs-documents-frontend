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
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

import javax.inject.Inject
import uk.gov.hmrc.uploaddocuments.models.UrlValidator
import uk.gov.hmrc.uploaddocuments.utils.LoggerUtil

class SignOutController @Inject() (controllerComponents: MessagesControllerComponents, appConfig: AppConfig)
    extends FrontendController(controllerComponents) with LoggerUtil {

  final def signOut(continueUrl: Option[RedirectUrl]): Action[AnyContent] = Action { _ =>
    continueUrl.fold(Redirect(appConfig.signOutUrl)){ url =>
      url.getEither(OnlyRelative) match {
        case Right(safeUrl: SafeRedirectUrl) =>
          Redirect(appConfig.signOutUrl, Map("continue" -> Seq(safeUrl.url)))
        case Left(e) =>
          if (UrlValidator.isValidFrontendUrl(url.unsafeValue)) {
            Logger.info(s"[SignOutController.signOut] Using unsafeUrl, $e")
            Redirect(appConfig.signOutUrl, Map("continue" -> Seq(url.unsafeValue)))
          } else {
            Redirect(appConfig.signOutUrl)
          }
      }
    } 
  }

  final def signOutTimeout(continueUrl: Option[RedirectUrl]): Action[AnyContent] = signOut(continueUrl)
}
