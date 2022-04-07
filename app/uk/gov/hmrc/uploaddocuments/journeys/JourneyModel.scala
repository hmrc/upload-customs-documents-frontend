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

package uk.gov.hmrc.uploaddocuments.journeys

import uk.gov.hmrc.uploaddocuments.connectors.{FileUploadResultPushConnector, UpscanInitiateRequest, UpscanInitiateResponse}
import uk.gov.hmrc.uploaddocuments.models._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import uk.gov.hmrc.uploaddocuments.support.UploadLog

object JourneyModel {

  /** Minimum time gap to allow overwriting upload status. */
  final val minStatusOverwriteGapInMilliseconds: Long = 1000

  type UpscanRequestBuilder = (String, Long) => UpscanInitiateRequest
  type UpscanInitiateApi = (String, UpscanInitiateRequest) => Future[UpscanInitiateResponse]
  type FileUploadResultPushApi =
    FileUploadResultPushConnector.Request => Future[FileUploadResultPushConnector.Response]

  /** Common file upload initialization helper. */
  private[journeys] final def gotoUploadSingleFileOrSummary(
    context: FileUploadContext,
    upscanRequest: UpscanRequestBuilder,
    upscanInitiate: UpscanInitiateApi,
    fileUploadsOpt: Option[FileUploads],
    showUploadSummaryIfAny: Boolean
  )(implicit ec: ExecutionContext): Future[State] = {
    val fileUploads = fileUploadsOpt.getOrElse(FileUploads())
    if (
      (showUploadSummaryIfAny && fileUploads.nonEmpty) || fileUploads.acceptedCount >= context.config.maximumNumberOfFiles
    )
      goto(
        State.Summary(context, fileUploads)
      )
    else {
      val nonce = Nonce.random
      for {
        upscanResponse <-
          upscanInitiate(
            context.hostService.userAgent,
            upscanRequest(nonce.toString(), context.config.maximumFileSizeBytes)
          )
      } yield State.UploadSingleFile(
        context,
        upscanResponse.reference,
        upscanResponse.uploadRequest,
        fileUploads + FileUpload.Initiated(nonce, Timestamp.now, upscanResponse.reference, None, None)
      )
    }
  }

  import State._

  final def initialize(hostService: HostService)(
    request: FileUploadInitializationRequest
  ) =
    Transition[State] { case _ =>
      val fileUploadContext = FileUploadContext(request.config, hostService)
      if (fileUploadContext.isValid)
        goto(
          Initialized(
            fileUploadContext,
            request.toFileUploads
          )
        )
      else
        fail(new Exception(s"Invalid initialization request $fileUploadContext"))
    }

  final val continueToHost =
    Transition[State] {
      case s: FileUploadState    => goto(ContinueToHost(s.context, s.fileUploads))
      case s: CanEnterFileUpload => goto(ContinueToHost(s.context, s.fileUploadsOpt.getOrElse(FileUploads())))
      case _                     => goto(Uninitialized)
    }

  final def continueWithYesNo(selectedYes: Boolean) =
    Transition[State] {
      case s: FileUploadState =>
        goto(
          if (selectedYes) Initialized(s.context, s.fileUploads)
          else ContinueToHost(s.context, s.fileUploads)
        )
      case s: CanEnterFileUpload =>
        goto(
          if (selectedYes) Initialized(s.context, s.fileUploadsOpt.getOrElse(FileUploads()))
          else ContinueToHost(s.context, s.fileUploadsOpt.getOrElse(FileUploads()))
        )
      case _ => goto(Uninitialized)
    }

  final val wipeOut =
    Transition[State] { case _ => goto(Uninitialized) }

  private def resetFileUploadStatusToInitiated(reference: String, fileUploads: FileUploads): FileUploads =
    fileUploads.copy(files = fileUploads.files.map {
      case f if f.reference == reference =>
        FileUpload.Initiated(f.nonce, Timestamp.now, f.reference, None, None)
      case other => other
    })

