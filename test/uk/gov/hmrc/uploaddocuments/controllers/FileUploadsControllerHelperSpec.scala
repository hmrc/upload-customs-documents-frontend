/*
 * Copyright 2025 HM Revenue & Customs
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

import uk.gov.hmrc.uploaddocuments.support.UnitSpec
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.uploaddocuments.models.JourneyId
import scala.concurrent.Future
import uk.gov.hmrc.uploaddocuments.services.FileUploadService
import uk.gov.hmrc.uploaddocuments.models.FileUploads
import play.api.mvc.Results

class FileUploadsControllerHelperSpec extends UnitSpec {

  trait Fixture {

    val fileUploadsResponse: Option[FileUploads]

    given journeyId: JourneyId = JourneyId("dummy-journey-id")

    final val underTest = new FileUploadsControllerHelper {
      override val govukStartUrl: String = "dummy-gov-uk-start-url"
      override val fileUploadService: FileUploadService = new FileUploadService(null, null, null, null, null) {
        override def getFiles(implicit journeyId: JourneyId): Future[Option[FileUploads]] =
          Future.successful(fileUploadsResponse)
      }
    }
  }

  "withJourneyContext" should {
    "return a redirect to the start url if file uploads are not found" in new Fixture {

      val fileUploadsResponse = None

      val result = underTest.withFileUploads(fileUploads => Future.successful(Results.Redirect("dummy-redirect-url")))

      await(result).header.headers("Location") shouldBe "dummy-gov-uk-start-url"
    }

    "return a redirect to the redirect url if file uploads are found" in new Fixture {
      val fileUploadsResponse = Some(FileUploads())

      val result = underTest.withFileUploads(fileUploads => Future.successful(Results.Redirect("dummy-redirect-url")))

      await(result).header.headers("Location") shouldBe "dummy-redirect-url"
    }

  }

}
