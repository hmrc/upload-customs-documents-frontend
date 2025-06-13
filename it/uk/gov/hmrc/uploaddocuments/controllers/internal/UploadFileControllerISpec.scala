package uk.gov.hmrc.uploaddocuments.controllers.internal

import play.api.libs.json.Json
import play.api.libs.ws.{writeableOf_JsValue, writeableOf_String}
import uk.gov.hmrc.uploaddocuments.controllers.ControllerISpecBase
import uk.gov.hmrc.uploaddocuments.models.*
import uk.gov.hmrc.uploaddocuments.stubs.{ExternalApiStubs, ObjectStoreStubs, UpscanInitiateStubs}

import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import play.api.libs.json.JsValue
import play.api.libs.ws.readableAsJson

class UploadFileControllerISpec
    extends ControllerISpecBase with UpscanInitiateStubs with ExternalApiStubs with ObjectStoreStubs {

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
        givenObjectStorePresignedUrl(url = "http://foo.bar/test-123.txt", size = 363, md5Hash = "fooMD5")

        val uploadCall = backchannelRequest("/upload")
          .post(Json.toJson(FileToUpload("a", "b", "c", "Hello!".getBytes(StandardCharsets.UTF_8))))

        val result = await(uploadCall)
        result.status shouldBe 201
        val responseBody = result.body[JsValue]
        (responseBody \ "downloadUrl").asOpt[String] shouldBe Some("http://foo.bar/test-123.txt")
        (responseBody \ "fileName").asOpt[String] shouldBe Some("b")
        (responseBody \ "fileSize").asOpt[Int] shouldBe Some(363)
        (responseBody \ "checksum").asOpt[String] shouldBe Some(
          "334d016f755cd6dc58c53a86e183882f8ec14f52fb05345887c8a5edd42c87b7"
        )

      }

    }
  }
}
