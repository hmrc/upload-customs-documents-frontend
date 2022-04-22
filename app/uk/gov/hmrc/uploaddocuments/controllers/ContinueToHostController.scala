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
import uk.gov.hmrc.uploaddocuments.models.{FileUploadContext, FileUploads}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContinueToHostController @Inject()(components: BaseControllerComponents)
                                        (implicit ec: ExecutionContext) extends BaseController(components) {

  // GET /continue-to-host
  final val continueToHost: Action[AnyContent] =
    Action.async { implicit request =>
      whenInSession {
        whenAuthenticated {
          withJourneyContext { journeyConfig =>
            withUploadedFiles { files =>
              Future(Redirect(redirectRoute(files, journeyConfig)))
            }
          }
        }
      }
    }

  private def redirectRoute(fileUploads: FileUploads, context: FileUploadContext) =
    if (fileUploads.acceptedCount == 0)
      context.config.getContinueWhenEmptyUrl
    else if (fileUploads.acceptedCount >= context.config.maximumNumberOfFiles)
      context.config.getContinueWhenFullUrl
    else
      context.config.continueUrl
}
