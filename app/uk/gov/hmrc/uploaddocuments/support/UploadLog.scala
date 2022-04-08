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

import play.api.Logger
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.uploaddocuments.models.{FileUploadContext, UpscanNotification}
import uk.gov.hmrc.uploaddocuments.models.S3UploadError
import uk.gov.hmrc.uploaddocuments.models.Timestamp

object UploadLog {

  val logger = Logger(getClass())

  case class Success(
    service: String,
    fileMimeType: String,
    fileSize: Int,
    success: Boolean = true,
    duration: Option[Long] = None
  )
  object Success {
    implicit val format: Format[Success] = Json.format[Success]
  }

  final case class Failure(
    service: String,
    error: String,
    description: String,
    success: Boolean = false,
    duration: Option[Long] = None
  )
  object Failure {
    implicit val format: Format[Failure] = Json.format[Failure]
  }

  def success(context: FileUploadContext, uploadDetails: UpscanNotification.UploadDetails, timestamp: Timestamp): Unit =
    logger.info(
      s"json${Json
        .stringify(Json.toJson(Success(context.hostService.userAgent, uploadDetails.fileMimeType, uploadDetails.size, duration = Some(timestamp.duration))))}"
    )

  def failure(
    context: FileUploadContext,
    failureDetails: UpscanNotification.FailureDetails,
    timestamp: Timestamp
  ): Unit =
    logger.info(
      s"json${Json.stringify(Json.toJson(Failure(context.hostService.userAgent, failureDetails.failureReason.toString(), failureDetails.message, duration = Some(timestamp.duration))))}"
    )

  def failure(context: FileUploadContext, error: S3UploadError): Unit =
    logger.info(
      s"json${Json.stringify(Json.toJson(Failure(context.hostService.userAgent, error.errorCode, error.errorMessage)))}"
    )

}
