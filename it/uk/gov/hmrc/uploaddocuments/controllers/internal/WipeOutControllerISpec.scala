package uk.gov.hmrc.uploaddocuments.controllers.internal

import uk.gov.hmrc.uploaddocuments.controllers.ControllerISpecBase
import uk.gov.hmrc.uploaddocuments.models._

class WipeOutControllerISpec extends ControllerISpecBase {

  "WipeOutController" when {

    "POST /internal/wipe-out" should {

      "return 404 if wrong http method" in {

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val result = await(backchannelRequest("/wipe-out").get())
        result.status shouldBe 404
      }

      "return 204 and cleanup session state" in {

        val context = FileUploadContext(fileUploadSessionConfig)
        val fileUploads = FileUploads(files =
          Seq(
            FileUpload.Initiated(Nonce.Any, Timestamp.Any, "11370e18-6e24-453e-b45a-76d3e32ea33d")
          )
        )

        setContext(context)
        setFileUploads(fileUploads)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val result = await(backchannelRequest("/wipe-out").post(""))
        result.status shouldBe 204

        getContext() shouldBe None
        getFileUploads() shouldBe None
      }
    }
  }
}
