package uk.gov.hmrc.uploaddocuments.controllers.internal

import play.api.libs.json.Json
import uk.gov.hmrc.uploaddocuments.controllers.ControllerISpecBase
import uk.gov.hmrc.uploaddocuments.models._
import play.api.libs.ws.writeableOf_JsValue
import play.api.libs.ws.writeableOf_String

import java.time.ZonedDateTime
import java.nio.charset.StandardCharsets
import uk.gov.hmrc.uploaddocuments.stubs.UpscanInitiateStubs
import uk.gov.hmrc.uploaddocuments.stubs.ExternalApiStubs
import play.api.http.HeaderNames
import play.api.http.MimeTypes
import play.api.libs.json.JsString
import play.api.libs.json.JsNumber

class UploadFileControllerISpec extends ControllerISpecBase with UpscanInitiateStubs with ExternalApiStubs {

  "UploadFileController" when {

    "POST /internal/upload" should {
      "return 404 if wrong http method" in {
        setContext()

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val result = await(backchannelRequest("/upload").get())
        result.status shouldBe 404
      }

      "return 400 if malformed payload" in {
        setContext()

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val result = await(backchannelRequest("/upload").post(""))
        result.status shouldBe 400
      }

      "return 400 if cannot accept payload" in {
        setContext()

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val result = await(
          backchannelRequest("/upload")
            .post(
              Json.toJson(
                UploadedFile(
                  upscanReference = "jjSJKjksjJSJ",
                  downloadUrl = "https://aws.amzon.com/dummy.jpg",
                  uploadTimestamp = ZonedDateTime.parse("2007-12-03T10:15:30+01:00"),
                  checksum = "akskakslaklskalkskalksl",
                  fileName = "dummy.jpg",
                  fileMimeType = "image/jpg",
                  fileSize = 1024
                )
              )
            )
        )
        result.status shouldBe 400
      }

      "return 201 with uploaded file" in {
        setContext()
        setFileUploads()

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val callbackUrl =
          appConfig.baseInternalCallbackUrl + s"/internal/upload/callback-from-upscan/journey/$getJourneyId"

        val upscanUrl = stubForFileUpload(201, "test.txt")

        givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent, upscanUrl)

        val uploadCall = backchannelRequest("/upload")
          .post(Json.toJson(FileToUpload("a", "b", "c", "Hello!".getBytes(StandardCharsets.UTF_8))))

        Thread.sleep(2000)

        val nonce = getFileUploads()
          .flatMap(_.files.headOption)
          .map(_.nonce)
          .getOrElse(fail("Cannot find posted file"))

        await(
          backchannelRequestWithoutSessionId(
            s"/upload/callback-from-upscan/journey/$getJourneyId/$nonce"
          ).withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
            .post(
              Json.obj(
                "reference"   -> JsString("11370e18-6e24-453e-b45a-76d3e32ea33d"),
                "fileStatus"  -> JsString("READY"),
                "downloadUrl" -> JsString("https://foo.bar/XYZ123/test.txt"),
                "uploadDetails" -> Json.obj(
                  "uploadTimestamp" -> JsString("2018-04-24T09:30:00Z"),
                  "checksum"        -> JsString("396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100"),
                  "fileName"        -> JsString("test.txt"),
                  "fileMimeType"    -> JsString("text/plain"),
                  "size"            -> JsNumber(23)
                )
              )
            )
        )

        val result = await(uploadCall)
        result.status shouldBe 201
      }

      "return 400 when file upload fails" in {
        setContext()
        setFileUploads()

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val callbackUrl =
          appConfig.baseInternalCallbackUrl + s"/internal/upload/callback-from-upscan/journey/$getJourneyId"

        val upscanUrl = stubForFileUpload(400, "test.txt")

        givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent, upscanUrl)

        val uploadCall = backchannelRequest("/upload")
          .post(Json.toJson(FileToUpload("a", "b", "c", "Hello!".getBytes(StandardCharsets.UTF_8))))

        val result = await(uploadCall)
        result.status shouldBe 400
      }

      "return 400 when upscan initialization fails" in {
        setContext()
        setFileUploads()

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val callbackUrl =
          appConfig.baseInternalCallbackUrl + s"/internal/upload/callback-from-upscan/journey/$getJourneyId"

        givenUpscanInitiateFails(callbackUrl, hostUserAgent)

        val uploadCall = backchannelRequest("/upload")
          .post(Json.toJson(FileToUpload("a", "b", "c", "Hello!".getBytes(StandardCharsets.UTF_8))))

        val result = await(uploadCall)
        result.status shouldBe 400
      }

      "return 404 when file verification rejected" in {
        setContext()
        setFileUploads()

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val callbackUrl =
          appConfig.baseInternalCallbackUrl + s"/internal/upload/callback-from-upscan/journey/$getJourneyId"

        val upscanUrl = stubForFileUpload(201, "test.txt")

        givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent, upscanUrl)

        val uploadCall = backchannelRequest("/upload")
          .post(Json.toJson(FileToUpload("a", "b", "c", "Hello!".getBytes(StandardCharsets.UTF_8))))

        Thread.sleep(2000)

        val nonce = getFileUploads()
          .flatMap(_.files.headOption)
          .map(_.nonce)
          .getOrElse(fail("Cannot find posted file"))

        await(
          backchannelRequestWithoutSessionId(
            s"/upload/callback-from-upscan/journey/$getJourneyId/$nonce"
          ).withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
            .post(
              Json.obj(
                "reference"  -> JsString("11370e18-6e24-453e-b45a-76d3e32ea33d"),
                "fileStatus" -> JsString("FAILED"),
                "failureDetails" -> Json.obj(
                  "failureReason" -> JsString("QUARANTINE"),
                  "message"       -> JsString("e.g. This file has a virus")
                )
              )
            )
        )

        val result = await(uploadCall)
        result.status shouldBe 400
      }

    }
  }
}
