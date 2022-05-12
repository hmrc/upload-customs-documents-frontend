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

package uk.gov.hmrc.uploaddocuments.controllers.actions

import com.google.inject.Inject
import play.api.mvc.Results.Redirect
import play.api.mvc._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.AuthProvider.{GovernmentGateway, PrivilegedApplication}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.credentials
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.uploaddocuments.models.JourneyId
import uk.gov.hmrc.uploaddocuments.models.requests.AuthRequest
import uk.gov.hmrc.uploaddocuments.support.{CallOps, SHA256}
import uk.gov.hmrc.uploaddocuments.utils.LoggerUtil
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

import scala.concurrent.{ExecutionContext, Future}

trait AuthAction extends ActionBuilder[AuthRequest, AnyContent] with ActionFunction[Request, AuthRequest] {

  val appConfig: AppConfig

  final def journeyIdFromSession(implicit hc: HeaderCarrier): Option[JourneyId] =
    hc.sessionId.map(_.value).map(SHA256.compute).map(JourneyId.apply)

  final def withJourneyId(f: JourneyId => Future[Result])(implicit hc: HeaderCarrier): Future[Result] =
    journeyIdFromSession.fold(Future.successful(Redirect(appConfig.govukStartUrl)))(f)
}


class AuthenticatedAction @Inject()(override val authConnector: AuthConnector,
                                    override val appConfig: AppConfig,
                                    override val config: Configuration,
                                    override val env: Environment,
                                    override val parser: BodyParsers.Default)
                                   (implicit val executionContext: ExecutionContext)
  extends AuthAction with AuthorisedFunctions with AuthRedirects with LoggerUtil {

  override def invokeBlock[A](request: Request[A], block: AuthRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(session = request.session, request = request)
    withJourneyId { journeyId =>
      authorised(AuthProviders(GovernmentGateway, PrivilegedApplication))
        .retrieve(credentials)(cred => block(AuthRequest(request, journeyId, cred)))
        .recover {
          case e: AuthorisationException =>
            Logger.warn(s"Access forbidden because of ${e.getMessage}")
            val continueUrl = CallOps.localFriendlyUrl(env, config)(request.uri, request.host)
            toGGLogin(continueUrl)
        }
    }
  }
}

class BackChannelAuthenticatedAction @Inject()(override val authConnector: AuthConnector,
                                               override val appConfig: AppConfig,
                                               override val config: Configuration,
                                               override val env: Environment,
                                               override val parser: BodyParsers.Default)
                                              (implicit val executionContext: ExecutionContext)
  extends AuthAction with AuthorisedFunctions with AuthRedirects with LoggerUtil {

  override def invokeBlock[A](request: Request[A], block: AuthRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc = HeaderCarrierConverter.fromRequest(request) // required to process Session-ID from the cookie
    withJourneyId { journeyId =>
      authorised(AuthProviders(GovernmentGateway, PrivilegedApplication))
        .retrieve(credentials)(cred => block(AuthRequest(request, journeyId, cred)))
        .recover {
          case e: AuthorisationException =>
            Logger.warn(s"Access forbidden because of ${e.getMessage}")
            Results.Forbidden
        }
    }
  }
}
