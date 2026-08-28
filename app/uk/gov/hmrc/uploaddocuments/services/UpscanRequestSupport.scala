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

package uk.gov.hmrc.uploaddocuments.services

import uk.gov.hmrc.uploaddocuments.controllers.{internal, routes}
import uk.gov.hmrc.uploaddocuments.models.{JourneyId, Nonce, UpscanInitiateRequest}
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

trait UpscanRequestSupport {
  val appConfig: AppConfig

  final def upscanRequest(nonce: Nonce, maximumFileSizeBytes: Long)(using JourneyId) =
    UpscanInitiateRequest(
      callbackUrl = callbackFromUpscan(nonce),
      successRedirect = Some(successRedirect()),
      errorRedirect = Some(errorRedirect()),
      minimumFileSize = Some(1),
      maximumFileSize = Some(maximumFileSizeBytes.toInt)
    )

  final def callbackFromUpscan(nonce: Nonce)(using journeyId: JourneyId) =
    appConfig.baseInternalCallbackUrl +
      internal.routes.CallbackFromUpscanController.callbackFromUpscan(journeyId, nonce.toString).url

  final def successRedirect(): String =
    appConfig.baseExternalCallbackUrl + routes.FilePostedController.markFileUploadAsPosted.url

  final def errorRedirect(): String =
    appConfig.baseExternalCallbackUrl + routes.FileRejectedController.markFileUploadAsRejected.url
}
