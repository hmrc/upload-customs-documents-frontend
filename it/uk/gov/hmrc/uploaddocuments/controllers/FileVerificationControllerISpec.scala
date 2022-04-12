package uk.gov.hmrc.uploaddocuments.controllers

import uk.gov.hmrc.uploaddocuments.journeys.State
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.repository.NewJourneyCacheRepository.DataKeys

import java.time.ZonedDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.uploaddocuments.support.SHA256

class FileVerificationControllerISpec extends ControllerISpecBase {

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

        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.journeyContextDataKey, context))
        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.uploadedFiles, fileUploads))
        sessionStateService.setState(
          State.UploadSingleFile(
            context,
            upscanRef2,
            UploadRequest(href = "https://s3.bucket", fields = Map("callbackUrl" -> "https://foo.bar/callback")),
            fileUploads
          )
        )
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result = await(request("/file-verification").get())

        result.status shouldBe 200
        result.body should include(htmlEscapedPageTitle("view.upload-file.waiting"))
        result.body should include(htmlEscapedMessage("view.upload-file.waiting"))

        sessionStateService.getState shouldBe
          State.WaitingForFileVerification(
            context,
            upscanRef2,
            UploadRequest(href = "https://s3.bucket", fields = Map("callbackUrl" -> "https://foo.bar/callback")),
            FileUpload.Posted(Nonce.Any, Timestamp.Any, upscanRef2),
            fileUploads
          )
      }

      "display waiting for file verification page after the timeout the next time" in {

        val context = FileUploadContext(fileUploadSessionConfig)
        val fileUploads = FileUploads(files =
          Seq(
            FileUpload.Initiated(Nonce.Any, Timestamp.Any, upscanRef1),
            FileUpload.Posted(Nonce.Any, Timestamp.Any, upscanRef2)
          )
        )

        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.journeyContextDataKey, context))
        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.uploadedFiles, fileUploads))

        val state = State.WaitingForFileVerification(
          context,
          upscanRef2,
          UploadRequest(href = "https://s3.bucket", fields = Map("callbackUrl" -> "https://foo.bar/callback")),
          FileUpload.Posted(Nonce.Any, Timestamp.Any, upscanRef2),
          fileUploads
        )
        sessionStateService.setState(state)
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result = await(request("/file-verification").get())

        result.status shouldBe 200
        result.body should include(htmlEscapedPageTitle("view.upload-file.waiting"))
        result.body should include(htmlEscapedMessage("view.upload-file.waiting"))

        sessionStateService.getState shouldBe state
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

        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.journeyContextDataKey, context))
        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.uploadedFiles, fileUploads))

        val state = State.Summary(
          context,
          fileUploads,
          acknowledged = false
        )
        sessionStateService.setState(state)
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result1 =
          await(
            request("/file-verification/11370e18-6e24-453e-b45a-76d3e32ea33d/status")
              .get()
          )
        result1.status shouldBe 200
        result1.body shouldBe s"""{"reference":"$upscanRef1","fileStatus":"NOT_UPLOADED","uploadRequest":{"href":"https://s3.amazonaws.com/bucket/123abc","fields":{"foo1":"bar1"}}}"""
        sessionStateService.getState shouldBe state

        val result2 =
          await(request("/file-verification/2b72fe99-8adf-4edb-865e-622ae710f77c/status").get())
        result2.status shouldBe 200
        result2.body shouldBe s"""{"reference":"$upscanRef2","fileStatus":"WAITING"}"""
        sessionStateService.getState shouldBe state

        val result3 =
          await(request("/file-verification/f029444f-415c-4dec-9cf2-36774ec63ab8/status").get())
        result3.status shouldBe 200
        result3.body shouldBe """{"reference":"f029444f-415c-4dec-9cf2-36774ec63ab8","fileStatus":"ACCEPTED","fileMimeType":"application/pdf","fileName":"test.pdf","fileSize":4567890,"previewUrl":"/upload-customs-documents/preview/f029444f-415c-4dec-9cf2-36774ec63ab8/test.pdf"}"""
        sessionStateService.getState shouldBe state

        val result4 =
          await(request("/file-verification/4b1e15a4-4152-4328-9448-4924d9aee6e2/status").get())
        result4.status shouldBe 200
        result4.body shouldBe """{"reference":"4b1e15a4-4152-4328-9448-4924d9aee6e2","fileStatus":"FAILED","errorMessage":"The selected file contains a virus - upload a different one"}"""
        sessionStateService.getState shouldBe state

        val result5 =
          await(request("/file-verification/f0e317f5-d394-42cc-93f8-e89f4fc0114c/status").get())
        result5.status shouldBe 404
        sessionStateService.getState shouldBe state

        val result6 =
          await(request("/file-verification/4b1e15a4-4152-4328-9448-4924d9aee6e3/status").get())
        result6.status shouldBe 200
        result6.body shouldBe """{"reference":"4b1e15a4-4152-4328-9448-4924d9aee6e3","fileStatus":"REJECTED","errorMessage":"The selected file could not be uploaded"}"""
        sessionStateService.getState shouldBe state

        val result7 =
          await(request("/file-verification/4b1e15a4-4152-4328-9448-4924d9aee6e4/status").get())
        result7.status shouldBe 200
        result7.body shouldBe """{"reference":"4b1e15a4-4152-4328-9448-4924d9aee6e4","fileStatus":"DUPLICATE","errorMessage":"The selected file has already been uploaded"}"""
        sessionStateService.getState shouldBe state
      }
    }

    "GET /journey/:journeyId/file-verification" should {
      "set current file upload status as posted and return 204 NoContent" in {

        val context = FileUploadContext(fileUploadSessionConfig)
        val fileUploads = FileUploads(files =
          Seq(
            FileUpload.Initiated(Nonce.Any, Timestamp.Any, upscanRef1),
            FileUpload.Posted(Nonce.Any, Timestamp.Any, upscanRef2)
          )
        )

        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.journeyContextDataKey, context))
        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.uploadedFiles, fileUploads))

        sessionStateService.setState(
          State.UploadSingleFile(
            context,
            upscanRef1,
            UploadRequest(href = "https://s3.bucket", fields = Map("callbackUrl" -> "https://foo.bar/callback")),
            fileUploads
          )
        )
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result1 =
          await(requestWithoutSessionId(s"/journey/${SHA256.compute(journeyId)}/file-verification").get())

        result1.status shouldBe 202
        result1.body.isEmpty shouldBe true
        sessionStateService.getState shouldBe
          State.WaitingForFileVerification(
            context,
            upscanRef1,
            UploadRequest(href = "https://s3.bucket", fields = Map("callbackUrl" -> "https://foo.bar/callback")),
            FileUpload.Posted(Nonce.Any, Timestamp.Any, upscanRef1),
            FileUploads(files =
              Seq(
                FileUpload.Posted(Nonce.Any, Timestamp.Any, upscanRef1),
                FileUpload.Posted(Nonce.Any, Timestamp.Any, upscanRef2)
              )
            )
          )
      }
    }
  }
}
