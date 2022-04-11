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

import play.api.libs.json.JsValue
import play.api.mvc.Action
import uk.gov.hmrc.uploaddocuments.connectors.FileUploadResultPushConnector
import uk.gov.hmrc.uploaddocuments.controllers.{BaseController, BaseControllerComponents}
import uk.gov.hmrc.uploaddocuments.journeys.JourneyModel
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.services.SessionStateService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class CallbackFromUpscanController @Inject() (
  sessionStateService: SessionStateService,
  fileUploadResultPushConnector: FileUploadResultPushConnector,
  components: BaseControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController(components) {

  // POST /callback-from-upscan/journey/:journeyId/:nonce
  final def callbackFromUpscan(journeyId: String, nonce: String): Action[JsValue] =
    Action.async(parse.tolerantJson) { implicit request =>
      withJsonBody[UpscanNotification] { payload =>
        whenInSession {
          val sessionStateUpdate =
            JourneyModel
              .upscanCallbackArrived(fileUploadResultPushConnector.push(_))(Nonce(nonce))(payload)
          sessionStateService
            .updateSessionState(sessionStateUpdate)
            .map(_ => NoContent)
        }
      }
    }

}
