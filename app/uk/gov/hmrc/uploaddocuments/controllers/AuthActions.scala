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

import play.api.mvc.{Request, Result}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import uk.gov.hmrc.uploaddocuments.support.CallOps

import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.Results
import play.api.Logger
import uk.gov.hmrc.play.http.HeaderCarrierConverter

trait AuthActions extends AuthorisedFunctions with AuthRedirects {

  protected def authorisedWithEnrolment[A](serviceName: String, identifierKey: String)(
    body: ((Option[String], Option[String])) => Future[Result]
  )(implicit request: Request[A], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] =
    authorised(
      Enrolment(serviceName)
        and AuthProviders(GovernmentGateway)
    )
      .retrieve(credentials and authorisedEnrolments) { case credentials ~ enrolments =>
        val id = for {
          enrolment  <- enrolments.getEnrolment(serviceName)
          identifier <- enrolment.getIdentifier(identifierKey)
        } yield identifier.value

        id.map(x => body((credentials.map(_.providerId), Some(x))))
          .getOrElse(throw InsufficientEnrolments())
      }
      .recover(handleFailure)

  protected def authorisedWithoutEnrolment[A](
    body: ((Option[String], Option[String])) => Future[Result]
  )(implicit request: Request[A], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] =
    authorised(AuthProviders(GovernmentGateway, PrivilegedApplication))
      .retrieve(credentials)(credentials => body((credentials.map(_.providerId), None)))
      .recover(handleFailure)

  protected def authorisedWithoutEnrolmentReturningForbidden[A](
    body: ((Option[String], Option[String])) => Future[Result]
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result] =
    authorised(AuthProviders(GovernmentGateway, PrivilegedApplication))
      .retrieve(credentials)(credentials => body((credentials.map(_.providerId), None)))
      .recover { case e: AuthorisationException =>
        Logger(getClass).warn(s"Access forbidden because of ${e.getMessage()}")
        Results.Forbidden
      }

  protected def whenAuthenticated[A](
    body: => Future[Result]
  )(implicit request: Request[A], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] =
    authorised(AuthProviders(GovernmentGateway, PrivilegedApplication))
      .retrieve(credentials)(_ => body)
      .recover(handleFailure)

  protected def whenAuthenticatedInBackchannel[A](
    body: => Future[Result]
  )(implicit request: Request[A], ec: ExecutionContext): Future[Result] = {
    implicit val hc = HeaderCarrierConverter.fromRequest(request) // required to process Session-ID from the cookie
    authorised(AuthProviders(GovernmentGateway, PrivilegedApplication))
      .retrieve(credentials)(_ => body)
      .recover { case e: AuthorisationException =>
        Logger(getClass).warn(s"Access forbidden because of ${e.getMessage()}")
        Results.Forbidden
      }
  }

  protected def handleFailure(implicit request: Request[_]): PartialFunction[Throwable, Result] = {
    case e: AuthorisationException =>
      val continueUrl = CallOps.localFriendlyUrl(env, config)(request.uri, request.host)
      toGGLogin(continueUrl)
  }

}
