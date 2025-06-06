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

package uk.gov.hmrc.uploaddocuments.services.mocks

import org.scalamock.handlers.{CallHandler1, CallHandler2, CallHandler3}
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.uploaddocuments.models.{FileUploads, JourneyId}
import uk.gov.hmrc.uploaddocuments.services.FileUploadService

import scala.concurrent.{ExecutionContext, Future}

trait MockFileUploadService {
  this: MockFactory =>

  lazy val mockFileUploadService = mock[FileUploadService]

  def mockGetFiles(journeyId: JourneyId)(
    response: => Future[Option[FileUploads]]
  ): CallHandler1[JourneyId, Future[Option[FileUploads]]] =
    (mockFileUploadService.getFiles(_: JourneyId)).expects(journeyId).returning(response)

  def mockPutFiles(journeyId: JourneyId, request: FileUploads)(
    response: => Future[FileUploads]
  ): CallHandler2[FileUploads, JourneyId, Future[FileUploads]] =
    (mockFileUploadService.putFiles(_: FileUploads)(_: JourneyId)).expects(request, journeyId).returning(response)

  def mockWithFiles[T](journeyId: JourneyId)(
    files: => Future[Option[FileUploads]]
  )(implicit ec: ExecutionContext): CallHandler3[Future[T], FileUploads => Future[T], JourneyId, Future[T]] =
    (mockFileUploadService
      .withFiles[T](_: Future[T])(_: FileUploads => Future[T])(_: JourneyId))
      .expects(*, *, journeyId)
      .onCall { mock =>
        files.flatMap {
          case Some(value) =>
            mock.productElement(1).asInstanceOf[FileUploads => Future[T]](value)
          case None =>
            mock.productElement(0).asInstanceOf[() => Future[T]]()
        }
      }

}
