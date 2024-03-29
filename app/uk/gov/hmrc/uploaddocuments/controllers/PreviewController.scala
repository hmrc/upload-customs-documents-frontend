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

package uk.gov.hmrc.uploaddocuments.controllers

import org.apache.pekko.actor.ActorSystem
import play.api.mvc.{Action, AnyContent}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.uploaddocuments.connectors.FileStream
import uk.gov.hmrc.uploaddocuments.models.{FileUpload, FileUploads, RFC3986Encoder}
import uk.gov.hmrc.uploaddocuments.services.FileUploadService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PreviewController @Inject() (
  components: BaseControllerComponents,
  val actorSystem: ActorSystem,
  override val fileUploadService: FileUploadService
)(implicit ec: ExecutionContext)
    extends BaseController(components) with FileStream with FileUploadsControllerHelper {

  // GET /preview/:reference/:fileName
  final def previewFileUploadByReference(reference: String, fileName: String): Action[AnyContent] = Action.async {
    implicit request =>
      whenInSession { implicit journeyId =>
        whenAuthenticated {
          withFileUploads { files =>
            streamFileFromUspcan(reference, files)
          }
        }
      }
  }

  private def streamFileFromUspcan(reference: String, files: FileUploads) =
    files.files.find(_.reference == reference) match {
      case Some(file: FileUpload.Accepted) =>
        getFileStream(
          url = file.url,
          fileName = file.fileName,
          fileMimeType = file.fileMimeType,
          fileSize = file.fileSize,
          contentDispositionForMimeType = (fileName, fileMimeType) =>
            fileMimeType match {
              case _ =>
                HeaderNames.CONTENT_DISPOSITION ->
                  s"""inline; filename="${fileName.filter(_.toInt < 128)}"; filename*=utf-8''${RFC3986Encoder
                    .encode(fileName)}"""
            }
        )
      case _ => Future.successful(NotFound)
    }
}
