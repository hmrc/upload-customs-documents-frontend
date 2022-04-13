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

import play.api.mvc.{Action, AnyContent, Result}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.uploaddocuments.journeys.JourneyModel.canOverwriteFileUploadStatus
import uk.gov.hmrc.uploaddocuments.models.{FileUpload, Timestamp}
import uk.gov.hmrc.uploaddocuments.repository.NewJourneyCacheRepository.DataKeys
import uk.gov.hmrc.uploaddocuments.support.UploadLog

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileRejectedController @Inject()(components: BaseControllerComponents)
                                      (implicit ec: ExecutionContext) extends BaseController(components) {

  // GET /file-rejected
  final val markFileUploadAsRejected: Action[AnyContent] =
    Action.async { implicit request =>
      whenInSession {
        whenAuthenticated {
          withJourneyContext { journeyContext =>
            withUploadedFiles { files =>
              Forms.UpscanUploadErrorForm.bindFromRequest
                .fold(
                  formWithErrors => Future(Redirect(routes.ChooseSingleFileController.showChooseFile).withFormError(formWithErrors)),
                  s3UploadError => {
                    UploadLog.failure(journeyContext, s3UploadError)
                    val now = Timestamp.now
                    val updatedFileUploads = files.copy(files = files.files.map {
                      case fu@FileUpload.Initiated(nonce, _, ref, _, _)
                        if ref == s3UploadError.key && canOverwriteFileUploadStatus(fu, true, now) =>
                        FileUpload.Rejected(nonce, Timestamp.now, ref, s3UploadError)
                      case u => u
                    })
                    components.newJourneyCacheRepository.put(currentJourneyId)(DataKeys.uploadedFiles, updatedFileUploads).map { _ =>
                      Redirect(routes.ChooseSingleFileController.showChooseFile)
                    }
                  }
                )
            }
          }
        }
      }
    }

  // POST /file-rejected
  final val markFileUploadAsRejectedAsync: Action[AnyContent] = rejectedAsynLogicWithStatus(Created)

  // GET /journey/:journeyId/file-rejected
  final def asyncMarkFileUploadAsRejected(journeyId: String): Action[AnyContent] = rejectedAsynLogicWithStatus(NoContent)

  private def rejectedAsynLogicWithStatus(status: Result) = Action.async { implicit request =>
    whenInSession {
      whenAuthenticated {
        withJourneyContext { journeyContext =>
          withUploadedFiles { files =>
            Forms.UpscanUploadErrorForm.bindFromRequest
              .fold(
                formWithErrors => Future(Redirect(routes.ChooseMultipleFilesController.showChooseMultipleFiles).withFormError(formWithErrors)),
                s3UploadError => {
                  UploadLog.failure(journeyContext, s3UploadError)
                  val updatedFileUploads = files.copy(files = files.files.map {
                    case FileUpload(nonce, ref, _) if ref == s3UploadError.key =>
                      FileUpload.Rejected(nonce, Timestamp.now, ref, s3UploadError)
                    case u => u
                  })
                  components.newJourneyCacheRepository.put(currentJourneyId)(DataKeys.uploadedFiles, updatedFileUploads).map { _ =>
                    status.withHeaders(HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
                  }
                }
              )
          }
        }
      }
    }
  }

  // OPTIONS /journey/:journeyId/file-rejected
  final def preflightUpload(journeyId: String): Action[AnyContent] =
    Action {
      Created.withHeaders(HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
    }

}
