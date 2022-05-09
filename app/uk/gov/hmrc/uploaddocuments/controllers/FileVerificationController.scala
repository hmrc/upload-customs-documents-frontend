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
import uk.gov.hmrc.uploaddocuments.models.{FileUpload, FileUploadContext, JourneyId}
import uk.gov.hmrc.uploaddocuments.services.{FileVerificationService, JourneyContextService}
import uk.gov.hmrc.uploaddocuments.views.html.WaitingForFileVerificationView
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileVerificationController @Inject()(components: BaseControllerComponents,
                                           waitingView: WaitingForFileVerificationView,
                                           actorSystem: ActorSystem,
                                           fileVerificationService: FileVerificationService,
                                           override val journeyContextService: JourneyContextService)
                                          (implicit ec: ExecutionContext, appConfig: AppConfig)
  extends BaseController(components) with JourneyContextControllerHelper {

  implicit val scheduler: Scheduler = actorSystem.scheduler

  // GET /file-verification?key
  def showWaitingForFileVerification(key: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    key match {
      case None => Future(BadRequest)
      case Some(upscanReference) =>
        whenInSession { implicit journeyId =>
          whenAuthenticated {
            withJourneyContext { implicit journeyContext =>
              val timeoutNanoTime: Long = System.nanoTime() + appConfig.upscanInitialWaitTime.toNanos
              fileVerificationService.waitForUpscanResponse(upscanReference, appConfig.upscanWaitInterval.toMillis, timeoutNanoTime)(
                {
                  case _: FileUpload.Accepted => Future(Redirect(routes.SummaryController.showSummary))
                  case _                      => Future(Redirect(routes.ChooseSingleFileController.showChooseFile(None)))
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
      failureAction     = routes.ChooseSingleFileController.showChooseFile(None),
      checkStatusAction = routes.FileVerificationController.checkFileVerificationStatus(reference),
      backLink          = routes.ChooseSingleFileController.showChooseFile(None)
    )(implicitly[Request[_]], context.messages, context.config.features, context.config.content)

  // GET /file-verification/:reference/status
  final def checkFileVerificationStatus(reference: String): Action[AnyContent] = Action.async { implicit request =>
    whenInSession { implicit journeyId =>
      whenAuthenticated {
        withJourneyContext { implicit journeyContext =>
          fileVerificationService.getFileVerificationStatus(reference).map {
            case Some(verificationStatus) =>
              Logger.info(s"[checkFileVerificationStatus] UpscanRef: '$reference', Status: ${verificationStatus.fileStatus}")
              Ok(Json.toJson(verificationStatus))
            case None =>
              Logger.error(s"[checkFileVerificationStatus] No File exists for UpscanRef: '$reference'")
              NotFound
          }
        }
      }
    }
  }

  // GET /journey/:journeyId/file-verification?key
  final def asyncWaitingForFileVerification(journeyId: JourneyId, key: Option[String]): Action[AnyContent] = Action.async {
    implicit val journey: JourneyId = journeyId
    key match {
      case None => Future(BadRequest)
      case Some(upscanReference) =>
        val timeoutNanoTime: Long = System.nanoTime() + appConfig.upscanInitialWaitTime.toNanos
        fileVerificationService.waitForUpscanResponse(upscanReference, appConfig.upscanWaitInterval.toMillis, timeoutNanoTime)(
          _ => Future(Created),
          Future(Accepted)
        ).map(_.withHeaders(HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> "*"))
    }
  }
}