  final def toUploadMultipleFiles(preferUploadMultipleFiles: Boolean = true) =
    Transition[State] {
      case current: UploadMultipleFiles =>
        goto(current.copy(fileUploads = current.fileUploads.onlyAccepted))

      case state: CanEnterFileUpload =>
        if (preferUploadMultipleFiles)
          goto(
            UploadMultipleFiles(
              context = state.context,
              fileUploads = state.fileUploadsOpt.map(_.onlyAccepted).getOrElse(FileUploads())
            )
          )
        else {
          goto(
            SwitchToUploadSingleFile(
              context = state.context,
              fileUploadsOpt = state.fileUploadsOpt
            )
          )
        }

      case state: FileUploadState =>
        goto(UploadMultipleFiles(state.context, state.fileUploads.onlyAccepted))

    }

  final def initiateNextFileUpload(uploadId: String)(
    upscanRequest: UpscanRequestBuilder
  )(upscanInitiate: UpscanInitiateApi)(implicit ec: ExecutionContext) =
    Transition[State] { case state: UploadMultipleFiles =>
      if (
        !state.fileUploads.hasUploadId(uploadId) &&
        state.fileUploads.initiatedOrAcceptedCount < state.context.config.maximumNumberOfFiles
      ) {
        val nonce = Nonce.random
        upscanInitiate(
          state.context.hostService.userAgent,
          upscanRequest(
            nonce.toString(),
            state.context.config.maximumFileSizeBytes
          )
        )
          .flatMap { upscanResponse =>
            goto(
              state.copy(fileUploads =
                state.fileUploads + FileUpload
                  .Initiated(
                    nonce,
                    Timestamp.now,
                    upscanResponse.reference,
                    Some(upscanResponse.uploadRequest),
                    Some(uploadId)
                  )
              )
            )
          }
      } else goto(state)
    }

  final def initiateFileUpload(
    upscanRequest: UpscanRequestBuilder
  )(upscanInitiate: UpscanInitiateApi)(implicit ec: ExecutionContext) =
    Transition[State] {
      case state: CanEnterFileUpload =>
        gotoUploadSingleFileOrSummary(
          state.context,
          upscanRequest,
          upscanInitiate,
          state.fileUploadsOpt,
          showUploadSummaryIfAny = !state.context.config.features.showYesNoQuestionBeforeContinue
        )

      case current @ UploadSingleFile(context, reference, uploadRequest, fileUploads, maybeUploadError) =>
        if (maybeUploadError.isDefined)
          goto(
            current
              .copy(fileUploads = resetFileUploadStatusToInitiated(reference, fileUploads))
          )
        else
          goto(current)

      case WaitingForFileVerification(
            config,
            reference,
            uploadRequest,
            currentFileUpload,
            fileUploads
          ) =>
        goto(UploadSingleFile(config, reference, uploadRequest, fileUploads))

      case current @ Summary(context, fileUploads, _) =>
        if (fileUploads.acceptedCount >= context.config.maximumNumberOfFiles)
          goto(current)
        else
          gotoUploadSingleFileOrSummary(
            context,
            upscanRequest,
            upscanInitiate,
            Some(fileUploads),
            showUploadSummaryIfAny = false
          )

      case UploadMultipleFiles(context, fileUploads) =>
        gotoUploadSingleFileOrSummary(
          context,
          upscanRequest,
          upscanInitiate,
          Some(fileUploads),
          showUploadSummaryIfAny = false
        )

    }

  final def markUploadAsRejected(error: S3UploadError) =
    Transition[State] {
      case current @ UploadSingleFile(
            context,
            reference,
            uploadRequest,
            fileUploads,
            maybeUploadError
          ) =>
        UploadLog.failure(context, error)
        val now = Timestamp.now
        val updatedFileUploads = fileUploads.copy(files = fileUploads.files.map {
          case fu @ FileUpload.Initiated(nonce, _, ref, _, _)
              if ref == error.key && canOverwriteFileUploadStatus(fu, true, now) =>
            FileUpload.Rejected(nonce, Timestamp.now, ref, error)
          case u => u
        })
        goto(current.copy(fileUploads = updatedFileUploads, maybeUploadError = Some(FileTransmissionFailed(error))))

      case current @ UploadMultipleFiles(context, fileUploads) =>
        UploadLog.failure(context, error)
        val updatedFileUploads = fileUploads.copy(files = fileUploads.files.map {
          case FileUpload(nonce, ref, _) if ref == error.key =>
            FileUpload.Rejected(nonce, Timestamp.now, ref, error)
          case u => u
        })
        goto(current.copy(fileUploads = updatedFileUploads))
    }

