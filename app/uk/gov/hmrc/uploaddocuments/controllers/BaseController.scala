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

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.{Utf8MimeTypes, WithJsonBody}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.uploaddocuments.connectors.FrontendAuthConnector
import uk.gov.hmrc.uploaddocuments.models.{FileUploadContext, FileUploads}
import uk.gov.hmrc.uploaddocuments.repository.JourneyCacheRepository
import uk.gov.hmrc.uploaddocuments.repository.JourneyCacheRepository.DataKeys
import uk.gov.hmrc.uploaddocuments.services.{FileUploadService, JourneyContextService}
import uk.gov.hmrc.uploaddocuments.support.SHA256
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig
import uk.gov.hmrc.uploaddocuments.support.JsEnabled.COOKIE_JSENABLED

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BaseControllerComponents @Inject()(
  val appConfig: AppConfig,
  val authConnector: FrontendAuthConnector,
  val environment: Environment,
  val configuration: Configuration,
  val messagesControllerComponents: MessagesControllerComponents,
  val newJourneyCacheRepository: JourneyCacheRepository,
  val journeyContextService: JourneyContextService,
  val fileUploadService: FileUploadService
)

abstract class BaseController(
  val components: BaseControllerComponents
) extends MessagesBaseController with Utf8MimeTypes with WithJsonBody with I18nSupport with AuthActions {
  type JourneyId = String

  final def config: Configuration        = components.configuration
  final def env: Environment             = components.environment
  final def authConnector: AuthConnector = components.authConnector

  final protected def controllerComponents: MessagesControllerComponents = components.messagesControllerComponents

  final def journeyIdFromSession(implicit rh: RequestHeader): Option[JourneyId] =
    decodeHeaderCarrier(rh).sessionId.map(_.value).map(SHA256.compute)

  final implicit def context(implicit rh: RequestHeader): HeaderCarrier = {
    val hc = decodeHeaderCarrier(rh)
    journeyIdFromSession.fold(hc)(jid => hc.withExtraHeaders("FileUploadJourney" -> jid))
  }

  private def decodeHeaderCarrier(rh: RequestHeader): HeaderCarrier =
    HeaderCarrierConverter.fromRequestAndSession(rh, rh.session)

  final def whenInSession(
    body: JourneyId => Future[Result]
  )(implicit request: Request[_]): Future[Result] =
    journeyIdFromSession.fold(Future.successful(Redirect(components.appConfig.govukStartUrl)))(body)

  final def withJourneyContext(
    body: FileUploadContext => Future[Result]
  )(implicit ec: ExecutionContext, journeyId: JourneyId): Future[Result] =
    components.newJourneyCacheRepository.get(journeyId)(DataKeys.journeyContext) flatMap {
      _.fold(Future.successful(Redirect(components.appConfig.govukStartUrl)))(body)
    }

  final def withUploadedFiles(
    body: FileUploads => Future[Result]
  )(implicit ec: ExecutionContext, journeyId: JourneyId): Future[Result] =
    components.newJourneyCacheRepository.get(journeyId)(DataKeys.uploadedFiles) flatMap {
      _.fold(Future.successful(Redirect(components.appConfig.govukStartUrl)))(body)
    }

  implicit class ResultExtensions(r: Result) {

    //TODO: Not really sure what the flashing does - this was lifted from the Router class...
    def withFormError(formWithErrors: Form[_]) =
      r.flashing(Flash {
        val data = formWithErrors.data
        // dummy parameter required if empty data
        if (data.isEmpty) Map("dummy" -> "") else data
      })
  }

  final def preferUploadMultipleFiles(implicit rh: RequestHeader): Boolean =
    rh.cookies.get(COOKIE_JSENABLED).isDefined

}
