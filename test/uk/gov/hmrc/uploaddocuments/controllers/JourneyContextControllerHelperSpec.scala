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
import uk.gov.hmrc.uploaddocuments.services.JourneyContextService
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.uploaddocuments.models.JourneyId
import scala.concurrent.Future
import uk.gov.hmrc.uploaddocuments.models.FileUploadContext
import uk.gov.hmrc.mongo.cache.CacheItem
import play.api.mvc.Results
import uk.gov.hmrc.uploaddocuments.models.FileUploadSessionConfig
import uk.gov.hmrc.uploaddocuments.models.Nonce
import uk.gov.hmrc.uploaddocuments.models.CustomizedServiceContent

class JourneyContextControllerHelperSpec extends UnitSpec {

  trait Fixture {

    val journeyContextResponse: Option[FileUploadContext]

    given journeyId: JourneyId = JourneyId("dummy-journey-id")

    final val underTest = new JourneyContextControllerHelper {

      override def govukStartUrl: String = "dummy-gov-uk-start-url"

      override val journeyContextService: JourneyContextService =
        new JourneyContextService(null) { // we don't need the repository here as we are mocking the service
          override def getJourneyContext()(implicit journeyId: JourneyId): Future[Option[FileUploadContext]] =
            Future.successful(journeyContextResponse)

          override def putJourneyContext(journeyContext: FileUploadContext)(implicit
            journeyId: JourneyId
          ): Future[CacheItem] =
            ??? // we don't use this in the test
        }
    }
  }

  "withJourneyContext" should {
    "return a redirect to the start url if the journey context is not found" in new Fixture {

      val journeyContextResponse = None

      val result = underTest
        .withJourneyContext(fileUploadContext => Future.successful(Results.Redirect("dummy-redirect-url")))

      await(result.header.headers("Location")) shouldBe "dummy-gov-uk-start-url"

    }

    "return a redirect to the sendoff url if the journey context is found and is active" in new Fixture {

      val journeyContextResponse =
        Some(
          FileUploadContext(
            FileUploadSessionConfig(
              nonce = Nonce.Any,
              continueUrl = "dummy-continue-url",
              callbackUrl = "dummy-callback-url",
              sendoffUrl = Some("dummy-sendoff-url")
            )
          )
        )

      val result = underTest
        .withJourneyContext(fileUploadContext => Future.successful(Results.Redirect("dummy-redirect-url")))

      await(result.header.headers("Location")) shouldBe "dummy-redirect-url"
    }

    "return a redirect to the sendoff url if the journey context is found and is not active" in new Fixture {

      val journeyContextResponse =
        Some(
          FileUploadContext(
            FileUploadSessionConfig(
              nonce = Nonce.Any,
              continueUrl = "dummy-continue-url",
              callbackUrl = "dummy-callback-url",
              sendoffUrl = Some("dummy-sendoff-url")
            )
          ).deactivate()
        )

      val result = underTest
        .withJourneyContext(fileUploadContext => Future.successful(Results.Redirect("dummy-redirect-url")))

      await(result.header.headers("Location")) shouldBe "dummy-sendoff-url"
    }

    "return a redirect to the serviceUrl url if the journey context is found and is not active" in new Fixture {

      val journeyContextResponse =
        Some(
          FileUploadContext(
            FileUploadSessionConfig(
              nonce = Nonce.Any,
              continueUrl = "dummy-continue-url",
              callbackUrl = "dummy-callback-url",
              content = CustomizedServiceContent(
                serviceUrl = Some("dummy-service-url")
              )
            )
          ).deactivate()
        )

      val result = underTest
        .withJourneyContext(fileUploadContext => Future.successful(Results.Redirect("dummy-redirect-url")))

      await(result.header.headers("Location")) shouldBe "dummy-service-url"
    }

    "return a redirect to the gov.uk start url if the journey context is found and is not active and no sendoff url or service url is set" in new Fixture {

      val journeyContextResponse =
        Some(
          FileUploadContext(
            FileUploadSessionConfig(
              nonce = Nonce.Any,
              continueUrl = "dummy-continue-url",
              callbackUrl = "dummy-callback-url"
            )
          ).deactivate()
        )

      val result = underTest
        .withJourneyContext(fileUploadContext => Future.successful(Results.Redirect("dummy-redirect-url")))

      await(result.header.headers("Location")) shouldBe "dummy-gov-uk-start-url"
    }
  }

}
