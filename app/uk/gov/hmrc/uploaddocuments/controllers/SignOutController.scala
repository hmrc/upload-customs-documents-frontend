/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

import javax.inject.Inject
import uk.gov.hmrc.uploaddocuments.models.UrlValidator

class SignOutController @Inject() (controllerComponents: MessagesControllerComponents, appConfig: AppConfig)
    extends FrontendController(controllerComponents) {

  final def signOut(continueUrl: Option[String]): Action[AnyContent] = Action { _ =>
    continueUrl.fold(Redirect(appConfig.signOutUrl))(url =>
      if (UrlValidator.isReleativeUrl(url) || UrlValidator.isValidFrontendUrl(url))
        Redirect(appConfig.signOutUrl, Map("continue" -> Seq(url)))
      else
        Redirect(appConfig.signOutUrl)
    )
  }

  final def signOutTimeout(continueUrl: Option[String]): Action[AnyContent] = signOut(continueUrl)
}