  final def markUploadAsPosted(receipt: S3UploadSuccess) =
    Transition[State] { case current @ UploadMultipleFiles(context, fileUploads) =>
      val now = Timestamp.now
      val updatedFileUploads =
        fileUploads.copy(files = fileUploads.files.map {
          case fu @ FileUpload(nonce, ref, _) if ref == receipt.key && canOverwriteFileUploadStatus(fu, true, now) =>
            FileUpload.Posted(nonce, Timestamp.now, ref)
          case u => u
        })
      goto(current.copy(fileUploads = updatedFileUploads))
    }

  /** Common transition helper based on the file upload status. */
  final def commonFileUploadStatusHandler(
    context: FileUploadContext,
    fileUploads: FileUploads,
    reference: String,
    uploadRequest: UploadRequest,
    fallbackState: => State
  ): PartialFunction[Option[FileUpload], Future[State]] = {

    case None =>
      goto(fallbackState)

    case Some(initiatedFile: FileUpload.Initiated) =>
      goto(UploadSingleFile(context, reference, uploadRequest, fileUploads))

    case Some(postedFile: FileUpload.Posted) =>
      goto(
        WaitingForFileVerification(
          context,
          reference,
          uploadRequest,
          postedFile,
          fileUploads
        )
      )

    case Some(acceptedFile: FileUpload.Accepted) =>
      goto(Summary(context, fileUploads))

    case Some(failedFile: FileUpload.Failed) =>
      goto(
        UploadSingleFile(
          context,
          reference,
          uploadRequest,
          fileUploads,
          Some(FileVerificationFailed(failedFile.details))
        )
      )

    case Some(rejectedFile: FileUpload.Rejected) =>
      goto(
        UploadSingleFile(
          context,
          reference,
          uploadRequest,
          fileUploads,
          Some(FileTransmissionFailed(rejectedFile.details))
        )
      )

    case Some(duplicatedFile: FileUpload.Duplicate) =>
      goto(
        UploadSingleFile(
          context,
          reference,
          uploadRequest,
          fileUploads,
          Some(
            DuplicateFileUpload(
              duplicatedFile.checksum,
              duplicatedFile.existingFileName,
              duplicatedFile.duplicateFileName
            )
          )
        )
      )
  }

  /** Transition when file has been uploaded and should wait for verification. */
  final val waitForFileVerification =
    Transition[State] {
      /** Change file status to posted and wait. */
      case current @ UploadSingleFile(
            context,
            reference,
            uploadRequest,
            fileUploads,
            errorOpt
          ) =>
        val updatedFileUploads = fileUploads.copy(files = fileUploads.files.map {
          case FileUpload.Initiated(nonce, _, ref, _, _) if ref == reference =>
            FileUpload.Posted(nonce, Timestamp.now, reference)
          case other => other
        })
        val currentUpload = updatedFileUploads.files.find(_.reference == reference)
        commonFileUploadStatusHandler(
          context,
          updatedFileUploads,
          reference,
          uploadRequest,
          current.copy(fileUploads = updatedFileUploads)
        )
          .apply(currentUpload)

      /** If waiting already, keep waiting. */
      case current @ WaitingForFileVerification(
            context,
            reference,
            uploadRequest,
            currentFileUpload,
            fileUploads
          ) =>
        val currentUpload = fileUploads.files.find(_.reference == reference)
        commonFileUploadStatusHandler(
          context,
          fileUploads,
          reference,
          uploadRequest,
          UploadSingleFile(context, reference, uploadRequest, fileUploads)
        )
          .apply(currentUpload)

      /** If file already uploaded, do nothing. */
      case state: Summary =>
        goto(state.copy(acknowledged = true))
    }

  final def canOverwriteFileUploadStatus(
    fileUpload: FileUpload,
    allowStatusOverwrite: Boolean,
    now: Timestamp
  ): Boolean =
    fileUpload.isNotReady ||
      (allowStatusOverwrite && now.isAfter(fileUpload.timestamp, minStatusOverwriteGapInMilliseconds))

