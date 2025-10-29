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
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.uploaddocuments.models.{UpscanInitiateRequest, UpscanInitiateResponse}
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.net.URI
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.libs.ws.JsonBodyWritables.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpResponse
import scala.util.{Failure, Success, Try}
import UpscanInitiateConnector.*
import org.apache.pekko.actor.ActorSystem

/** Connects to the upscan-initiate service API.
  */
@Singleton
class UpscanInitiateConnector @Inject() (
  appConfig: AppConfig,
  http: HttpClientV2,
  val kenshooRegistry: MetricRegistry,
  val actorSystem: ActorSystem
) extends HttpAPIMonitor with Retries {

  lazy val baseUrl: String = appConfig.upscanInitiateBaseUrl
  val upscanInitiatev2Path = "/upscan/v2/initiate"
  val userAgent            = "upload-customs-documents-frontend"

  def initiate(
    consumingService: String,
    request: UpscanInitiateRequest
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UpscanInitiateResponse] =
    retry(appConfig.fileUploadResultPushRetryIntervals*)(shouldRetry, errorMessage) {
      monitor(s"ConsumedAPI-upscan-v2-initiate-$consumingService-POST") {
        val url = new URL(baseUrl + upscanInitiatev2Path).toExternalForm
        val requestWithConsumingService: UpscanInitiateRequest =
          request.withConsumingService(consumingService)
        Logger.debug(
          s"[initiate] Making call to Upscan Initiate. Url '$url', body: \n${Json.prettyPrint(Json.toJson(requestWithConsumingService))}"
        )
        http
          .post(URI.create(url).toURL())
          .withBody(Json.toJson(requestWithConsumingService))
          .execute[HttpResponse]
          .flatMap { response =>
            response.status match {
              case x if x >= 200 && x < 300 => Future(Right(response.json.as[UpscanInitiateResponse]))
              case _                        => Future.successful(Left(response))
            }
          }
      }
    }.transform {
      case Success(Right(upscanInitiateResponse)) => Success(upscanInitiateResponse)
      case Success(Left(httpResponse))            => Failure(new Exception(httpResponse.body))
      case Failure(e)                             => Failure(e)
    }
}

object UpscanInitiateConnector {
  type Response = Either[HttpResponse, UpscanInitiateResponse]

  final val shouldRetry: Try[Response] => Boolean =
    _.fold(_ => false, _.left.exists(r => (r.status >= 500 && r.status < 600) || r.status == 499))

  final val errorMessage: Response => String = response =>
    s"Error ${response.left.map(r => s"status=${r.status} body=${r.body}").left.getOrElse("")}"
}
