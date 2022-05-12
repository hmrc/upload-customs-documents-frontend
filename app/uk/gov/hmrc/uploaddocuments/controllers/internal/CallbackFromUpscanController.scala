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

package uk.gov.hmrc.uploaddocuments.controllers.internal

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.uploaddocuments.controllers.actions.JourneyContextAction
import uk.gov.hmrc.uploaddocuments.controllers.{BaseController, BaseControllerComponents, FileUploadsControllerHelper, JourneyContextControllerHelper}
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.models.requests.{AuthRequest, JourneyContextRequest}
import uk.gov.hmrc.uploaddocuments.services.{FileUploadService, JourneyContextService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class CallbackFromUpscanController @Inject()(components: BaseControllerComponents,
                                             override val fileUploadService: FileUploadService,
                                             journeyContext: JourneyContextAction)
                                            (implicit ec: ExecutionContext) extends BaseController(components) with FileUploadsControllerHelper {

  // POST /callback-from-upscan/journey/:journeyId/:nonce
  final def callbackFromUpscan(journeyId: JourneyId, nonce: String): Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withJsonBody[UpscanNotification] { payload =>
      logResponse(payload)
      journeyContext.invokeBlock(AuthRequest(request, journeyId, None), { implicit journeyContextRequest: JourneyContextRequest[_] =>
        fileUploadService.markFileWithUpscanResponseAndNotifyHost(
          payload, Nonce(nonce)
        )(journeyContextRequest.journeyContext, journeyContextRequest.journeyId, hc(journeyContextRequest)).map { _ => NoContent }
      })
    }
  }

  private val logResponse: UpscanNotification => Unit = {
    case UpscanFileReady(reference, _, _) =>
      Logger.info(s"[callbackFromUpscan] UpscanRef: '$reference', Status: 'READY'")
    case UpscanFileFailed(reference, failureDetails) =>
      Logger.info(s"[callbackFromUpscan] UpscanRef: '$reference', Status: '${failureDetails.failureReason.toString.toUpperCase}'")
  }
}
