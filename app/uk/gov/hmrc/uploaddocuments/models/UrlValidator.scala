/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.uploaddocuments.models

import scala.util.Try
import java.net.URL
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

object UrlValidator {

  final def isValidFrontendUrl(url: String): Boolean =
    url.nonEmpty && Try(new URL(url))
      .map { url =>
        val isHttps = url.getProtocol == "https"
        val host    = url.getHost
        (host == "localhost") || (isHttps && host.endsWith(".gov.uk"))
      }
      .getOrElse(false)

  final def isReleativeUrl(url: String): Boolean =
    url.nonEmpty && RedirectUrl.isRelativeUrl(url)

  final def isValidCallbackUrl(url: String): Boolean =
    url.nonEmpty && Try(new URL(url))
      .map { url =>
        val isHttps = url.getProtocol == "https"
        val host    = url.getHost
        (host == "localhost") || (isHttps && host.endsWith(".mdtp"))
      }
      .getOrElse(false)
}
