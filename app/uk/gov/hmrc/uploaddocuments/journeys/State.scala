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

import uk.gov.hmrc.uploaddocuments.models.FileUploads
import uk.gov.hmrc.uploaddocuments.models.FileUploadContext
import uk.gov.hmrc.uploaddocuments.models.FileUpload
import uk.gov.hmrc.uploaddocuments.models.UploadRequest
import uk.gov.hmrc.uploaddocuments.models.FileUploadError

sealed trait State

sealed trait IsTransient extends State

object State {

  /** Root state of the journey. */
  final case object Uninitialized extends State

  final case class Initialized(context: FileUploadContext, fileUploads: FileUploads)
      extends State with CanEnterFileUpload with HasFileUploads {
    final def fileUploadsOpt: Option[FileUploads] =
      if (fileUploads.isEmpty) None else Some(fileUploads)
  }

  final case class UploadMultipleFiles(
    context: FileUploadContext,
    fileUploads: FileUploads
  ) extends FileUploadState

  final case class UploadSingleFile(
    context: FileUploadContext,
    reference: String,
    uploadRequest: UploadRequest,
    fileUploads: FileUploads,
    maybeUploadError: Option[FileUploadError] = None
  ) extends FileUploadState

  final case class WaitingForFileVerification(
    context: FileUploadContext,
    reference: String,
    uploadRequest: UploadRequest,
    currentFileUpload: FileUpload,
    fileUploads: FileUploads
  ) extends FileUploadState with IsTransient

  final case class Summary(
    context: FileUploadContext,
    fileUploads: FileUploads,
    acknowledged: Boolean = false
  ) extends FileUploadState

  final case class SwitchToUploadSingleFile(context: FileUploadContext, fileUploadsOpt: Option[FileUploads])
      extends CanEnterFileUpload with IsTransient

  final case class ContinueToHost(context: FileUploadContext, fileUploads: FileUploads)
      extends State with CanEnterFileUpload with HasFileUploads {
    final def fileUploadsOpt: Option[FileUploads] =
      if (fileUploads.isEmpty) None else Some(fileUploads)
  }

  sealed trait HasFileUploads {
    def fileUploads: FileUploads
  }

  sealed trait HasContext {
    def context: FileUploadContext
  }

  sealed trait CanEnterFileUpload extends State with HasContext {
    def context: FileUploadContext
    def fileUploadsOpt: Option[FileUploads]
  }

  sealed trait FileUploadState extends State with HasFileUploads with HasContext {
    def context: FileUploadContext
    def fileUploads: FileUploads
  }

}