  /** Transition when async notification arrives from the Upscan. */
  final def upscanCallbackArrived(
    pushfileUploadResult: FileUploadResultPushApi
  )(requestNonce: Nonce)(notification: UpscanNotification)(implicit ec: ExecutionContext) = {
    val now = Timestamp.now

    def updateFileUploads(
      fileUploads: FileUploads,
      allowStatusOverwrite: Boolean,
      context: FileUploadContext
    ): (FileUploads, Boolean) = {
      val modifiedFileUploads = fileUploads.copy(files = fileUploads.files.map {
        // update status of the file with matching nonce
        case fileUpload @ FileUpload(nonce, reference, _)
            if nonce.value == requestNonce.value && canOverwriteFileUploadStatus(
              fileUpload,
              allowStatusOverwrite,
              now
            ) =>
          notification match {
            case UpscanFileReady(_, url, uploadDetails) =>
              // check for existing file uploads with duplicated checksum
              val modifiedFileUpload: FileUpload = fileUploads.files
                .find(file =>
                  file.checksumOpt.contains(uploadDetails.checksum) && file.reference != notification.reference
                ) match {
                case Some(existingFileUpload: FileUpload.Accepted) =>
                  FileUpload.Duplicate(
                    nonce,
                    Timestamp.now,
                    reference,
                    uploadDetails.checksum,
                    existingFileName = existingFileUpload.fileName,
                    duplicateFileName = uploadDetails.fileName
                  )
                case _ =>
                  UploadLog.success(context, uploadDetails, fileUpload.timestamp)
                  FileUpload.Accepted(
                    nonce,
                    Timestamp.now,
                    reference,
                    url,
                    uploadDetails.uploadTimestamp,
                    uploadDetails.checksum,
                    FileUpload.sanitizeFileName(uploadDetails.fileName),
                    uploadDetails.fileMimeType,
                    uploadDetails.size,
                    description = context.config.newFileDescription
                  )
              }
              modifiedFileUpload

            case UpscanFileFailed(_, failureDetails) =>
              UploadLog.failure(context, failureDetails, fileUpload.timestamp)
              FileUpload.Failed(
                nonce,
                Timestamp.now,
                reference,
                failureDetails
              )
          }
        case u => u
      })
      (modifiedFileUploads, modifiedFileUploads.acceptedCount != fileUploads.acceptedCount)
    }

    Transition[State] {
      case current @ WaitingForFileVerification(
            context,
            reference,
            uploadRequest,
            currentFileUpload,
            fileUploads
          ) =>
        val (updatedFileUploads, newlyAccepted) =
          updateFileUploads(fileUploads, allowStatusOverwrite = false, context = context)
        (if (newlyAccepted)
           pushfileUploadResult(FileUploadResultPushConnector.Request.from(context, updatedFileUploads))
         else Future.successful(Right(())))
          .flatMap {
            case Left(e) => fail(new Exception(s"$e"))
            case Right(()) =>
              val currentUpload = updatedFileUploads.files.find(_.reference == reference)
              commonFileUploadStatusHandler(
                context,
                updatedFileUploads,
                reference,
                uploadRequest,
                current.copy(fileUploads = updatedFileUploads)
              )
                .apply(currentUpload)
          }

      case current @ UploadSingleFile(context, reference, uploadRequest, fileUploads, errorOpt) =>
        val (updatedFileUploads, newlyAccepted) =
          updateFileUploads(fileUploads, allowStatusOverwrite = false, context = context)
        (if (newlyAccepted)
           pushfileUploadResult(FileUploadResultPushConnector.Request.from(context, updatedFileUploads))
         else Future.successful(Right(())))
          .flatMap {
            case Left(e) => fail(new Exception(s"$e"))
            case Right(()) =>
              val currentUpload = updatedFileUploads.files.find(_.reference == reference)
              commonFileUploadStatusHandler(
                context,
                updatedFileUploads,
                reference,
                uploadRequest,
                current.copy(fileUploads = updatedFileUploads)
              )
                .apply(currentUpload)
          }

      case current @ UploadMultipleFiles(context, fileUploads) =>
        val (updatedFileUploads, newlyAccepted) =
          updateFileUploads(fileUploads, allowStatusOverwrite = true, context = context)
        (if (newlyAccepted)
           pushfileUploadResult(FileUploadResultPushConnector.Request.from(context, updatedFileUploads))
         else Future.successful(Right(())))
          .flatMap {
            case Left(e)   => fail(new Exception(s"$e"))
            case Right(()) => goto(current.copy(fileUploads = updatedFileUploads))
          }

      case current @ ContinueToHost(context, fileUploads) =>
        val (updatedFileUploads, newlyAccepted) =
          updateFileUploads(fileUploads, allowStatusOverwrite = true, context = context)
        (if (newlyAccepted)
           pushfileUploadResult(FileUploadResultPushConnector.Request.from(context, updatedFileUploads))
         else Future.successful(Right(())))
          .flatMap {
            case Left(e)   => fail(new Exception(s"$e"))
            case Right(()) => goto(current.copy(fileUploads = updatedFileUploads))
          }
    }
  }

