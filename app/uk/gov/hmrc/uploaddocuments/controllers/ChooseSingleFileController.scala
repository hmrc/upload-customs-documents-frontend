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
import uk.gov.hmrc.uploaddocuments.connectors.UpscanInitiateConnector
import uk.gov.hmrc.uploaddocuments.journeys.{JourneyModel, State}
import uk.gov.hmrc.uploaddocuments.models.FileUploadContext
import uk.gov.hmrc.uploaddocuments.services.SessionStateService
import uk.gov.hmrc.uploaddocuments.views.html.UploadSingleFileView

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ChooseSingleFileController @Inject() (
  sessionStateService: SessionStateService,
  upscanInitiateConnector: UpscanInitiateConnector,
  val router: Router,
  renderer: Renderer,
  components: BaseControllerComponents,
  view: UploadSingleFileView
)(implicit ec: ExecutionContext)
    extends BaseController(components) with UpscanRequestSupport {

  // GET /choose-file
  final val showChooseFile: Action[AnyContent] =
    Action.async { implicit request =>
      whenInSession {
        whenAuthenticated {
          withJourneyContext { journeyConfig =>
            val sessionStateUpdate =
              JourneyModel
                .initiateFileUpload(upscanRequest(currentJourneyId))(upscanInitiateConnector.initiate(_, _))
            sessionStateService
              .updateSessionState(sessionStateUpdate)
              .map {
                case (uploadSingleFile: State.UploadSingleFile, breadcrumbs) =>
                  // TODO: Change in future to retrieve state from Mongo rather than SessionStateService
                  Ok(renderView(journeyConfig, uploadSingleFile, breadcrumbs))

                case other =>
                  router.redirectTo(other)
              }
          }
        }
      }
    }

  def renderView(journeyConfig: FileUploadContext, uploadSingleFile: State.UploadSingleFile, breadcrumbs: List[State])(
    implicit request: Request[_]
  ) =
    view(
      maxFileUploadsNumber = journeyConfig.config.maximumNumberOfFiles,
      maximumFileSizeBytes = journeyConfig.config.maximumFileSizeBytes,
      filePickerAcceptFilter = journeyConfig.config.getFilePickerAcceptFilter,
      allowedFileTypesHint = journeyConfig.config.content.allowedFilesTypesHint
        .orElse(journeyConfig.config.allowedFileExtensions)
        .getOrElse(journeyConfig.config.allowedContentTypes),
      journeyConfig.config.newFileDescription,
      uploadRequest = uploadSingleFile.uploadRequest,
      fileUploads = uploadSingleFile.fileUploads,
      maybeUploadError = uploadSingleFile.maybeUploadError,
      successAction = router.showSummary,
      failureAction = router.showChooseSingleFile,
      checkStatusAction = router.checkFileVerificationStatus(uploadSingleFile.reference),
      backLink = renderer.backlink(breadcrumbs)
    )(implicitly[Request[_]], journeyConfig.messages, journeyConfig.config.features, journeyConfig.config.content)
}
