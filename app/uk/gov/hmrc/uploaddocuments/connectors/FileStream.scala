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

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse}
import org.apache.pekko.stream.scaladsl.{Flow, Source}
import play.api.mvc.{Result, Results}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait FileStream {

  val defaultFileSizeLimit: Long = 100 * 1024 * 1024

  implicit val actorSystem: ActorSystem

  private val connectionPool: Flow[(HttpRequest, String), (Try[HttpResponse], String), NotUsed] =
    Http().superPool[String]()

  final def getFileStream(
    url: String,
    fileName: String,
    fileMimeType: String,
    fileSize: Long,
    contentDispositionForMimeType: (String, String) => (String, String)
  ): Future[Result] = {
    val httpRequest = HttpRequest(method = HttpMethods.GET, uri = url)
    fileStream(httpRequest, fileName, fileMimeType, fileSize, contentDispositionForMimeType)
  }

  final def fileStream(
    httpRequest: HttpRequest,
    fileName: String,
    fileMimeType: String,
    fileSize: Long,
    contentDispositionForMimeType: (String, String) => (String, String)
  ): Future[Result] =
    Source
      .single((httpRequest, httpRequest.uri.toString()))
      .via(connectionPool)
      .runFold[Result](Results.Ok) {
        case (_, (Success(httpResponse), _)) if httpResponse.status.isSuccess() =>
          Results.Ok
            .streamed(
              content =
                httpResponse.entity.withSizeLimit(if (fileSize == 0) defaultFileSizeLimit else fileSize).dataBytes,
              contentLength = httpResponse.entity.contentLengthOption,
              contentType = Some(fileMimeType)
            )
            .withHeaders(contentDispositionForMimeType(fileName, fileMimeType))

        case (_, (Success(httpResponse), url)) =>
          throw new Exception(s"Error status ${httpResponse.status} when accessing $url")

        case (_, (Failure(error), url)) =>
          throw new Exception(s"Error when accessing $url: ${error.getMessage}.")
      }

}