  final def submitedUploadAnotherFileChoice(
    upscanRequest: UpscanRequestBuilder
  )(
    upscanInitiate: UpscanInitiateApi
  )(exitFileUpload: Transition[State])(selectedYes: Boolean)(implicit ec: ExecutionContext) =
    Transition[State] {
      case current @ Summary(context, fileUploads, acknowledged) =>
        if (selectedYes && fileUploads.acceptedCount < context.config.maximumNumberOfFiles)
          if (current.context.features.showYesNoQuestionBeforeContinue)
            goto(Initialized(current.context, current.fileUploads))
          else
            gotoUploadSingleFileOrSummary(
              context,
              upscanRequest,
              upscanInitiate,
              Some(fileUploads),
              showUploadSummaryIfAny = false
            )
        else {
          exitFileUpload.apply(current)
        }
      case current: CanEnterFileUpload =>
        exitFileUpload.apply(current)

    }

  final def removeFileUploadByReference(reference: String)(
    upscanRequest: UpscanRequestBuilder
  )(upscanInitiate: UpscanInitiateApi)(
    pushfileUploadResult: FileUploadResultPushApi
  )(implicit ec: ExecutionContext) =
    Transition[State] {
      case current: Summary =>
        val updatedFileUploads = current.fileUploads
          .copy(files = current.fileUploads.files.filterNot(_.reference == reference))
        if (updatedFileUploads.acceptedCount != current.fileUploads.acceptedCount) {
          Try(pushfileUploadResult(FileUploadResultPushConnector.Request.from(current.context, updatedFileUploads)))
        }
        val updatedCurrentState = current.copy(fileUploads = updatedFileUploads)
        if (updatedFileUploads.isEmpty && current.context.config.minimumNumberOfFiles > 0)
          initiateFileUpload(upscanRequest)(upscanInitiate)
            .apply(updatedCurrentState)
        else
          goto(updatedCurrentState)

      case current: UploadMultipleFiles =>
        val updatedFileUploads = current.fileUploads
          .copy(files = current.fileUploads.files.filterNot(_.reference == reference))
        if (updatedFileUploads.acceptedCount != current.fileUploads.acceptedCount) {
          Try(pushfileUploadResult(FileUploadResultPushConnector.Request.from(current.context, updatedFileUploads)))
        }
        val updatedCurrentState = current.copy(fileUploads = updatedFileUploads)
        goto(updatedCurrentState)
    }

  final val backToSummary =
    Transition[State] {
      case s: FileUploadState =>
        if (s.fileUploads.nonEmpty || s.context.config.minimumNumberOfFiles == 0)
          goto(Summary(s.context, s.fileUploads, acknowledged = true))
        else
          continueToHost.apply(s)

      case s: CanEnterFileUpload =>
        if (s.fileUploadsOpt.exists(_.nonEmpty) || s.context.config.minimumNumberOfFiles == 0)
          goto(Summary(s.context, s.fileUploadsOpt.get, acknowledged = true))
        else
          continueToHost.apply(s)

      case s =>
        continueToHost.apply(s)
    }

  /** Replace the current state with the new one. */
  final def goto(state: State): Future[State] =
    Future.successful(state)

  /** Fail the transition */
  final def fail(exception: Exception): Future[State] =
    Future.failed(exception)

}
