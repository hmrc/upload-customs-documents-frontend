package uk.gov.hmrc.uploaddocuments.controllers

import play.api.http.Status
import uk.gov.hmrc.uploaddocuments.models._
import play.api.libs.ws.DefaultBodyReadables.readableAsString

import java.time.ZonedDateTime
import uk.gov.hmrc.uploaddocuments.stubs.UpscanInitiateStubs

class FileVerificationControllerISpec extends ControllerISpecBase with UpscanInitiateStubs {

  val upscanRef1 = "11370e18-6e24-453e-b45a-76d3e32ea33d"
  val upscanRef2 = "2b72fe99-8adf-4edb-865e-622ae710f77c"

  "FileVerificationController" when {

    "GET /file-verification" should {
      "display waiting for file verification page after the timeout the first time" in {

        val context = FileUploadContext(fileUploadSessionConfig)
        val fileUploads = FileUploads(files =
          Seq(
            FileUpload.Initiated(Nonce.Any, Timestamp.Any, upscanRef1),
            FileUpload.Posted(Nonce.Any, Timestamp.Any, upscanRef2)
          )
        )

        setContext(context)
        setFileUploads(fileUploads)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result = await(request(s"/file-verification?key=$upscanRef2").get())

        result.status shouldBe 200
        result.body should include(htmlEscapedPageTitle("view.upload-file.waiting"))
        result.body should include(htmlEscapedMessage("view.upload-file.waiting"))

        getFileUploads() shouldBe Some(fileUploads)
      }

      "display waiting for file verification page after the timeout the next time" in {

        val context = FileUploadContext(fileUploadSessionConfig)
        val fileUploads = FileUploads(files =
          Seq(
            FileUpload.Initiated(Nonce.Any, Timestamp.Any, upscanRef1),
            FileUpload.Posted(Nonce.Any, Timestamp.Any, upscanRef2)
          )
        )

        setContext(context)
        setFileUploads(fileUploads)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result = await(request(s"/file-verification?key=$upscanRef2").get())

        result.status shouldBe 200
        result.body should include(htmlEscapedPageTitle("view.upload-file.waiting"))
        result.body should include(htmlEscapedMessage("view.upload-file.waiting"))

        getFileUploads() shouldBe Some(fileUploads)
      }

      "show summary page if the file is accepted" in {

        val context = FileUploadContext(fileUploadSessionConfig)
        val fileUploads = FileUploads(files =
          Seq(
            FileUpload.Initiated(Nonce.Any, Timestamp.Any, upscanRef1),
            FileUpload.Accepted(
              Nonce.Any,
              Timestamp.Any,
              upscanRef2,
              "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
              ZonedDateTime.parse("2018-04-24T09:30:00Z"),
              "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
              "test.pdf",
              "application/pdf",
              4567890
            )
          )
        )

        setContext(context)
        setFileUploads(fileUploads)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result = await(request(s"/file-verification?key=$upscanRef2").get())

        result.status shouldBe 200
        result.body should include(htmlEscapedPageTitle("view.summary.singular.heading"))
        result.body should include(htmlEscapedMessage("view.summary.singular.heading"))

        getFileUploads() shouldBe Some(fileUploads)
      }

      "show choose file page if the file is rejected" in {

        val context = FileUploadContext(fileUploadSessionConfig)
        val fileUploads = FileUploads(files =
          Seq(
            FileUpload.Initiated(Nonce.Any, Timestamp.Any, upscanRef1),
            FileUpload.Rejected(
              Nonce.Any,
              Timestamp.Any,
              upscanRef2,
              S3UploadError(upscanRef2, "EntityTooLarge", "Entity Too Large")
            )
          )
        )

        setContext(context)
        setFileUploads(fileUploads)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val callbackUrl =
          appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/$getJourneyId"

        givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

        val result = await(request(s"/file-verification?key=$upscanRef2").get())

        result.status shouldBe 400
      }

      "show error page if key is not provided" in {

        val context = FileUploadContext(fileUploadSessionConfig)
        val fileUploads = FileUploads(files =
          Seq(
            FileUpload.Initiated(Nonce.Any, Timestamp.Any, upscanRef1),
            FileUpload.Posted(Nonce.Any, Timestamp.Any, upscanRef2)
          )
        )

        setContext(context)
        setFileUploads(fileUploads)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result = await(request(s"/file-verification").get())

        result.status shouldBe 400

        getFileUploads() shouldBe Some(fileUploads)
      }
    }

    "GET /file-verification/:reference/status" should {
      "return file verification status" in {

        val context = FileUploadContext(fileUploadSessionConfig)
        val fileUploads = FileUploads(files =
          Seq(
            FileUpload.Initiated(
              Nonce.Any,
              Timestamp.Any,
              upscanRef1,
              uploadRequest =
                Some(UploadRequest(href = "https://s3.amazonaws.com/bucket/123abc", fields = Map("foo1" -> "bar1")))
            ),
            FileUpload.Posted(Nonce.Any, Timestamp.Any, upscanRef2),
            FileUpload.Accepted(
              Nonce.Any,
              Timestamp.Any,
              "f029444f-415c-4dec-9cf2-36774ec63ab8",
              "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
              ZonedDateTime.parse("2018-04-24T09:30:00Z"),
              "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
              "test.pdf",
              "application/pdf",
              4567890
            ),
            FileUpload.Failed(
              Nonce.Any,
              Timestamp.Any,
              "4b1e15a4-4152-4328-9448-4924d9aee6e2",
              UpscanNotification.FailureDetails(UpscanNotification.QUARANTINE, "some reason")
            ),
            FileUpload.Rejected(
              Nonce.Any,
              Timestamp.Any,
              "4b1e15a4-4152-4328-9448-4924d9aee6e3",
              details = S3UploadError("key", "errorCode", "Invalid file type.")
            ),
            FileUpload.Duplicate(
              Nonce.Any,
              Timestamp.Any,
              "4b1e15a4-4152-4328-9448-4924d9aee6e4",
              checksum = "0" * 64,
              existingFileName = "test.pdf",
              duplicateFileName = "test1.png"
            )
          )
        )

        setContext(context)
        setFileUploads(fileUploads)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result1 =
          await(
            request("/file-verification/11370e18-6e24-453e-b45a-76d3e32ea33d/status")
              .get()
          )

        result1.status shouldBe 200
        result1.body shouldBe s"""{"reference":"$upscanRef1","fileStatus":"NOT_UPLOADED","uploadRequest":{"href":"https://s3.amazonaws.com/bucket/123abc","fields":{"foo1":"bar1"}}}"""

        val result2 =
          await(request("/file-verification/2b72fe99-8adf-4edb-865e-622ae710f77c/status").get())
        result2.status shouldBe 200
        result2.body shouldBe s"""{"reference":"$upscanRef2","fileStatus":"WAITING"}"""

        val result3 =
          await(request("/file-verification/f029444f-415c-4dec-9cf2-36774ec63ab8/status").get())
        result3.status shouldBe 200
        result3.body shouldBe """{"reference":"f029444f-415c-4dec-9cf2-36774ec63ab8","fileStatus":"ACCEPTED","fileMimeType":"application/pdf","fileName":"test.pdf","fileSize":4567890,"previewUrl":"/upload-customs-documents/preview/f029444f-415c-4dec-9cf2-36774ec63ab8/test.pdf"}"""

        val result4 =
          await(request("/file-verification/4b1e15a4-4152-4328-9448-4924d9aee6e2/status").get())
        result4.status shouldBe 200
        result4.body shouldBe """{"reference":"4b1e15a4-4152-4328-9448-4924d9aee6e2","fileStatus":"FAILED","errorMessage":"The selected file contains a virus - upload a different one"}"""

        val result5 =
          await(request("/file-verification/f0e317f5-d394-42cc-93f8-e89f4fc0114c/status").get())
        result5.status shouldBe 404

        val result6 =
          await(request("/file-verification/4b1e15a4-4152-4328-9448-4924d9aee6e3/status").get())
        result6.status shouldBe 200
        result6.body shouldBe """{"reference":"4b1e15a4-4152-4328-9448-4924d9aee6e3","fileStatus":"REJECTED","errorMessage":"The selected file could not be uploaded"}"""

        val result7 =
          await(request("/file-verification/4b1e15a4-4152-4328-9448-4924d9aee6e4/status").get())
        result7.status shouldBe 200
        result7.body shouldBe """{"reference":"4b1e15a4-4152-4328-9448-4924d9aee6e4","fileStatus":"DUPLICATE","errorMessage":"The selected file has already been uploaded"}"""
      }
    }

    "GET /journey/:journeyId/file-verification" should {

      "set return 202 Accepted (when no response yet received)" in {

        val context = FileUploadContext(fileUploadSessionConfig)
        val fileUploads = FileUploads(files =
          Seq(
            FileUpload.Posted(Nonce.Any, Timestamp.Any, upscanRef1)
          )
        )

        setContext(context)
        setFileUploads(fileUploads)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result1 =
          await(requestWithoutSessionId(s"/journey/$getJourneyId/file-verification?key=$upscanRef1").get())

        result1.status shouldBe Status.ACCEPTED
        result1.body.isEmpty shouldBe true
      }

      "set return 201 Created (when a response is ready)" in {

        val context = FileUploadContext(fileUploadSessionConfig)
        val fileUploads = FileUploads(files =
          Seq(
            FileUpload.Accepted(
              Nonce.Any,
              Timestamp.Any,
              upscanRef1,
              "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
              ZonedDateTime.parse("2018-04-24T09:30:00Z"),
              "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
              "test.pdf",
              "application/pdf",
              4567890
            )
          )
        )

        setContext(context)
        setFileUploads(fileUploads)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result1 =
          await(requestWithoutSessionId(s"/journey/$getJourneyId/file-verification?key=$upscanRef1").get())

        result1.status shouldBe Status.CREATED
        result1.body.isEmpty shouldBe true
      }

      "set return 400 Bad Request (when key is not provided)" in {

        val context = FileUploadContext(fileUploadSessionConfig)
        val fileUploads = FileUploads(files =
          Seq(
            FileUpload.Posted(Nonce.Any, Timestamp.Any, upscanRef1)
          )
        )

        setContext(context)
        setFileUploads(fileUploads)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result1 =
          await(requestWithoutSessionId(s"/journey/$getJourneyId/file-verification").get())

        result1.status shouldBe Status.BAD_REQUEST
        result1.body.isEmpty shouldBe true
      }
    }
  }
}
