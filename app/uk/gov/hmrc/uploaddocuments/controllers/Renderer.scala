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

import akka.actor.ActorSystem
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.uploaddocuments.connectors._
import uk.gov.hmrc.uploaddocuments.controllers.Forms._
import uk.gov.hmrc.uploaddocuments.journeys.State
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.views.UploadFileViewHelper
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

/** Component responsible for translating given session state into the action result. */
@Singleton
class Renderer @Inject() (
  views: uk.gov.hmrc.uploaddocuments.views.FileUploadViews,
  uploadFileViewHelper: UploadFileViewHelper,
  router: Router,
  appConfig: AppConfig,
  val actorSystem: ActorSystem
) extends FileStream {

  import uk.gov.hmrc.uploaddocuments.support.OptionalFormOps._

  final def display(state: State, breadcrumbs: List[State], formWithErrors: Option[Form[_]])(implicit
    request: Request[_],
    m: Messages
  ): Result = {
    state match {
      case State.Uninitialized =>
        Redirect(appConfig.govukStartUrl)

      case State.Initialized(config, fileUploads) =>
        if (router.preferUploadMultipleFiles)
          Redirect(router.showChooseMultipleFiles)
        else
          Redirect(router.showChooseSingleFile)

      case State.ContinueToHost(context, fileUploads) =>
        if (fileUploads.acceptedCount == 0)
          Redirect(context.config.getContinueWhenEmptyUrl)
        else if (fileUploads.acceptedCount >= context.config.maximumNumberOfFiles)
          Redirect(context.config.getContinueWhenFullUrl)
        else
          Redirect(context.config.continueUrl)

      case State.UploadMultipleFiles(context, fileUploads) =>
        Ok(
          views.uploadMultipleFilesView(
            minimumNumberOfFiles = context.config.minimumNumberOfFiles,
            maximumNumberOfFiles = context.config.maximumNumberOfFiles,
            initialNumberOfEmptyRows = context.config.initialNumberOfEmptyRows,
            maximumFileSizeBytes = context.config.maximumFileSizeBytes,
            filePickerAcceptFilter = context.config.getFilePickerAcceptFilter,
            allowedFileTypesHint = context.config.content.allowedFilesTypesHint
              .orElse(context.config.allowedFileExtensions)
              .getOrElse(context.config.allowedContentTypes),
            context.config.newFileDescription,
            initialFileUploads = fileUploads.files,
            initiateNextFileUpload = router.initiateNextFileUpload,
            checkFileVerificationStatus = router.checkFileVerificationStatus,
            removeFile = router.removeFileUploadByReferenceAsync,
            previewFile = router.previewFileUploadByReference,
            markFileRejected = router.markFileUploadAsRejectedAsync,
            continueAction =
              if (context.config.features.showYesNoQuestionBeforeContinue)
                router.continueWithYesNo
              else router.continueToHost,
            backLink = Call("GET", context.config.backlinkUrl),
            context.config.features.showYesNoQuestionBeforeContinue,
            context.config.content.yesNoQuestionText,
            formWithErrors.or(YesNoChoiceForm)
          )(implicitly[Request[_]], context.messages, context.config.features, context.config.content)
        )

      case State.UploadSingleFile(context, reference, uploadRequest, fileUploads, maybeUploadError) =>
        Ok(
          views.uploadSingleFileView(
            maxFileUploadsNumber = context.config.maximumNumberOfFiles,
            maximumFileSizeBytes = context.config.maximumFileSizeBytes,
            filePickerAcceptFilter = context.config.getFilePickerAcceptFilter,
            allowedFileTypesHint = context.config.content.allowedFilesTypesHint
              .orElse(context.config.allowedFileExtensions)
              .getOrElse(context.config.allowedContentTypes),
            context.config.newFileDescription,
            uploadRequest = uploadRequest,
            fileUploads = fileUploads,
            maybeUploadError = maybeUploadError,
            successAction = router.showSummary,
            failureAction = router.showChooseSingleFile,
            checkStatusAction = router.checkFileVerificationStatus(reference),
            backLink = backlink(breadcrumbs)
          )(implicitly[Request[_]], context.messages, context.config.features, context.config.content)
        )

      case State.WaitingForFileVerification(context, reference, _, _, _) =>
        Ok(
          views.waitingForFileVerificationView(
            successAction = router.showSummary,
            failureAction = router.showChooseSingleFile,
            checkStatusAction = router.checkFileVerificationStatus(reference),
            backLink = backlink(breadcrumbs)
          )(implicitly[Request[_]], context.messages, context.config.features, context.config.content)
        )

      case State.Summary(context, fileUploads, _) =>
        Ok(
          if (fileUploads.acceptedCount < context.config.maximumNumberOfFiles)
            views.summaryView(
              maxFileUploadsNumber = context.config.maximumNumberOfFiles,
              maximumFileSizeBytes = context.config.maximumFileSizeBytes,
              formWithErrors.or(YesNoChoiceForm),
              fileUploads,
              router.submitUploadAnotherFileChoice,
              router.previewFileUploadByReference,
              router.removeFileUploadByReference,
              backlink(breadcrumbs)
            )(implicitly[Request[_]], context.messages, context.config.features, context.config.content)
          else
            views.summaryNoChoiceView(
              context.config.maximumNumberOfFiles,
              fileUploads,
              router.continueToHost,
              router.previewFileUploadByReference,
              router.removeFileUploadByReference,
              Call("GET", context.config.backlinkUrl)
            )(implicitly[Request[_]], context.messages, context.config.features, context.config.content)
        )

      case _ => NotImplemented
    }
  }

  final def renderUploadRequestJson(
    uploadId: String
  ) =
    resultOf {
      case s: State.UploadMultipleFiles =>
        s.fileUploads
          .findReferenceAndUploadRequestForUploadId(uploadId) match {
          case Some((reference, uploadRequest)) =>
            val json =
              Json.obj(
                "upscanReference" -> reference,
                "uploadId"        -> uploadId,
                "uploadRequest"   -> UploadRequest.formats.writes(uploadRequest)
              )
            Ok(json)

          case None => NotFound
        }

      case _ => Forbidden
    }

  final def renderFileVerificationStatus(
    reference: String
  )(implicit messages: Messages) =
    resultOf {
      case s: State.FileUploadState =>
        s.fileUploads.files.find(_.reference == reference) match {
          case Some(file) =>
            Ok(
              Json.toJson(
                FileVerificationStatus(
                  file,
                  uploadFileViewHelper,
                  router.previewFileUploadByReference(_, _),
                  s.context.config.maximumFileSizeBytes.toInt,
                  s.context.config.content.allowedFilesTypesHint
                    .orElse(s.context.config.allowedFileExtensions)
                    .getOrElse(s.context.config.allowedContentTypes)
                )
              )
            )
          case None => NotFound
        }
      case _ => NotFound
    }

  final def initializationResponse =
    resultOf {
      case State.Initialized(context, _) =>
        Created.withHeaders(
          HeaderNames.LOCATION ->
            (
              if (!context.config.features.showUploadMultiple)
                router.showChooseSingleFile
              else
                routes.StartController.start
            ).url
        )
      case _ => BadRequest
    }

  final def renderFileRemovalStatus =
    resultOf {
      case s: State => NoContent
      case _        => BadRequest
    }

  final def streamFileFromUspcan(
    reference: String
  ) =
    asyncResultOf {
      case s: State.HasFileUploads =>
        s.fileUploads.files.find(_.reference == reference) match {
          case Some(file: FileUpload.Accepted) =>
            getFileStream(
              file.url,
              file.fileName,
              file.fileMimeType,
              file.fileSize,
              (fileName, fileMimeType) =>
                fileMimeType match {
                  case _ =>
                    HeaderNames.CONTENT_DISPOSITION ->
                      s"""inline; filename="${fileName.filter(_.toInt < 128)}"; filename*=utf-8''${RFC3986Encoder
                        .encode(fileName)}"""
                }
            )

          case _ => Future.successful(NotFound)
        }
      case _ => Future.successful(NotFound)

    }

  final def acknowledgeFileUploadRedirect = resultOf { case state =>
    (state match {
      case _: State.UploadMultipleFiles        => Created
      case _: State.Summary                    => Created
      case _: State.WaitingForFileVerification => Accepted
      case _                                   => NoContent
    }).withHeaders(HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
  }

  private def backlink(breadcrumbs: List[State]): Call =
    breadcrumbs.headOption
      .map(router.routeTo)
      .getOrElse(router.routeTo(State.Uninitialized))

  private def resultOf(
    f: PartialFunction[State, Result]
  ): ((State, List[State])) => Result =
    (stateAndBreadcrumbs: (State, List[State])) =>
      f.applyOrElse(stateAndBreadcrumbs._1, (_: State) => play.api.mvc.Results.NotImplemented)

  private def asyncResultOf(
    f: PartialFunction[State, Future[Result]]
  ): State => Future[Result] = { (state: State) =>
    f(state)
  }

}
