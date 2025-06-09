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

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.objectstore.client.play.Implicits.*
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient
import uk.gov.hmrc.objectstore.client.{Path, RetentionPeriod}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.uploaddocuments.controllers.{BaseController, BaseControllerComponents, JourneyContextControllerHelper}
import uk.gov.hmrc.uploaddocuments.models.{FileToUpload, UploadedFile}
import uk.gov.hmrc.uploaddocuments.services.{FileUploadService, JourneyContextService}

import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UploadFileController @Inject() (
  components: BaseControllerComponents,
  val fileUploadService: FileUploadService,
  override val journeyContextService: JourneyContextService,
  objectStoreClient: PlayObjectStoreClient
)(implicit ec: ExecutionContext)
    extends BaseController(components) with JourneyContextControllerHelper {

  val log: Logger = play.api.Logger(this.getClass())

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
                  log.info(
                    s"[uploadFile] Call to upload file: '$journeyId', body: \n${Json.prettyPrint(Json.toJson(fileToUpload))}"
                  )
                  val objectStorePath = Path
                    .Directory(journeyContext.hostService.userAgent)
                    .file(UUID.randomUUID().toString + "_" + fileToUpload.name)
                  log.debug(s"[uploadFile] Uploading to object-store at ${objectStorePath.asUri}")
                  objectStoreClient
                    .putObject(
                      path = objectStorePath,
                      content = fileToUpload.content,
                      contentType = Some(fileToUpload.contentType),
                      contentMd5 = None
                    )
                    .transformWith {
                      case scala.util.Failure(exception) =>
                        log.error(s"Failure to store object because of $exception")
                        exception.printStackTrace()
                        Future.successful(BadRequest(s"Failure to store object because of $exception"))
                      case scala.util.Success(objectWithMD5) =>
                        objectStoreClient
                          .presignedDownloadUrl(path = objectStorePath)
                          .transformWith {
                            case scala.util.Failure(exception) =>
                              log.error(s"Failure to get pre-signed URL to $objectStorePath because of $exception")
                              exception.printStackTrace()
                              Future.successful(
                                BadRequest(s"Failure to get pre-signed URL to $objectStorePath because of $exception")
                              )
                            case scala.util.Success(presignedDownloadUrl) =>
                              val uploadedFile =
                                UploadedFile(
                                  upscanReference = objectStorePath.asUri,
                                  downloadUrl = presignedDownloadUrl.downloadUrl.toExternalForm(),
                                  uploadTimestamp = ZonedDateTime.now(),
                                  checksum = presignedDownloadUrl.contentMd5.value,
                                  fileName = fileToUpload.name,
                                  fileMimeType = fileToUpload.contentType,
                                  fileSize = presignedDownloadUrl.contentLength.toInt
                                )
                              Future.successful(
                                Created(Json.toJson(uploadedFile))
                              )
                          }
                    }
                case None =>
                  log.error(s"Failure, wrong payload:\n${request.body}")
                  Future.successful(BadRequest(s"Failure, wrong payload:\n${request.body}"))
              }
          }()
        }
      }
    }

}
