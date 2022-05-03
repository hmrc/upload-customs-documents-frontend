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

package uk.gov.hmrc.uploaddocuments.support

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.uploaddocuments.models.{FileUploadContext, S3UploadError, Timestamp, UpscanNotification}
import uk.gov.hmrc.uploaddocuments.utils.LoggerUtil

trait UploadLog extends LoggerUtil {

  def logSuccess(
    context: FileUploadContext,
    uploadDetails: UpscanNotification.UploadDetails,
    timestamp: Timestamp): Unit = {
    val success =
      Success(
        context.hostService.userAgent,
        uploadDetails.fileMimeType,
        uploadDetails.size,
        duration = Some(timestamp.duration))
    Logger.info(s"json${Json.stringify(Json.toJson(success))}")
  }

  def logFailure(
    context: FileUploadContext,
    failureDetails: UpscanNotification.FailureDetails,
    timestamp: Timestamp
  ): Unit = {
    val failure =
      Failure(
        context.hostService.userAgent,
        failureDetails.failureReason.toString,
        failureDetails.message,
        duration = Some(timestamp.duration))
    Logger.info(s"json${Json.stringify(Json.toJson(failure))}")
  }

  def logFailure(context: FileUploadContext, error: S3UploadError): Unit = {
    val failure = Failure(context.hostService.userAgent, error.errorCode, error.errorMessage)
    Logger.info(s"json${Json.stringify(Json.toJson(failure))}")
  }

}

case class Success(
  service: String,
  fileMimeType: String,
  fileSize: Int,
  success: Boolean       = true,
  duration: Option[Long] = None
)

object Success {
  implicit val format: Format[Success] = Json.format[Success]
}

final case class Failure(
  service: String,
  error: String,
  description: String,
  success: Boolean       = false,
  duration: Option[Long] = None
)

object Failure {
  implicit val format: Format[Failure] = Json.format[Failure]
}
