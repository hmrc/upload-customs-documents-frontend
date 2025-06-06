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
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.uploaddocuments.models.{UpscanInitiateRequest, UpscanInitiateResponse}
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.net.URI

/** Connects to the upscan-initiate service API.
  */
@Singleton
class UpscanInitiateConnector @Inject() (
  appConfig: AppConfig,
  http: HttpGet with HttpPost,
  val kenshooRegistry: MetricRegistry
) extends HttpAPIMonitor {

  lazy val baseUrl: String = appConfig.upscanInitiateBaseUrl
  val upscanInitiatev2Path = "/upscan/v2/initiate"
  val userAgent            = "upload-customs-documents-frontend"

  def initiate(
    consumingService: String,
    request: UpscanInitiateRequest
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UpscanInitiateResponse] =
    monitor(s"ConsumedAPI-upscan-v2-initiate-$consumingService-POST") {
      val url = new URL(baseUrl + upscanInitiatev2Path).toExternalForm
      val requestWithConsumingService: UpscanInitiateRequest =
        request.withConsumingService(consumingService)
      Logger.debug(
        s"[initiate] Making call to Upscan Initiate. Url '$url', body: \n${Json.prettyPrint(Json.toJson(requestWithConsumingService))}"
      )
      http
        .POST[UpscanInitiateRequest, UpscanInitiateResponse](URI.create(url).toURL(), requestWithConsumingService)
        .recoverWith { case e: Throwable =>
          Future.failed(UpscanInitiateError(e))
        }
    }
}

case class UpscanInitiateError(e: Throwable) extends RuntimeException(e)
