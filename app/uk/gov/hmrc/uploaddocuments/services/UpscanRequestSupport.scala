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

package uk.gov.hmrc.uploaddocuments.services

import uk.gov.hmrc.uploaddocuments.controllers.{internal, routes}
import uk.gov.hmrc.uploaddocuments.models.requests.JourneyContextRequest
import uk.gov.hmrc.uploaddocuments.models.{Nonce, UpscanInitiateRequest}
import uk.gov.hmrc.uploaddocuments.support.JsEnabled.COOKIE_JSENABLED
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

trait UpscanRequestSupport {
  val appConfig: AppConfig

  final def upscanRequest(nonce: Nonce)(implicit request: JourneyContextRequest[_]) =
    UpscanInitiateRequest(
      callbackUrl     = callbackFromUpscan(nonce),
      successRedirect = Some(successRedirect()),
      errorRedirect   = Some(errorRedirect()),
      minimumFileSize = Some(1),
      maximumFileSize = Some(request.journeyContext.config.maximumFileSizeBytes.toInt)
    )

  final def upscanRequestWhenUploadingMultipleFiles(nonce: Nonce)(implicit request: JourneyContextRequest[_]) =
    UpscanInitiateRequest(
      callbackUrl     = callbackFromUpscan(nonce),
      successRedirect = Some(successRedirectWhenUploadingMultipleFiles()),
      errorRedirect   = Some(errorRedirect()),
      minimumFileSize = Some(1),
      maximumFileSize = Some(request.journeyContext.config.maximumFileSizeBytes.toInt)
    )

  final def callbackFromUpscan(nonce: Nonce)(implicit request: JourneyContextRequest[_]) =
    appConfig.baseInternalCallbackUrl +
      internal.routes.CallbackFromUpscanController.callbackFromUpscan(request.journeyId, nonce.toString).url

  final def successRedirect()(implicit request: JourneyContextRequest[_]): String =
    appConfig.baseExternalCallbackUrl + (request.cookies.get(COOKIE_JSENABLED) match {
      case Some(_) => routes.FileVerificationController.asyncWaitingForFileVerification(request.journeyId)
      case None    => routes.FileVerificationController.showWaitingForFileVerification(None)
    })

  final def successRedirectWhenUploadingMultipleFiles()(implicit request: JourneyContextRequest[_]): String =
    appConfig.baseExternalCallbackUrl + routes.FilePostedController.asyncMarkFileUploadAsPosted(request.journeyId)

  final def errorRedirect()(implicit request: JourneyContextRequest[_]): String =
    appConfig.baseExternalCallbackUrl + (request.cookies.get(COOKIE_JSENABLED) match {
      case Some(_) => routes.FileRejectedController.asyncMarkFileUploadAsRejected(request.journeyId)
      case None    => routes.FileRejectedController.markFileUploadAsRejected
    })
}
