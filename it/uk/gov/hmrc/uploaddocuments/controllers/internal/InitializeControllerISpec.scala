package uk.gov.hmrc.uploaddocuments.controllers.internal

import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.uploaddocuments.controllers.ControllerISpecBase
import uk.gov.hmrc.uploaddocuments.journeys.State
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.repository.NewJourneyCacheRepository.DataKeys

import java.time.ZonedDateTime
import scala.concurrent.ExecutionContext.Implicits.global

class InitializeControllerISpec extends ControllerISpecBase {

  "InitializeController" when {

    "POST /internal/initialize" should {
      "return 404 if wrong http method" in {
        sessionStateService.setState(State.Uninitialized)
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val result = await(backchannelRequest("/initialize").get())
        result.status shouldBe 404
        sessionStateService.getState shouldBe State.Uninitialized
      }

      "return 400 if malformed payload" in {
        sessionStateService.setState(State.Uninitialized)
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val result = await(backchannelRequest("/initialize").post(""))
        result.status shouldBe 400
        sessionStateService.getState shouldBe State.Uninitialized
      }

      "return 400 if cannot accept payload" in {
        sessionStateService.setState(State.Uninitialized)
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
        sessionStateService.getState shouldBe State.Uninitialized
      }

      "register config and empty file uploads" in {
        sessionStateService.setState(State.Uninitialized)
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val result = await(
          backchannelRequest("/initialize")
            .post(Json.toJson(FileUploadInitializationRequest(fileUploadSessionConfig, Seq.empty)))
        )
        result.status shouldBe 201
        sessionStateService.getState shouldBe State.Initialized(
          FileUploadContext(
            fileUploadSessionConfig,
            HostService.Any
          ),
          FileUploads()
        )

        await(
          newJourneyRepo.get(sessionStateService.getJourneyId(hc).get)(DataKeys.journeyConfigDataKey)
        ) shouldBe Some(
          FileUploadContext(
            fileUploadSessionConfig,
            HostService.Any
          )
        )

        await(newJourneyRepo.get(sessionStateService.getJourneyId(hc).get)(DataKeys.uploadedFiles)) shouldBe Some(
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
        sessionStateService.setState(State.Uninitialized)
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
        sessionStateService.getState shouldBe State.Initialized(
          FileUploadContext(
            fileUploadSessionConfig,
            HostService.Any
          ),
          FileUploads(preexistingUploads.map(_.toFileUpload))
        )

        await(
          newJourneyRepo.get(sessionStateService.getJourneyId(hc).get)(DataKeys.journeyConfigDataKey)
        ) shouldBe Some(
          FileUploadContext(
            fileUploadSessionConfig,
            HostService.Any
          )
        )

        await(newJourneyRepo.get(sessionStateService.getJourneyId(hc).get)(DataKeys.uploadedFiles)) shouldBe Some(
          FileUploads(preexistingUploads.map(_.toFileUpload))
        )
      }
    }

  }
}
