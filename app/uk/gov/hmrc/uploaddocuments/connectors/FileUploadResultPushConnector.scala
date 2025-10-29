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

package uk.gov.hmrc.uploaddocuments.connectors

import com.codahale.metrics.MetricRegistry
import org.apache.pekko.actor.ActorSystem
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.uploaddocuments.connectors.httpParsers.FileUploadResultPushConnectorReads
import uk.gov.hmrc.uploaddocuments.models.*
import uk.gov.hmrc.uploaddocuments.models.fileUploadResultPush.{Error, Payload, Request}
import uk.gov.hmrc.uploaddocuments.utils.LoggerUtil
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

import java.net.{URI, URL}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class FileUploadResultPushConnector @Inject() (
  appConfig: AppConfig,
  http: HttpClientV2,
  val kenshooRegistry: MetricRegistry,
  val actorSystem: ActorSystem
) extends HttpAPIMonitor with Retries with LoggerUtil {

  import FileUploadResultPushConnector.*

  def push(request: Request)(implicit hc: HeaderCarrier, jid: JourneyId, ec: ExecutionContext): Future[Response] = {
    val responseReads = new FileUploadResultPushConnectorReads(request.hostService)
    retry(appConfig.fileUploadResultPushRetryIntervals: _*)(shouldRetry, errorMessage) {
      monitor(s"ConsumedAPI-push-file-uploads-${request.hostService.userAgent}-POST") {
        withUrl(request) { endpointUrl =>
          val payload = Payload(request, appConfig.baseExternalCallbackUrl)
          Logger
            .debug(
              s"[push] JourneyId: '${jid.value}' - sending notification to host service. Url: '$endpointUrl', Body: \n${Json
                  .prettyPrint(Json.toJson(payload)(Payload.writeNoDownloadUrl))}"
            )
          http
            .post(URI.create(endpointUrl).toURL())(
              request.hostService.populate(hc).withExtraHeaders("FileUploadJourney" -> jid.value)
            )
            .withBody(Json.toJson(payload))
            .execute[HttpResponse]
            .flatMap { response =>
              Future(responseReads.read("POST", endpointUrl, response))
            }
            .recover { case exception =>
              Logger.debug(
                s"[push] JourneyId: '${jid.value}' - Exception when handling the HttpResponse from the host service"
              )
              Logger.error(exception.getMessage)
              Left(Error(0, exception.getMessage))
            }
        }
      }
    }
  }

  private def withUrl(request: Request)(f: String => Future[Response]): Future[Response] =
    Try(new URL(request.url).toExternalForm).fold(
      e => {
        val msg = s"${e.getClass.getName} ${e.getMessage}"
        Logger.error(msg)
        Future.successful(Left(Error(0, msg)))
      },
      f
    )

}

object FileUploadResultPushConnector {

  type Response = Either[Error, Unit]

  val SuccessResponse: Response = Right[Error, Unit](())

  final val shouldRetry: Try[Response] => Boolean = _.fold(_ => false, _.left.exists(_.shouldRetry))

  final val errorMessage: Response => String = response =>
    s"Error ${response.left.map(e => s"status=${e.status} message=${e.message}").left.getOrElse("")}"

}
