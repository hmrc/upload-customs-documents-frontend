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

import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.uploaddocuments.views.html.StartView

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.uploaddocuments.support.JsEnabled
import play.api.mvc.DiscardingCookie
import scala.concurrent.Future

@Singleton
class StartController @Inject() (view: StartView, components: BaseControllerComponents)
    extends BaseController(components) {

  // GET /
  final val start: Action[AnyContent] = Action { _ =>
    Ok(view(routes.ChooseMultipleFilesController.showChooseMultipleFiles))
      .discardingCookies(DiscardingCookie(JsEnabled.COOKIE_JSENABLED))
  }

  final def options(url: String): Action[AnyContent] = Action.async { _ =>
    Future.successful(NoContent.withHeaders(CORS.headers*))
  }

}

object CORS {

  val headers = List(
    "Access-Control-Allow-Origin"  -> "*",
    "Access-Control-Allow-Methods" -> "GET, POST, OPTIONS"
  )
}
