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
import uk.gov.hmrc.uploaddocuments.support.SHA256
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BaseControllerComponents @Inject() (
  val appConfig: AppConfig,
  val authConnector: FrontendAuthConnector,
  val environment: Environment,
  val configuration: Configuration,
  val messagesControllerComponents: MessagesControllerComponents,
  val newJourneyCacheRepository: JourneyCacheRepository
)

abstract class BaseController(
  val components: BaseControllerComponents
) extends MessagesBaseController with Utf8MimeTypes with WithJsonBody with I18nSupport with AuthActions {

  final val COOKIE_JSENABLED = "jsenabled"

  final def config: Configuration = components.configuration
  final def env: Environment = components.environment
  final def authConnector: AuthConnector = components.authConnector
  final protected def controllerComponents: MessagesControllerComponents = components.messagesControllerComponents

  implicit class FutureOps[A](value: A) {
    def asFuture: Future[A] = Future.successful(value)
  }

  private val journeyIdPathParamRegex = ".*?/journey/([a-fA-F0-9]+?)/.*".r

  final def currentJourneyId(implicit rh: RequestHeader): String = journeyId.get

  final def journeyId(implicit rh: RequestHeader): Option[String] =
    journeyId(decodeHeaderCarrier(rh), rh)

  private def journeyId(hc: HeaderCarrier, rh: RequestHeader): Option[String] =
    (rh.path match {
      case journeyIdPathParamRegex(id) => Some(id)
      case _                           => None
    })
      .orElse(hc.sessionId.map(_.value).map(SHA256.compute))

  final implicit def context(implicit rh: RequestHeader): HeaderCarrier = {
    val hc = decodeHeaderCarrier(rh)
    journeyId(hc, rh)
      .map(jid => hc.withExtraHeaders("FileUploadJourney" -> jid))
      .getOrElse(hc)
  }

  private def decodeHeaderCarrier(rh: RequestHeader): HeaderCarrier =
    HeaderCarrierConverter.fromRequestAndSession(rh, rh.session)

  final def whenInSession(
    body: => Future[Result]
  )(implicit request: Request[_]): Future[Result] =
    journeyId match {
      case None => Future.successful(Redirect(components.appConfig.govukStartUrl))
      case _    => body
    }

  final def withJourneyContext(
    body: FileUploadContext => Future[Result]
  )(implicit request: Request[_], ec: ExecutionContext): Future[Result] =
    components.newJourneyCacheRepository.get(currentJourneyId)(DataKeys.journeyContext) flatMap {
      case Some(journey) => body(journey)
      case _             => Future.successful(Redirect(components.appConfig.govukStartUrl))
    }

  final def withUploadedFiles(
    body: FileUploads => Future[Result]
  )(implicit request: Request[_], ec: ExecutionContext): Future[Result] =
    components.newJourneyCacheRepository.get(currentJourneyId)(DataKeys.uploadedFiles) flatMap {
      case Some(files) => body(files)
      case _           => Future.successful(Redirect(components.appConfig.govukStartUrl))
    }

  implicit class ResultExtensions(r: Result) {

    //TODO: Not really sure what the flashing does - this was lifted from the Router class...
    def withFormError(formWithErrors: Form[_]) = r.flashing(Flash {
      val data = formWithErrors.data
      // dummy parameter required if empty data
      if (data.isEmpty) Map("dummy" -> "") else data
    })
  }

  final def preferUploadMultipleFiles(implicit rh: RequestHeader): Boolean =
    rh.cookies.get(COOKIE_JSENABLED).isDefined

}
