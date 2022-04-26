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

import akka.actor.{ActorSystem, Scheduler}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Request}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.uploaddocuments.models.{FileUpload, FileUploadContext, FileUploads, FileVerificationStatus, Timestamp}
import uk.gov.hmrc.uploaddocuments.repository.JourneyCacheRepository.DataKeys
import uk.gov.hmrc.uploaddocuments.services.{FileUploadService, FileVerificationService, ScheduleAfter}
import uk.gov.hmrc.uploaddocuments.views.html.WaitingForFileVerificationView
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileVerificationController @Inject()(
                                            components: BaseControllerComponents,
                                            waitingView: WaitingForFileVerificationView,
                                            actorSystem: ActorSystem,
                                            fileVerificationService: FileVerificationService
                                          )(implicit ec: ExecutionContext, appConfig: AppConfig)
  extends BaseController(components) {

  implicit val scheduler: Scheduler = actorSystem.scheduler

  // GET /file-verification?key
  def showWaitingForFileVerification(key: Option[String]): Action[AnyContent] =
    Action.async { implicit request =>
      key match {
        case None => Future(BadRequest)
        case Some(upscanReference) =>
          whenInSession { implicit journeyId =>
            whenAuthenticated {
              withJourneyContext { journeyContext =>
                val timeoutNanoTime: Long = System.nanoTime() + appConfig.upscanInitialWaitTime.toNanos
                fileVerificationService.waitForUpscanResponse(upscanReference, appConfig.upscanWaitInterval.toMillis, timeoutNanoTime)(
                  {
                    case _: FileUpload.Accepted => Future(Redirect(routes.SummaryController.showSummary))
                    case _                      => Future(Redirect(routes.ChooseSingleFileController.showChooseFile))
                  },
                  Future(Ok(renderWaitingView(journeyContext, upscanReference)))
                )
              }
            }
          }
      }
    }

  private def renderWaitingView(context: FileUploadContext, reference: String)(implicit request: Request[_]) =
    waitingView(
      successAction     = routes.SummaryController.showSummary,
      failureAction     = routes.ChooseSingleFileController.showChooseFile,
      checkStatusAction = routes.FileVerificationController.checkFileVerificationStatus(reference),
      backLink          = routes.StartController.start // TODO: Back Linking needs fixing! Set to start by default for now!!!
    )(implicitly[Request[_]], context.messages, context.config.features, context.config.content)

  // GET /file-verification/:reference/status
  final def checkFileVerificationStatus(reference: String): Action[AnyContent] =
    Action.async { implicit request =>
      whenInSession { implicit journeyId =>
        whenAuthenticated {
          withJourneyContext { journeyContext =>
            withUploadedFiles {
              _.files.find(_.reference == reference) match {
                case Some(file) =>
                  Future(
                    Ok(
                      Json.toJson(
                        FileVerificationStatus(
                          fileUpload           = file,
                          filePreviewUrl       = routes.PreviewController.previewFileUploadByReference,
                          maximumFileSizeBytes = journeyContext.config.maximumFileSizeBytes.toInt,
                          allowedFileTypesHint = journeyContext.config.content.allowedFilesTypesHint
                            .orElse(journeyContext.config.allowedFileExtensions)
                            .getOrElse(journeyContext.config.allowedContentTypes)
                        )
                      )
                    )
                  )
                case None => Future(NotFound)
              }
            }
          }
        }
      }
    }

  // GET /journey/:journeyId/file-verification?key
  final def asyncWaitingForFileVerification(journeyId: JourneyId, key: Option[String]): Action[AnyContent] =
    Action.async { implicit request =>
      implicit val journey: JourneyId = journeyId
      key match {
        case None => Future(BadRequest)
        case Some(upscanReference) =>
          withUploadedFiles { files =>
            val updatedFileUploads = FileUploads(files.files.map {
              case FileUpload.Initiated(nonce, _, `upscanReference`, _, _) =>
                FileUpload.Posted(nonce, Timestamp.now, upscanReference)
              case u => u
            })
            components.newJourneyCacheRepository
              .put(journeyId)(DataKeys.uploadedFiles, updatedFileUploads)
              .flatMap { _ =>
                val timeoutNanoTime: Long = System.nanoTime() + appConfig.upscanInitialWaitTime.toNanos
                fileVerificationService.waitForUpscanResponse(upscanReference, appConfig.upscanWaitInterval.toMillis, timeoutNanoTime)(
                  _ => Future(Created),
                  Future(Accepted)
                ).map(_.withHeaders(HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> "*"))
              }
          }
      }
    }
}
