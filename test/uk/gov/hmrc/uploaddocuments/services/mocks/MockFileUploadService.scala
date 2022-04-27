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

package uk.gov.hmrc.uploaddocuments.services

import org.scalamock.handlers.{CallHandler1, CallHandler2, CallHandler3}
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.mongo.cache.CacheItem
import uk.gov.hmrc.uploaddocuments.models.FileUploads

import scala.concurrent.Future

trait MockFileUploadService extends MockFactory {

  val mockFileUploadService = mock[FileUploadService]

  def mockGetFiles(journeyId: String)(response: Future[Option[FileUploads]]): CallHandler1[String, Future[Option[FileUploads]]] =
    (mockFileUploadService.getFiles(_: String)).expects(journeyId).returning(response)

  def mockPutFiles(journeyId: String, request: FileUploads)(response: Future[CacheItem]): CallHandler2[FileUploads, String, Future[CacheItem]] =
    (mockFileUploadService.putFiles(_: FileUploads)(_: String)).expects(request, journeyId).returning(response)

  def mockWithFiles[T](journeyId: String)(files: Option[FileUploads]): CallHandler3[Future[T], FileUploads => Future[T], String, Future[T]] = {
    (mockFileUploadService.withFiles[T](_: Future[T])(_: FileUploads => Future[T])(_: String))
      .expects(*, *, journeyId)
      .onCall { mock =>
        files match {
          case Some(value) =>
            mock.productElement(1).asInstanceOf[FileUploads => Future[T]](value)
          case None =>
            mock.productElement(0).asInstanceOf[() => Future[T]]()
        }
      }
  }

}