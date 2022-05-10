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
import play.api.mvc._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.AuthProvider.{GovernmentGateway, PrivilegedApplication}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.credentials
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.uploaddocuments.models.requests.{AuthRequest, SessionRequest}
import uk.gov.hmrc.uploaddocuments.support.CallOps
import uk.gov.hmrc.uploaddocuments.utils.LoggerUtil

import scala.concurrent.{ExecutionContext, Future}

trait AuthAction extends ActionFunction[SessionRequest, AuthRequest]


class AuthenticatedAction @Inject()(override val authConnector: AuthConnector,
                                    override val config: Configuration,
                                    override val env: Environment)
                                   (implicit val executionContext: ExecutionContext)
  extends AuthAction with AuthorisedFunctions with AuthRedirects with LoggerUtil {

  override def invokeBlock[A](request: SessionRequest[A], block: AuthRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(
      session = request.session,
      request = request
    )
    authorised(AuthProviders(GovernmentGateway, PrivilegedApplication))
      .retrieve(credentials)(cred => block(AuthRequest(request, request.journeyId, cred)))
      .recover {
        case e: AuthorisationException =>
          Logger.warn(s"Access forbidden because of ${e.getMessage}")
          val continueUrl = CallOps.localFriendlyUrl(env, config)(request.uri, request.host)
          toGGLogin(continueUrl)
      }
  }
}

class BackChannelAuthenticatedAction @Inject()(override val authConnector: AuthConnector,
                                               override val config: Configuration,
                                               override val env: Environment,
                                               val parser: BodyParsers.Default)
                                              (implicit val executionContext: ExecutionContext)
  extends AuthAction with AuthorisedFunctions with AuthRedirects with LoggerUtil {

  override def invokeBlock[A](request: SessionRequest[A], block: AuthRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc = HeaderCarrierConverter.fromRequest(request) // required to process Session-ID from the cookie
    authorised(AuthProviders(GovernmentGateway, PrivilegedApplication))
      .retrieve(credentials)(cred => block(AuthRequest(request, request.journeyId, cred)))
      .recover {
        case e: AuthorisationException =>
          Logger.warn(s"Access forbidden because of ${e.getMessage}")
          Results.Forbidden
      }
  }
}
