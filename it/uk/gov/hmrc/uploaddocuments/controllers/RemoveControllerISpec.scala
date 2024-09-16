package uk.gov.hmrc.uploaddocuments.controllers

import uk.gov.hmrc.uploaddocuments.models.fileUploadResultPush._
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.stubs.ExternalApiStubs
import play.api.libs.ws.DefaultBodyReadables.readableAsString
import play.api.libs.ws.writeableOf_String

import java.time.ZonedDateTime

class RemoveControllerISpec extends ControllerISpecBase with ExternalApiStubs {

  val fileUploadNotDeleted: FileUpload.Accepted = FileUpload.Accepted(
    Nonce.Any,
    Timestamp.Any,
    "22370e18-6e24-453e-b45a-76d3e32ea33d",
    "https://s3.amazonaws.com/bucket/123",
    ZonedDateTime.parse("2018-04-24T09:30:00Z"),
    "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
    "test1.png",
    "image/png",
    4567890
  )
  val fileUploadToBeDeleted: FileUpload.Accepted = FileUpload.Accepted(
    Nonce.Any,
    Timestamp.Any,
    "11370e18-6e24-453e-b45a-76d3e32ea33d",
    "https://s3.amazonaws.com/bucket/123",
    ZonedDateTime.parse("2018-04-24T09:30:00Z"),
    "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
    "test2.pdf",
    "application/pdf",
    5234567
  )

  "RemoveController" when {

    "GET /uploaded/:reference/remove" should {

      "remove file from upload list by reference" in {

        givenResultPushEndpoint(
          "/result-post-url",
          Payload(
            Request(FileUploadContext(fileUploadSessionConfig), FileUploads(files = Seq(fileUploadNotDeleted))),
            "http://base.external.callback"
          ),
          204
        )

        setContext()
        setFileUploads(FileUploads(files = Seq(fileUploadToBeDeleted, fileUploadNotDeleted)))

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result = await(request(s"/uploaded/${fileUploadToBeDeleted.reference}/remove").get())

        result.status shouldBe 200
        result.body should include(htmlEscapedPageTitle("view.summary.singular.title", "1"))
        result.body should include(htmlEscapedMessage("view.summary.singular.heading", "1"))

        getFileUploads() shouldBe Some(FileUploads(files = Seq(fileUploadNotDeleted)))

        eventually(verifyResultPushHasHappened("/result-post-url", 1))
      }
    }

    "POST /uploaded/:reference/remove" should {

      "remove file from upload list by reference" in {

        givenResultPushEndpoint(
          "/result-post-url",
          Payload(
            Request(FileUploadContext(fileUploadSessionConfig), FileUploads(files = Seq(fileUploadNotDeleted))),
            "http://base.external.callback"
          ),
          204
        )

        setContext()
        setFileUploads(FileUploads(files = Seq(fileUploadToBeDeleted, fileUploadNotDeleted)))

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result = await(request(s"/uploaded/${fileUploadToBeDeleted.reference}/remove").post(""))

        result.status shouldBe 204

        getFileUploads() shouldBe Some(FileUploads(files = Seq(fileUploadNotDeleted)))

        eventually(verifyResultPushHasHappened("/result-post-url", 1))
      }
    }
  }
}
