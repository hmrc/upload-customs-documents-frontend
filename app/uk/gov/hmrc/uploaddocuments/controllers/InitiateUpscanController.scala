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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.uploaddocuments.connectors.UpscanInitiateConnector
import uk.gov.hmrc.uploaddocuments.journeys.{JourneyModel, State}
import uk.gov.hmrc.uploaddocuments.models.{FileUpload, Nonce, Timestamp, UploadRequest}
import uk.gov.hmrc.uploaddocuments.repository.NewJourneyCacheRepository
import uk.gov.hmrc.uploaddocuments.repository.NewJourneyCacheRepository.DataKeys
import uk.gov.hmrc.uploaddocuments.services.SessionStateService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class InitiateUpscanController @Inject() (
  sessionStateService: SessionStateService,
  upscanInitiateConnector: UpscanInitiateConnector,
  val router: Router,
  renderer: Renderer,
  components: BaseControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController(components) with UpscanRequestSupport {

  // POST /initiate-upscan/:uploadId
//  final def initiateNextFileUpload(uploadId: String): Action[AnyContent] =
//    Action.async { implicit request =>
//      whenInSession {
//        whenAuthenticated {
//          val sessionStateUpdate =
//            JourneyModel
//              .initiateNextFileUpload(uploadId)(upscanRequestWhenUploadingMultipleFiles(currentJourneyId))(
//                upscanInitiateConnector.initiate(_, _)
//              )
//          sessionStateService
//            .updateSessionState(sessionStateUpdate)
//            .map(renderUploadRequestJson(uploadId))
//        }
//      }
//    }
//
//  def renderUploadRequestJson(uploadId: String) =
//    renderer.resultOf {
//      case s: State.UploadMultipleFiles =>
//        s.fileUploads
//          .findReferenceAndUploadRequestForUploadId(uploadId) match {
//          case Some((reference, uploadRequest)) =>
//            val json =
//              Json.obj(
//                "upscanReference" -> reference,
//                "uploadId"        -> uploadId,
//                "uploadRequest"   -> UploadRequest.formats.writes(uploadRequest)
//              )
//            Ok(json)
//
//          case None => NotFound
//        }
//
//      case _ => Forbidden
//    }

  // POST /new/initiate-upscan/:uploadId
  final def initiateNextFileUpload(uploadId: String): Action[AnyContent] =
    Action.async { implicit request =>
      whenInSession {
        whenAuthenticated {
          withJourneyContext { journeyContext =>
            withUploadedFiles { files =>
              val nonce = Nonce.random
              for {
                upscanResponse <- upscanInitiateConnector.initiate(
                                    journeyContext.hostService.userAgent,
                                    upscanRequestWhenUploadingMultipleFiles(currentJourneyId)(
                                      nonce.toString,
                                      journeyContext.config.maximumFileSizeBytes
                                    )
                                  )
                updatedFiles = files + FileUpload.Initiated(
                                 nonce = nonce,
                                 timestamp = Timestamp.now,
                                 reference = upscanResponse.reference,
                                 uploadRequest = Some(upscanResponse.uploadRequest),
                                 uploadId = Some(uploadId)
                               )
                _ <- components.newJourneyCacheRepository.put(currentJourneyId)(DataKeys.uploadedFiles, updatedFiles)
              } yield {
                val json =
                  Json.obj(
                    "upscanReference" -> upscanResponse.reference,
                    "uploadId"        -> uploadId,
                    "uploadRequest"   -> UploadRequest.formats.writes(upscanResponse.uploadRequest)
                  )
                Ok(json)
              }
            }
          }
        }
      }
    }

}
