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

import play.api.mvc.{Action, AnyContent, Call, Request}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.uploaddocuments.controllers.actions.{AuthAction, AuthenticatedAction, JourneyContextAction, JourneyContextActionI}
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.models.requests.JourneyContextRequest
import uk.gov.hmrc.uploaddocuments.services.{FileUploadService, InitiateUpscanService, JourneyContextService}
import uk.gov.hmrc.uploaddocuments.utils.LoggerUtil
import uk.gov.hmrc.uploaddocuments.views.html.UploadSingleFileView

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ChooseSingleFileController @Inject()(initiateUpscanService: InitiateUpscanService,
                                           components: BaseControllerComponents,
                                           view: UploadSingleFileView,
                                           @Named("authenticated") auth: AuthAction,
                                           journeyContext: JourneyContextAction,
                                           override val fileUploadService: FileUploadService)
                                          (implicit ec: ExecutionContext) extends BaseController(components) with FileUploadsControllerHelper {

  // GET /choose-file
  def showChooseFile(backLinkToSummary: Option[Boolean]): Action[AnyContent] = (auth andThen journeyContext).async { implicit request =>
    withFileUploads { files =>
      if(files.acceptedCount < request.journeyContext.config.maximumNumberOfFiles) {
        initiateUpscanService.initiateNextSingleFileUpload().map {
          case None => Redirect(components.appConfig.govukStartUrl)
          case Some((upscanResponse, updatedFiles, oError)) =>
            val view = renderView(upscanResponse, updatedFiles, oError, backLinkToSummary)
            oError.fold(Ok(view))(_ => BadRequest(view))
        }
      } else {
        Future.successful(Redirect(routes.SummaryController.showSummary))
      }
    }
  }

  private def renderView(initiateResponse: UpscanInitiateResponse,
                         files: FileUploads,
                         maybeUploadError: Option[FileUploadError],
                         backLinkToSummary: Option[Boolean])
                        (implicit request: JourneyContextRequest[_]): HtmlFormat.Appendable = {

    val config = request.journeyContext.config

    val backRoute = backLinkToSummary match {
      case Some(true) => routes.SummaryController.showSummary
      case _          => Call("GET", config.backlinkUrl)
    }

    view(
      maxFileUploadsNumber   = config.maximumNumberOfFiles,
      maximumFileSizeBytes   = config.maximumFileSizeBytes,
      filePickerAcceptFilter = config.getFilePickerAcceptFilter,
      allowedFileTypesHint = config.content.allowedFilesTypesHint
        .orElse(config.allowedFileExtensions)
        .getOrElse(config.allowedContentTypes),
      config.newFileDescription,
      uploadRequest     = initiateResponse.uploadRequest,
      fileUploads       = files,
      maybeUploadError  = maybeUploadError,
      successAction     = routes.SummaryController.showSummary,
      failureAction     = routes.ChooseSingleFileController.showChooseFile(backLinkToSummary),
      checkStatusAction = routes.FileVerificationController.checkFileVerificationStatus(initiateResponse.reference),
      backLink          = backRoute
    )(request, request.journeyContext.messages, config.features, config.content)
  }
}
