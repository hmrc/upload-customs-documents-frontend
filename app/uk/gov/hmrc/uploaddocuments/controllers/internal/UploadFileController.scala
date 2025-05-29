/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.uploaddocuments.controllers.{BaseController, BaseControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.uploaddocuments.controllers.JourneyContextControllerHelper
import scala.concurrent.Future
import uk.gov.hmrc.uploaddocuments.services.JourneyContextService
import uk.gov.hmrc.uploaddocuments.services.FileUploadService
import uk.gov.hmrc.uploaddocuments.services.InitiateUpscanService
import uk.gov.hmrc.uploaddocuments.models.FileToUpload
import play.api.libs.json.Json
import play.api.mvc.MultipartFormData
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.libs.ws.WSClient
import uk.gov.hmrc.uploaddocuments.models.JourneyId
import play.api.libs.json.JsValue
import uk.gov.hmrc.uploaddocuments.models.UpscanNotification
import uk.gov.hmrc.uploaddocuments.models.Nonce
import uk.gov.hmrc.uploaddocuments.models.FileUpload
import uk.gov.hmrc.uploaddocuments.models.UploadedFile
import uk.gov.hmrc.uploaddocuments.models.FileUpload.*
import uk.gov.hmrc.uploaddocuments.models.ErroredFileUpload

@Singleton
class UploadFileController @Inject() (
  components: BaseControllerComponents,
  val fileUploadService: FileUploadService,
  override val journeyContextService: JourneyContextService,
  upscanInitiateService: InitiateUpscanService,
  wsClient: WSClient
)(implicit ec: ExecutionContext)
    extends BaseController(components) with JourneyContextControllerHelper {

  // POST /internal/upload
  final val uploadFile: Action[AnyContent] = Action
    .async { implicit request =>
      implicit val hc = HeaderCarrierConverter.fromRequest(request) // required to process Session-ID from the cookie
      whenInSession { implicit journeyId =>
        whenAuthenticatedInBackchannel {
          withJourneyContextWithErrorHandler {
            Future.successful(BadRequest)
          } { implicit journeyContext =>
            request.body.asJson
              .flatMap(json => Json.fromJson[FileToUpload](json).asOpt)
              .match {
                case Some(fileToUpload) =>
                  Logger.debug(
                    s"[uploadFile] Call to upload file: '$journeyId', body: \n${Json.prettyPrint(Json.toJson(fileToUpload))}"
                  )
                  Logger.debug(s"[uploadFile] Initializing Upscan")
                  upscanInitiateService
                    .initiateFileContentUpload(fileToUpload.uploadId)
                    .flatMap {
                      case Some(upscanResponse) =>
                        Logger.debug(
                          s"[uploadFile] Upscan response: \n${Json.prettyPrint(Json.toJson(upscanResponse))}"
                        )
                        val source =
                          Source.apply[play.api.mvc.MultipartFormData.Part[Source[ByteString, Any]]](
                            upscanResponse.uploadRequest.fields.toList
                              .map((k, v) => MultipartFormData.DataPart(k, v))
                              ::: List(
                                MultipartFormData.FilePart(
                                  "file",
                                  fileToUpload.name,
                                  Some(fileToUpload.contentType),
                                  Source.single(ByteString.fromArray(fileToUpload.content))
                                )
                              )
                          )
                        Logger.debug(
                          s"[uploadFile] Uploading a file to ${upscanResponse.uploadRequest.href}"
                        )
                        wsClient
                          .url(upscanResponse.uploadRequest.href)
                          .post(source)
                          .flatMap(response =>
                            if response.status >= 200 && response.status < 300
                            then
                              Logger.debug(
                                s"[uploadFile] Uploading succeeded with status ${response.status}"
                              )
                              waitForFileVerification(upscanResponse.reference, 15)
                                .map {
                                  case Some(uploadedFile) =>
                                    Logger.debug(
                                      s"[uploadFile] Success, returning \n${Json.toJson(uploadedFile)}"
                                    )
                                    Created(Json.toJson(uploadedFile))

                                  case None =>
                                    BadRequest(s"Failure, no verified file found.")
                                }
                            else
                              Future.successful(
                                BadRequest(
                                  s"Uploading the file to ${upscanResponse.uploadRequest.href} has failed with ${response.status}"
                                )
                              )
                          )
                          .recover(e => BadRequest(s"Failure to upload a file because of $e"))
                      case None =>
                        Future.successful(BadRequest(s"Failure, no Upscan response received."))
                    }
                    .recover(e => BadRequest(s"Failure to initialize Upscan because of $e"))
                case None =>
                  Future.successful(BadRequest(s"Failure, wrong payload:\n${request.body}"))
              }
          }()
        }
      }
    }

  // POST /upload/callback-from-upscan/journey/:journeyId/:nonce
  final def callbackFromUpscan(journeyId: JourneyId, nonce: String): Action[JsValue] =
    Action.async(parse.tolerantJson) { implicit request =>
      withJsonBody[UpscanNotification] { payload =>
        implicit val journey: JourneyId = journeyId
        withJourneyContextWithErrorHandler {
          Future.successful(NoContent)
        } { implicit journeyContext =>
          Logger.debug(
            s"[callbackFromUpscan] Callback from upscan: '$journeyId', body: \n${Json.prettyPrint(Json.toJson(payload))}"
          )
          fileUploadService.markFileWithUpscanResponse(payload, Nonce(nonce)).map(_ => NoContent)
        }()
      }
    }

  private def waitForFileVerification(upscanReference: String, count: Int)(using
    JourneyId,
    ExecutionContext
  ): Future[Option[UploadedFile]] =
    if count == 0
    then Future.failed(new Exception(s"Timeout while waiting for verification of upload $upscanReference"))
    else {
      Thread.sleep(1000)
      fileUploadService.getFiles().flatMap {
        case Some(uploads) =>
          uploads.files.find(_.reference == upscanReference).match {
            case Some(upload: Accepted) =>
              Logger.debug(
                s"[waitForFileVerification] Found accepted file $upscanReference: \n$upload"
              )
              Future.successful(UploadedFile.apply(upload))

            case Some(error: ErroredFileUpload) =>
              Logger.debug(
                s"[waitForFileVerification] Found rejected file $upscanReference: \n$error"
              )
              Future.successful(None)

            case _ =>
              Logger.debug(
                s"[waitForFileVerification] File $upscanReference not verified yet, remaining attempts ${count - 1}"
              )
              waitForFileVerification(upscanReference, count - 1)
          }

        case None => waitForFileVerification(upscanReference, count - 1)
      }
    }
}
