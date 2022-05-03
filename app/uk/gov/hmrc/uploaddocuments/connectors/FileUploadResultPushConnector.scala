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

package uk.gov.hmrc.uploaddocuments.connectors

import akka.actor.ActorSystem
import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import play.api.libs.json.{Format, JsValue, Json, Writes}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.utils.LoggerUtil
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/** Connector to push the results of the file uploads back to the host service. */
@Singleton
class FileUploadResultPushConnector @Inject()(
  appConfig: AppConfig,
  http: HttpPost,
  metrics: Metrics,
  val actorSystem: ActorSystem
) extends HttpAPIMonitor with Retries with LoggerUtil {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  import FileUploadResultPushConnector._

  def push(request: Request)(implicit hc: HeaderCarrier, jid: JourneyId, ec: ExecutionContext): Future[Response] =
    retry(appConfig.fileUploadResultPushRetryIntervals: _*)(shouldRetry, errorMessage) {
      monitor(s"ConsumedAPI-push-file-uploads-${request.hostService.userAgent}-POST") {
        Try(new URL(request.url).toExternalForm).fold(
          e => {
            val msg = s"${e.getClass.getName} ${e.getMessage}"
            Logger.error(msg)
            Future.successful(Left(Error(0, msg)))
          },
          endpointUrl => {
            val wts = implicitly[Writes[FileUploadResultPushConnector.Payload]]
            val rds = implicitly[HttpReads[HttpResponse]]
            val ehc = request.hostService.populate(hc.withExtraHeaders("FileUploadJourney" -> jid.value))
            val payload = Payload(request, appConfig.baseExternalCallbackUrl)
            Logger.debug(s"[push] JourneyId: '${jid.value}' - sending notification to host service. Url: '$endpointUrl', Body: \n${Json.prettyPrint(Json.toJson(payload))}")
            http
              .POST[Payload, HttpResponse](endpointUrl, payload)(
                wts,
                rds,
                ehc,
                ec)
              .transformWith[Response] {
                case Success(response) =>
                  Logger.debug(s"[push] JourneyId: '${jid.value}' - Response from host: Status: '${response.status}', Body: '${response.body}'")
                  if (response.status == 204) Future.successful(SuccessResponse)
                  else {
                    val msg =
                      s"Failure pushing uploaded files to ${request.url}: ${response.body.take(1024)} ${request.hostService}"
                    Logger.error(msg)
                    Future.successful(Left(Error(response.status, msg)))
                  }
                case Failure(exception) =>
                  Logger.debug(s"[push] JourneyId: '${jid.value}' - Exception when handling the HttpResponse from the host service")
                  Logger.error(exception.getMessage)
                  Future.successful(Left(Error(0, exception.getMessage)))
              }
          }
        )
      }
    }

}

object FileUploadResultPushConnector {

  case class Request(
    url: String,
    nonce: Nonce,
    uploadedFiles: Seq[UploadedFile],
    context: Option[JsValue],
    hostService: HostService = HostService.Any
  )
  case class Payload(nonce: Nonce, uploadedFiles: Seq[UploadedFile], cargo: Option[JsValue])

  type Response = Either[FileUploadResultPushConnector.Error, Unit]

  val SuccessResponse: Response = Right[FileUploadResultPushConnector.Error, Unit](())

  case class Error(status: Int, message: String) {
    lazy val shouldRetry: Boolean = (status >= 500 && status < 600) || status == 499
  }

  object Request {
    def apply(context: FileUploadContext, fileUploads: FileUploads): Request =
      Request(
        context.config.callbackUrl,
        context.config.nonce,
        fileUploads.toUploadedFiles,
        context.config.cargo,
        context.hostService
      )

    implicit val format: Format[Request] = Json.format[Request]
  }

  object Payload {

    def apply(request: Request, baseUrl: String): Payload =
      Payload(
        request.nonce,
        request.uploadedFiles
          .map(file => file.copy(previewUrl = Some(baseUrl + filePreviewPathFor(file.upscanReference, file.fileName)))),
        request.context
      )

    private def filePreviewPathFor(reference: String, fileName: String): String =
      uk.gov.hmrc.uploaddocuments.controllers.routes.PreviewController
        .previewFileUploadByReference(reference, fileName)
        .url

    implicit val format: Format[Payload] = Json.format[Payload]
  }

  final val shouldRetry: Try[Response] => Boolean =
    _.fold(_ => false, _.left.exists(_.shouldRetry))

  final val errorMessage: Response => String = response =>
    s"Error ${response.left.map(e => s"status=${e.status} message=${e.message}").left.getOrElse("")}"

}
