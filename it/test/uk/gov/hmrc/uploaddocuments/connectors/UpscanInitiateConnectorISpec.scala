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

package uk.gov.hmrc.uploaddocuments.connectors

import play.api.Application
import uk.gov.hmrc.uploaddocuments.stubs.UpscanInitiateStubs
import uk.gov.hmrc.uploaddocuments.support.AppISpec
import uk.gov.hmrc.http.*
import uk.gov.hmrc.uploaddocuments.models.{UpscanInitiateRequest, UpscanInitiateResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.uploaddocuments.stubs.DataStreamStubs

class UpscanInitiateConnectorISpec extends UpscanInitiateConnectorISpecSetup {

  "UpscanInitiateConnector" when {
    "/upscan/v2/initiate" should {
      "return upload request metadata" in {
        givenUpscanInitiateSucceeds("https://myservice.com/callback", "dummy.service")
        givenAuditConnector()

        val result: UpscanInitiateResponse =
          await(
            connector.initiate("dummy.service", UpscanInitiateRequest(callbackUrl = "https://myservice.com/callback"))
          )

        result.reference shouldBe "11370e18-6e24-453e-b45a-76d3e32ea33d"
        result.uploadRequest.href shouldBe testUploadRequest.href
        result.uploadRequest.fields.toSet should contain theSameElementsAs (testUploadRequest.fields.toSet)
      }

      "return error if upscan initiate fails" in {
        givenUpscanInitiateFails("https://myservice.com/callback", "dummy.service")
        givenAuditConnector()

        intercept[Exception] {
          await(
            connector.initiate("dummy.service", UpscanInitiateRequest(callbackUrl = "https://myservice.com/callback"))
          )
        }
      }

      "return error if upscan initiate returns malformed json" in {
        givenUpscanInitiateReturnsMalformedJson("https://myservice.com/callback", "dummy.service")
        givenAuditConnector()

        intercept[Exception] {
          await(
            connector.initiate("dummy.service", UpscanInitiateRequest(callbackUrl = "https://myservice.com/callback"))
          )
        }
      }
    }
  }

}

trait UpscanInitiateConnectorISpecSetup extends AppISpec with UpscanInitiateStubs with DataStreamStubs {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def fakeApplication(): Application = appBuilder.build()

  lazy val connector: UpscanInitiateConnector =
    app.injector.instanceOf[UpscanInitiateConnector]

}
