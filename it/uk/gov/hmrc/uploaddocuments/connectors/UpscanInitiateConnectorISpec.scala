package uk.gov.hmrc.uploaddocuments.connectors

import play.api.Application
import uk.gov.hmrc.uploaddocuments.stubs.UpscanInitiateStubs
import uk.gov.hmrc.uploaddocuments.support.AppISpec
import uk.gov.hmrc.http._
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
    }
  }

}

trait UpscanInitiateConnectorISpecSetup extends AppISpec with UpscanInitiateStubs with DataStreamStubs {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def fakeApplication: Application = appBuilder.build()

  lazy val connector: UpscanInitiateConnector =
    app.injector.instanceOf[UpscanInitiateConnector]

}
