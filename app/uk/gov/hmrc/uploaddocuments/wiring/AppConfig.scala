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

package uk.gov.hmrc.uploaddocuments.wiring

import com.google.inject.ImplementedBy
import play.api.i18n.Lang
import play.api.mvc.{Call, RequestHeader}
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject
import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration
import uk.gov.hmrc.uploaddocuments.connectors.Retries
import play.api.Configuration

@ImplementedBy(classOf[AppConfigImpl])
trait AppConfig {

  val appName: String = "upload-customs-documents-frontend"
  val baseInternalCallbackUrl: String
  val baseExternalCallbackUrl: String
  val authBaseUrl: String
  val upscanInitiateBaseUrl: String
  val mongoSessionExpiration: Duration
  val govukStartUrl: String

  val languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

  val routeToSwitchLanguage: String => Call =
    (lang: String) => uk.gov.hmrc.uploaddocuments.controllers.routes.LanguageSwitchController.switchToLanguage(lang)

  val contactHost: String
  val contactFormServiceIdentifier: String

  def requestUri(implicit request: RequestHeader): String =
    SafeRedirectUrl(baseExternalCallbackUrl + request.uri).encodedUrl

  def reportProblemUrl(implicit request: RequestHeader): String =
    s"$contactHost/contact/problem_reports_nonjs?newTab=true&service=$contactFormServiceIdentifier&backUrl=$requestUri"

  val signOutUrl: String
  val timeout: Int
  val countdown: Int
  val fileUploadResultPushRetryIntervals: Seq[FiniteDuration]
  val upscanInitialWaitTime: Duration
  val upscanWaitInterval: Duration
  def lockReleaseCheckInterval: Duration
  def lockTimeout: Duration
}

class AppConfigImpl @Inject()(config: ServicesConfig, configuration: Configuration) extends AppConfig {

  override val baseExternalCallbackUrl: String      = config.getString("urls.callback.external")
  override val baseInternalCallbackUrl: String      = config.getString("urls.callback.internal")
  override val authBaseUrl: String                  = config.baseUrl("auth")
  override val upscanInitiateBaseUrl: String        = config.baseUrl("upscan-initiate")
  override val mongoSessionExpiration: Duration     = config.getDuration("mongodb.session.expiration")
  override val contactHost: String                  = config.getString("contact-frontend.host")
  override val contactFormServiceIdentifier: String = config.getString("feedback-frontend.formIdentifier")
  override val signOutUrl: String                   = config.getString("urls.signOut")
  override val timeout: Int                         = config.getInt("session.timeoutSeconds")
  override val countdown: Int                       = config.getInt("session.countdownInSeconds")
  override val govukStartUrl: String                = config.getString("govuk.start.url")

  override val fileUploadResultPushRetryIntervals: Seq[FiniteDuration] =
    Retries.getConfIntervals("fileUploadResultPush.retryIntervals", configuration)

  override val upscanInitialWaitTime: Duration = Duration.apply(config.getString("upscan.initialCallbackWaitTime"))
  override val upscanWaitInterval: Duration = Duration.apply(config.getString("upscan.waitInterval"))
  override val lockReleaseCheckInterval: Duration = Duration.apply(config.getString("mongodb.lock.releaseCheckInterval"))
  override val lockTimeout: Duration = Duration.apply(config.getString("mongodb.lock.timeout"))
}
