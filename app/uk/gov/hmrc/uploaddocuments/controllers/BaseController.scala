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

import play.api.i18n.I18nSupport
import play.api.mvc._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.uploaddocuments.connectors.FrontendAuthConnector
import uk.gov.hmrc.uploaddocuments.models.JourneyId
import uk.gov.hmrc.uploaddocuments.support.JsEnabled.COOKIE_JSENABLED
import uk.gov.hmrc.uploaddocuments.support.SHA256
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class BaseControllerComponents @Inject() (
  val appConfig: AppConfig,
  val authConnector: FrontendAuthConnector,
  val environment: Environment,
  val configuration: Configuration,
  val messagesControllerComponents: MessagesControllerComponents
)

abstract class BaseController(
  val components: BaseControllerComponents
) extends FrontendBaseController with I18nSupport with AuthActions {

  final def config: Configuration        = components.configuration
  final def env: Environment             = components.environment
  final def authConnector: AuthConnector = components.authConnector

  final protected def controllerComponents: MessagesControllerComponents = components.messagesControllerComponents

  final def journeyIdFromSession(implicit hc: HeaderCarrier): Option[JourneyId] =
    hc.sessionId.map(_.value).map(SHA256.compute).map(JourneyId.apply)

  final def whenInSession(body: JourneyId => Future[Result])(implicit hc: HeaderCarrier): Future[Result] =
    journeyIdFromSession.fold(Future.successful(Redirect(components.appConfig.govukStartUrl)))(body)

  final def preferUploadMultipleFiles(implicit rh: RequestHeader): Boolean = {
    val isEnabled = rh.cookies.get(COOKIE_JSENABLED).exists(_.value == "true")
    if (!isEnabled) logger.debug("javascript is disabled")
    isEnabled
  }

  final def govukStartUrl: String = components.appConfig.govukStartUrl

}
