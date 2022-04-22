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

import play.api.mvc.{Action, AnyContent, Request}
import uk.gov.hmrc.uploaddocuments.connectors.{UpscanInitiateConnector, UpscanInitiateResponse}
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.repository.JourneyCacheRepository.DataKeys
import uk.gov.hmrc.uploaddocuments.utils.LoggerUtil
import uk.gov.hmrc.uploaddocuments.views.html.UploadSingleFileView

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ChooseSingleFileController @Inject()(
  upscanInitiateConnector: UpscanInitiateConnector,
  components: BaseControllerComponents,
  view: UploadSingleFileView
)(implicit ec: ExecutionContext)
    extends BaseController(components) with UpscanRequestSupport with LoggerUtil {

  // GET /choose-file
  // TODO: The way this works now is it renders the Rejection message but then wipes the state and re-initiates an upload
  //      This is probably un-desirable, it would be better if it kept knowledge of the rejected file and re-used the same
  //      Upscan details - this will mean changing models to store the initiate response on rejected status
  //      However. Given this is the NonJS version - it may not be a problem re-initiating as volumes will be significantly lower (if any)
  final val showChooseFile: Action[AnyContent] =
    Action.async { implicit request =>
      whenInSession { implicit journeyId =>
        whenAuthenticated {
          withJourneyContext { journeyContext =>
            withUploadedFiles { files =>
              val nonce = Nonce.random

              val oFailedFile = files.erroredFileUpload
              val oError      = oFailedFile.flatMap(FileUploadError(_))

              val initiateRequest =
                upscanRequest(journeyId)(nonce.toString, journeyContext.config.maximumFileSizeBytes)

              for {
                upscanResponse <- upscanInitiateConnector
                                   .initiate(journeyContext.hostService.userAgent, initiateRequest)
                updatedFiles = updateFileUploads(nonce, upscanResponse, files)
                _ <- components.newJourneyCacheRepository.put(journeyId)(DataKeys.uploadedFiles, updatedFiles)
              } yield Ok(renderView(journeyContext, upscanResponse, updatedFiles, oError))
            }
          }
        }
      }
    }

  private def updateFileUploads(nonce: Nonce, initiateResponse: UpscanInitiateResponse, files: FileUploads) =
    files.onlyAccepted + FileUpload.Initiated(
      nonce         = nonce,
      timestamp     = Timestamp.now,
      reference     = initiateResponse.reference,
      uploadRequest = Some(initiateResponse.uploadRequest),
      uploadId      = None
    )

  def renderView(
    journeyConfig: FileUploadContext,
    initiateResponse: UpscanInitiateResponse,
    files: FileUploads,
    maybeUploadError: Option[FileUploadError]
  )(implicit request: Request[_]) =
    view(
      maxFileUploadsNumber   = journeyConfig.config.maximumNumberOfFiles,
      maximumFileSizeBytes   = journeyConfig.config.maximumFileSizeBytes,
      filePickerAcceptFilter = journeyConfig.config.getFilePickerAcceptFilter,
      allowedFileTypesHint = journeyConfig.config.content.allowedFilesTypesHint
        .orElse(journeyConfig.config.allowedFileExtensions)
        .getOrElse(journeyConfig.config.allowedContentTypes),
      journeyConfig.config.newFileDescription,
      uploadRequest     = initiateResponse.uploadRequest,
      fileUploads       = files,
      maybeUploadError  = maybeUploadError,
      successAction     = routes.SummaryController.showSummary,
      failureAction     = routes.ChooseSingleFileController.showChooseFile,
      checkStatusAction = routes.FileVerificationController.checkFileVerificationStatus(initiateResponse.reference),
      backLink          = routes.StartController.start // TODO: Back Linking needs fixing! Set to start by default for now!!!
    )(implicitly[Request[_]], journeyConfig.messages, journeyConfig.config.features, journeyConfig.config.content)
}
