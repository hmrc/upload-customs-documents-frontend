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

import play.api.mvc.Results.*
import play.api.mvc.{Request, Result, Results}
import play.api.{Configuration, Environment, Mode}
import uk.gov.hmrc.auth.core.AuthProvider.{GovernmentGateway, PrivilegedApplication}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.uploaddocuments.support.CallOps
import uk.gov.hmrc.uploaddocuments.utils.LoggerUtil

import scala.concurrent.{ExecutionContext, Future}

trait AuthActions extends AuthorisedFunctions with AuthRedirects with LoggerUtil {

  protected def whenAuthenticated[A](
    body: => Future[Result]
  )(implicit request: Request[A], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] =
    authorised(AuthProviders(GovernmentGateway, PrivilegedApplication))
      .retrieve(credentials)(_ => body)
      .recover { case e: AuthorisationException =>
        Logger.warn(s"Access forbidden because of ${e.getMessage}")
        val continueUrl = CallOps.localFriendlyUrl(env, config)(request.uri, request.host)
        toGGLogin(continueUrl)
      }

  protected def whenAuthenticatedInBackchannel[A](
    body: => Future[Result]
  )(implicit request: Request[A], ec: ExecutionContext): Future[Result] = {
    implicit val hc = HeaderCarrierConverter.fromRequest(request) // required to process Session-ID from the cookie
    authorised(AuthProviders(GovernmentGateway, PrivilegedApplication))
      .retrieve(credentials)(_ => body)
      .recover { case e: AuthorisationException =>
        Logger.warn(s"Access forbidden because of ${e.getMessage}")
        Results.Forbidden
      }
  }
}

// The following trait has been inlined because of
// https://github.com/hmrc/bootstrap-play/blob/0a1273f794ffcd1c993ac62fd70cea607d2c2b30/README.md#L262
trait AuthRedirects {

  /* Since this is a library for Play >= 2.5 we avoid depending on global configuration/environment
   * and thus do not depend on the play-config API which still uses the deprecated globals. */

  def config: Configuration

  def env: Environment

  private lazy val envPrefix =
    if (env.mode.equals(Mode.Test)) "Test"
    else
      config
        .getOptional[String]("run.mode")
        .getOrElse("Dev")

  private val hostDefaults: Map[String, String] = Map(
    "Dev.external-url.bas-gateway-frontend.host"           -> "http://localhost:9553",
    "Dev.external-url.citizen-auth-frontend.host"          -> "http://localhost:9029",
    "Dev.external-url.identity-verification-frontend.host" -> "http://localhost:9938",
    "Dev.external-url.stride-auth-frontend.host"           -> "http://localhost:9041"
  )

  private def host(service: String): String = {
    val key = s"$envPrefix.external-url.$service.host"
    config.getOptional[String](key).orElse(hostDefaults.get(key)).getOrElse("")
  }

  def ggLoginUrl: String = host("bas-gateway-frontend") + "/bas-gateway/sign-in"

  final lazy val defaultOrigin: String =
    config
      .getOptional[String]("sosOrigin")
      .orElse(config.getOptional[String]("appName"))
      .getOrElse("undefined")

  def origin: String = defaultOrigin

  def toGGLogin(continueUrl: String): Result =
    Redirect(
      ggLoginUrl,
      Map(
        "continue_url" -> Seq(continueUrl),
        "origin"       -> Seq(origin)
      )
    )

}
