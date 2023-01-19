/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.uploaddocuments.controllers.internal

import play.api.libs.json.JsValue
import play.api.mvc.Action
import uk.gov.hmrc.uploaddocuments.controllers.{BaseController, BaseControllerComponents, FileUploadsControllerHelper, JourneyContextControllerHelper}
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.services.{FileUploadService, JourneyContextService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CallbackFromUpscanController @Inject() (
  components: BaseControllerComponents,
  override val journeyContextService: JourneyContextService,
  override val fileUploadService: FileUploadService
)(implicit ec: ExecutionContext)
    extends BaseController(components) with FileUploadsControllerHelper with JourneyContextControllerHelper {

  // POST /callback-from-upscan/journey/:journeyId/:nonce
  final def callbackFromUpscan(journeyId: JourneyId, nonce: String): Action[JsValue] =
    Action.async(parse.tolerantJson) { implicit request =>
      withJsonBody[UpscanNotification] { payload =>
        implicit val journey: JourneyId = journeyId
        withJourneyContextWithErrorHandler {
          logErrorResponse(payload)
          Future.successful(NoContent)
        } { implicit journeyContext =>
          logSuccessResponse(payload)
          fileUploadService.markFileWithUpscanResponseAndNotifyHost(payload, Nonce(nonce)).map(_ => NoContent)
        }()
      }
    }

  private val logSuccessResponse: UpscanNotification => Unit = {
    case UpscanFileReady(reference, _, _) =>
      Logger.info(s"[callbackFromUpscan] UpscanRef: '$reference', Status: 'READY'")
    case UpscanFileFailed(reference, failureDetails) =>
      Logger.info(
        s"[callbackFromUpscan] UpscanRef: '$reference', Status: '${failureDetails.failureReason.toString.toUpperCase}'"
      )
  }

  private val logErrorResponse: UpscanNotification => Unit = {
    case UpscanFileReady(reference, _, _) =>
      Logger.error(s"[callbackFromUpscan][missingJourneyContext] UpscanRef: '$reference', Status: 'READY'")
    case UpscanFileFailed(reference, failureDetails) =>
      Logger.error(
        s"[callbackFromUpscan][missingJourneyContext] UpscanRef: '$reference', Status: '${failureDetails.failureReason.toString.toUpperCase}'"
      )
  }
}
