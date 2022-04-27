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

import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.uploaddocuments.connectors.FileUploadResultPushConnector
import uk.gov.hmrc.uploaddocuments.connectors.FileUploadResultPushConnector.Response
import uk.gov.hmrc.uploaddocuments.models.{FileUploadContext, FileUploads}
import uk.gov.hmrc.uploaddocuments.repository.JourneyCacheRepository.DataKeys

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveController @Inject()(
  fileUploadResultPushConnector: FileUploadResultPushConnector,
  components: BaseControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController(components) {

  // GET /uploaded/:reference/remove
  final def removeFileUploadByReference(reference: String): Action[AnyContent] =
    Action.async { implicit request =>
      whenInSession { implicit journeyId =>
        whenAuthenticated {
          withJourneyContext { journeyContext =>
            withUploadedFiles { files =>
              removeFile(files, reference, journeyId, journeyContext).map {
                case (Left(_), _) => InternalServerError
                case (Right(_), updatedFilesWithFileRemoved) =>
                  if (updatedFilesWithFileRemoved.isEmpty) {
                    Redirect(routes.ChooseSingleFileController.showChooseFile)
                  } else {
                    Redirect(routes.SummaryController.showSummary)
                  }
              }
            }
          }
        }
      }
    }

  // POST /uploaded/:reference/remove
  final def removeFileUploadByReferenceAsync(reference: String): Action[AnyContent] =
    Action.async { implicit request =>
      whenInSession { implicit journeyId =>
        whenAuthenticated {
          withJourneyContext { journeyContext =>
            withUploadedFiles { files =>
              removeFile(files, reference, journeyId, journeyContext).map { _ =>
                NoContent
              }
            }
          }
        }
      }
    }

  def removeFile(files: FileUploads, reference: String, journeyId: String, journeyContext: FileUploadContext)(
    implicit hc: HeaderCarrier): Future[(Response, FileUploads)] = {
    val updatedFiles = files.copy(files = files.files.filterNot(_.reference == reference))
    for {
      _ <- components.newJourneyCacheRepository.put(journeyId)(DataKeys.uploadedFiles, updatedFiles)
      result <- fileUploadResultPushConnector.push(FileUploadResultPushConnector.Request(journeyContext, updatedFiles))
    } yield (result, updatedFiles)
  }
}
