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

import play.api.mvc.{Request, Result, Results}
import uk.gov.hmrc.auth.core.AuthProvider.{GovernmentGateway, PrivilegedApplication}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.uploaddocuments.support.CallOps
import uk.gov.hmrc.uploaddocuments.utils.LoggerUtil

import scala.concurrent.{ExecutionContext, Future}

trait AuthActions extends AuthorisedFunctions with AuthRedirects with LoggerUtil {

  protected def whenAuthenticated[A](body: => Future[Result])
                                    (implicit request: Request[A], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] =
    authorised(AuthProviders(GovernmentGateway, PrivilegedApplication))
      .retrieve(credentials)(_ => body)
      .recover {
        case _: AuthorisationException =>
          val continueUrl = CallOps.localFriendlyUrl(env, config)(request.uri, request.host)
          toGGLogin(continueUrl)
      }

  protected def whenAuthenticatedInBackchannel[A](body: => Future[Result])
                                                 (implicit request: Request[A], ec: ExecutionContext): Future[Result] = {
    implicit val hc = HeaderCarrierConverter.fromRequest(request) // required to process Session-ID from the cookie
    authorised(AuthProviders(GovernmentGateway, PrivilegedApplication))
      .retrieve(credentials)(_ => body)
      .recover {
        case e: AuthorisationException =>
          Logger.warn(s"Access forbidden because of ${e.getMessage}")
          Results.Forbidden
      }
  }
}
