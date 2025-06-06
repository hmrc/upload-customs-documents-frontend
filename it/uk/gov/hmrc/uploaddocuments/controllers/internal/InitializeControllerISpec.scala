package uk.gov.hmrc.uploaddocuments.controllers.internal

import play.api.http.HeaderNames
import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.uploaddocuments.controllers.ControllerISpecBase
import uk.gov.hmrc.uploaddocuments.models._
import play.api.libs.ws.writeableOf_JsValue
import play.api.libs.ws.writeableOf_String

import java.time.ZonedDateTime
import uk.gov.hmrc.uploaddocuments.controllers.{routes => mainRoutes}

class InitializeControllerISpec extends ControllerISpecBase {

  "InitializeController" when {

    "POST /internal/initialize" should {
      "return 404 if wrong http method" in {

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val result = await(backchannelRequest("/initialize").get())
        result.status shouldBe 404
      }

      "return 400 if malformed payload" in {

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val result = await(backchannelRequest("/initialize").post(""))
        result.status shouldBe 400
      }

      "return 400 if cannot accept payload" in {

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val result = await(
          backchannelRequest("/initialize")
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

      "register config and empty file uploads" in {

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val result = await(
          backchannelRequest("/initialize")
            .post(Json.toJson(FileUploadInitializationRequest(fileUploadSessionConfig, Seq.empty)))
        )
        result.status shouldBe 201

        getContext() shouldBe Some(
          FileUploadContext(
            fileUploadSessionConfig,
            HostService.Any
          )
        )

        getFileUploads() shouldBe Some(
          FileUploads()
        )
      }

      "when feature for multi file is disabled still redirect to the start page" in {

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val journeyContext = fileUploadSessionConfig.copy(features = Features(showUploadMultiple = false))

        val result = await(
          backchannelRequest("/initialize")
            .post(Json.toJson(FileUploadInitializationRequest(journeyContext, Seq.empty)))
        )
        result.status shouldBe 201
        result.header(HeaderNames.LOCATION) shouldBe Some(mainRoutes.StartController.start.url)

        getContext() shouldBe Some(
          FileUploadContext(
            journeyContext,
            HostService.Any
          )
        )

        getFileUploads() shouldBe Some(
          FileUploads()
        )
      }

      "register config and pre-existing file uploads" in {
        val preexistingUploads = Seq(
          UploadedFile(
            upscanReference = "jjSJKjksjJSJ",
            downloadUrl = "https://aws.amzon.com/dummy.jpg",
            uploadTimestamp = ZonedDateTime.parse("2007-12-03T10:15:30+01:00"),
            checksum = "akskakslaklskalkskalksl",
            fileName = "dummy.jpg",
            fileMimeType = "image/jpg",
            fileSize = 1024,
            cargo = Some(Json.obj("foo" -> JsString("bar")))
          )
        )

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result = await(
          backchannelRequest("/initialize")
            .post(
              Json.toJson(
                FileUploadInitializationRequest(
                  fileUploadSessionConfig,
                  preexistingUploads
                )
              )
            )
        )
        result.status shouldBe 201

        getContext() shouldBe Some(
          FileUploadContext(
            fileUploadSessionConfig,
            HostService.Any
          )
        )

        getFileUploads() shouldBe Some(
          FileUploads(preexistingUploads.map(FileUpload.apply))
        )
      }
    }

  }
}
