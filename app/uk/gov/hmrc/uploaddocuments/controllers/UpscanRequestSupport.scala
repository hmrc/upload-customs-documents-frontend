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

trait UpscanRequestSupport {

  def router: Router

  final def upscanRequest(journeyId: String)(nonce: String, maximumFileSizeBytes: Long)(implicit
    rh: RequestHeader
  ) =
    UpscanInitiateRequest(
      callbackUrl = router.callbackFromUpscan(journeyId, nonce),
      successRedirect = Some(router.successRedirect(journeyId)),
      errorRedirect = Some(router.errorRedirect(journeyId)),
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
      callbackUrl = router.callbackFromUpscan(journeyId, nonce),
      successRedirect = Some(router.successRedirectWhenUploadingMultipleFiles(journeyId)),
      errorRedirect = Some(router.errorRedirect(journeyId)),
      minimumFileSize = Some(1),
      maximumFileSize = Some(maximumFileSizeBytes.toInt)
    )

}
