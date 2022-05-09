package uk.gov.hmrc.uploaddocuments.controllers

import play.api.http.{HeaderNames, Status}
import play.api.libs.json.Json
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.stubs.UpscanInitiateStubs

import java.time.ZonedDateTime

class FileRejectedControllerISpec extends ControllerISpecBase with UpscanInitiateStubs {

  "FileRejectedController" when {

    "GET /file-rejected" should {

      "show upload document again with new Upscan Initiate request (Rendering a Bad Request with errors)" in {

        setContext()
        setFileUploads(FileUploads(files =
          Seq(
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
            FileUpload.Initiated(Nonce.Any, Timestamp.Any, "2b72fe99-8adf-4edb-865e-622ae710f77c")
          )
        ))

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val callbackUrl =
          appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/$getJourneyId"
        givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

        val result = await(
          request(
            "/file-rejected?key=2b72fe99-8adf-4edb-865e-622ae710f77c&errorCode=EntityTooLarge&errorMessage=Entity+Too+Large"
          ).get()
        )

        result.status shouldBe Status.BAD_REQUEST
        result.body should include(htmlEscapedPageTitle("view.upload-file.next.title"))
        result.body should include(htmlEscapedMessage("view.upload-file.next.heading"))

        getFileUploads() shouldBe Some(
          FileUploads(files =
            Seq(
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
              FileUpload.Initiated(Nonce.Any, Timestamp.Any, "11370e18-6e24-453e-b45a-76d3e32ea33d", Some(UploadRequest(
                href = "https://bucketName.s3.eu-west-2.amazonaws.com",
                fields = Map(
                  "Content-Type"            -> "application/xml",
                  "acl"                     -> "private",
                  "key"                     -> "xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
                  "policy"                  -> "xxxxxxxx==",
                  "x-amz-algorithm"         -> "AWS4-HMAC-SHA256",
                  "x-amz-credential"        -> "ASIAxxxxxxxxx/20180202/eu-west-2/s3/aws4_request",
                  "x-amz-date"              -> "yyyyMMddThhmmssZ",
                  "x-amz-meta-callback-url" -> callbackUrl,
                  "x-amz-signature"         -> "xxxx",
                  "success_action_redirect" -> "https://myservice.com/nextPage",
                  "error_action_redirect"   -> "https://myservice.com/errorPage"
                )
              )))
            )
          )
        )
      }
    }

    "POST /file-rejected" should {
      "mark file upload as rejected" in {

        setContext()
        setFileUploads(FileUploads(files =
          Seq(
            FileUpload.Initiated(Nonce.Any, Timestamp.Any, "11370e18-6e24-453e-b45a-76d3e32ea33d"),
            FileUpload.Initiated(Nonce.Any, Timestamp.Any, "2b72fe99-8adf-4edb-865e-622ae710f77c")
          )
        ))

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result = await(
          request("/file-rejected").post(
            Json.obj(fields =
              "key"          -> "2b72fe99-8adf-4edb-865e-622ae710f77c",
              "errorCode"    -> "EntityTooLarge",
              "errorMessage" -> "Entity Too Large"
            )
          )
        )

        result.status shouldBe 201

        getFileUploads() shouldBe Some(
          FileUploads(files =
            Seq(
              FileUpload.Initiated(Nonce.Any, Timestamp.Any, "11370e18-6e24-453e-b45a-76d3e32ea33d"),
              FileUpload.Rejected(
                Nonce.Any,
                Timestamp.Any,
                "2b72fe99-8adf-4edb-865e-622ae710f77c",
                S3UploadError("2b72fe99-8adf-4edb-865e-622ae710f77c", "EntityTooLarge", "Entity Too Large")
              )
            )
          )
        )
      }
    }

    "GET /journey/:journeyId/file-rejected" should {
      "set current file upload status as rejected and return 204 NoContent" in {

        setContext()
        setFileUploads(FileUploads(files =
          Seq(
            FileUpload.Initiated(Nonce.Any, Timestamp.Any, "11370e18-6e24-453e-b45a-76d3e32ea33d"),
            FileUpload.Posted(Nonce.Any, Timestamp.Any, "2b72fe99-8adf-4edb-865e-622ae710f77c")
          )
        ))

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result1 =
          await(
            requestWithoutSessionId(
              s"/journey/$getJourneyId/file-rejected?key=11370e18-6e24-453e-b45a-76d3e32ea33d&errorCode=ABC123&errorMessage=ABC+123"
            ).get()
          )

        result1.status shouldBe 204
        result1.body.isEmpty shouldBe true

        getFileUploads() shouldBe Some(
          FileUploads(files =
            Seq(
              FileUpload.Rejected(
                Nonce.Any,
                Timestamp.Any,
                "11370e18-6e24-453e-b45a-76d3e32ea33d",
                S3UploadError(
                  key = "11370e18-6e24-453e-b45a-76d3e32ea33d",
                  errorCode = "ABC123",
                  errorMessage = "ABC 123"
                )
              ),
              FileUpload.Posted(Nonce.Any, Timestamp.Any, "2b72fe99-8adf-4edb-865e-622ae710f77c")
            )
          )
        )
      }
    }
  }
}
