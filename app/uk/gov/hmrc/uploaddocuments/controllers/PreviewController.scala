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
import uk.gov.hmrc.uploaddocuments.connectors.{FileUploadResultPushConnector, UpscanInitiateConnector}
import uk.gov.hmrc.uploaddocuments.services.SessionStateService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class PreviewController @Inject() (
  sessionStateService: SessionStateService,
  upscanInitiateConnector: UpscanInitiateConnector,
  fileUploadResultPushConnector: FileUploadResultPushConnector,
  router: Router,
  renderer: Renderer,
  components: BaseControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController(components) {

  // GET /preview/:reference/:fileName
  final def previewFileUploadByReference(reference: String, fileName: String): Action[AnyContent] =
    Action.async { implicit request =>
      whenInSession {
        whenAuthenticated {
          sessionStateService.currentSessionState
            .flatMap {
              case Some((state, _)) =>
                renderer.streamFileFromUspcan(reference)(state)

              case None =>
                NotFound.asFuture
            }
        }
      }
    }
}
