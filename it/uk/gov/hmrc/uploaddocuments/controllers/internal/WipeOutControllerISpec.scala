package uk.gov.hmrc.uploaddocuments.controllers.internal

import uk.gov.hmrc.uploaddocuments.controllers.ControllerISpecBase
import uk.gov.hmrc.uploaddocuments.journeys.State
import uk.gov.hmrc.uploaddocuments.models._

import scala.concurrent.ExecutionContext.Implicits.global

class WipeOutControllerISpec extends ControllerISpecBase {

  "WipeOutController" when {

    "POST /internal/wipe-out" should {
      "return 404 if wrong http method" in {
        val state = State.Initialized(
          FileUploadContext(
            fileUploadSessionConfig,
            HostService.Any
          ),
          FileUploads()
        )
        sessionStateService.setState(state)
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val result = await(backchannelRequest("/wipe-out").get())
        result.status shouldBe 404
        sessionStateService.getState shouldBe state
      }

      "return 204 and cleanup session state" in {
        val state = State.Initialized(
          FileUploadContext(
            fileUploadSessionConfig,
            HostService.Any
          ),
          FileUploads()
        )
        sessionStateService.setState(state)
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val result = await(backchannelRequest("/wipe-out").post(""))
        result.status shouldBe 204
        sessionStateService.getState shouldBe State.Uninitialized
      }
    }
  }
}
