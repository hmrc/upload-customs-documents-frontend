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

import play.api.mvc.RequestHeader
import uk.gov.hmrc.uploaddocuments.connectors.UpscanInitiateRequest

trait UpscanRequestSupport { baseController: BaseController =>

  final def upscanRequest(journeyId: String)(nonce: String, maximumFileSizeBytes: Long)(implicit
    rh: RequestHeader
  ) =
    UpscanInitiateRequest(
      callbackUrl = callbackFromUpscan(journeyId, nonce),
      successRedirect = Some(successRedirect(journeyId)),
      errorRedirect = Some(errorRedirect(journeyId)),
      minimumFileSize = Some(1),
      maximumFileSize = Some(maximumFileSizeBytes.toInt)
    )

  final def upscanRequestWhenUploadingMultipleFiles(journeyId: String)(
    nonce: String,
    maximumFileSizeBytes: Long
  )(implicit
    rh: RequestHeader
  ) =
    UpscanInitiateRequest(
      callbackUrl = callbackFromUpscan(journeyId, nonce),
      successRedirect = Some(successRedirectWhenUploadingMultipleFiles(journeyId)),
      errorRedirect = Some(errorRedirect(journeyId)),
      minimumFileSize = Some(1),
      maximumFileSize = Some(maximumFileSizeBytes.toInt)
    )

  final def callbackFromUpscan(journeyId: String, nonce: String) =
    baseController.components.appConfig.baseInternalCallbackUrl +
      internal.routes.CallbackFromUpscanController.callbackFromUpscan(journeyId, nonce).url

  final def successRedirect(journeyId: String)(implicit rh: RequestHeader): String =
    baseController.components.appConfig.baseExternalCallbackUrl + (rh.cookies.get(COOKIE_JSENABLED) match {
      case Some(_) => routes.FileVerificationController.asyncWaitingForFileVerification(journeyId)
      case None    => routes.FileVerificationController.showWaitingForFileVerification(None)
    })

  final def successRedirectWhenUploadingMultipleFiles(journeyId: String): String =
    baseController.components.appConfig.baseExternalCallbackUrl + routes.FilePostedController.asyncMarkFileUploadAsPosted(journeyId)

  final def errorRedirect(journeyId: String)(implicit rh: RequestHeader): String =
    baseController.components.appConfig.baseExternalCallbackUrl + (rh.cookies.get(COOKIE_JSENABLED) match {
      case Some(_) => routes.FileRejectedController.asyncMarkFileUploadAsRejected(journeyId)
      case None    => routes.FileRejectedController.markFileUploadAsRejected
    })

}
