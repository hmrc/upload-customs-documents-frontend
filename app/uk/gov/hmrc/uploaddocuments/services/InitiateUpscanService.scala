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

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.uploaddocuments.connectors.UpscanInitiateConnector
import uk.gov.hmrc.uploaddocuments.models.{FileUploadContext, FileUploadError, FileUploads, JourneyId, Nonce, UpscanInitiateResponse}
import uk.gov.hmrc.uploaddocuments.utils.LoggerUtil
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class InitiateUpscanService @Inject() (
  upscanInitiateConnector: UpscanInitiateConnector,
  fileUploadService: FileUploadService,
  val appConfig: AppConfig
)(using ExecutionContext)
    extends UpscanRequestSupport with LoggerUtil {

  def randomNonce: Nonce = Nonce.random

  def initiateNextFileUpload()(using
    journeyContext: FileUploadContext,
    journeyId: JourneyId,
    hc: HeaderCarrier
  ): Future[Option[(UpscanInitiateResponse, FileUploads, Option[FileUploadError])]] = {
    val nonce           = randomNonce
    val initiateRequest = upscanRequest(nonce, journeyContext.config.maximumFileSizeBytes)

    upscanInitiateConnector
      .initiate(journeyContext.hostService.userAgent, initiateRequest)
      .flatMap { upscanResponse =>
        fileUploadService.putInitiatedFile(nonce, upscanResponse).map {
          _.map(updatedFiles => (upscanResponse, updatedFiles, updatedFiles.tofileUploadErrors.headOption))
        }
      }
      .recoverWith { case e: Throwable =>
        Logger.error(s"[initiateNextFileUpload] Failed to initiate upscan: ${e.getMessage}")
        Future.successful(None)
      }
  }
}
