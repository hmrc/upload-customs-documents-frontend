package uk.gov.hmrc.uploaddocuments.connectors

import play.api.Application
import play.api.libs.json.{JsNumber, JsString, Json}
import uk.gov.hmrc.http._
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.stubs.ExternalApiStubs
import uk.gov.hmrc.uploaddocuments.support.{AppISpec, TestData}

import java.time.ZonedDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{FiniteDuration, _}
import scala.util.Success

class FileUploadResultPushConnectorISpec extends FileUploadResultPushConnectorISpecSetup {

  override implicit val defaultTimeout: FiniteDuration = 10.seconds

  implicit val jid: JourneyId = TestData.journeyId

  import FileUploadResultPushConnector._

  "FileUploadResultPushConnector" when {
    "push" should {
      "retry when applicable" in {
        shouldRetry(Success(Right(()))) shouldBe false
        shouldRetry(Success(Left(Error(501, "")))) shouldBe true
        shouldRetry(Success(Left(Error(500, "")))) shouldBe true
        shouldRetry(Success(Left(Error(499, "")))) shouldBe true
        shouldRetry(Success(Left(Error(498, "")))) shouldBe false
        shouldRetry(Success(Left(Error(404, "")))) shouldBe false
        shouldRetry(Success(Left(Error(403, "")))) shouldBe false
        shouldRetry(Success(Left(Error(400, "")))) shouldBe false
      }

      val uploadedFile = UploadedFile(
        upscanReference = "jjSJKjksjJSJ",
        downloadUrl     = "https://aws.amzon.com/dummy.jpg",
        uploadTimestamp = ZonedDateTime.parse("2007-12-03T10:15:30+01:00"),
        checksum        = "akskakslaklskalkskalksl",
        fileName        = "dummy.jpg",
        fileMimeType    = "image/jpg",
        fileSize        = 1024
      )

      def request(url: String): Request =
        Request(
          url,
          Nonce(123),
          Seq(uploadedFile),
          Some(Json.obj("foo" -> Json.obj("bar" -> JsNumber(123), "url" -> JsString(url)))))

      "accept valid request and return success when response 204" in {
        val path = s"/dummy-host-endpoint"
        val url  = s"$wireMockBaseUrlAsString$path"
        givenResultPushEndpoint(path, Payload(request(url), "http://base.external.callback"), 204)
        val result: Response = await(connector.push(request(url)))
        result.isRight shouldBe true
        verifyResultPushHasHappened(path, 1)
      }

      "accept valid request and return an error without retrying if 3xx" in {
        Set(301, 302, 303, 307, 308).foreach { status =>
          val path = s"/dummy-host-endpoint-$status"
          val url  = s"$wireMockBaseUrlAsString$path"
          givenResultPushEndpoint(path, Payload(request(url), "http://base.external.callback"), status)
          val result: Response = await(connector.push(request(url)))
          result shouldBe Left(Error(0, "originalUrl"))
          verifyResultPushHasHappened(path, 1)
        }
      }
      "accept valid request and return an error without retrying" in {
        (200 to 498).filterNot(Set(204, 301, 302, 303, 307, 308)).map { status =>
          val path = s"/dummy-host-endpoint-$status"
          val url  = s"$wireMockBaseUrlAsString$path"
          givenResultPushEndpoint(path, Payload(request(url), "http://base.external.callback"), status)
          val result = await(connector.push(request(url)))
          result shouldBe Left(Error(status, s"Failure pushing uploaded files to $url:  HostService.Any"))
          verifyResultPushHasHappened(path, 1)
        }
      }

      "accept valid request and return an error after retrying" in {
        (499 to 599).map { status =>
          val path = s"/dummy-host-endpoint-$status"
          val url  = s"$wireMockBaseUrlAsString$path"
          givenResultPushEndpoint(path, Payload(request(url), "http://base.external.callback"), status)
          val result = await(connector.push(request(url)))
          result shouldBe Left(Error(status, s"Failure pushing uploaded files to $url:  HostService.Any"))
          verifyResultPushHasHappened(path, 3)
        }
      }
    }
  }

}

trait FileUploadResultPushConnectorISpecSetup extends AppISpec with ExternalApiStubs {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def fakeApplication: Application = appBuilder.build()

  lazy val connector: FileUploadResultPushConnector =
    app.injector.instanceOf[FileUploadResultPushConnector]

}
