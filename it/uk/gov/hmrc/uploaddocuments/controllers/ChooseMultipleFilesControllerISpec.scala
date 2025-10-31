package uk.gov.hmrc.uploaddocuments.controllers

import uk.gov.hmrc.uploaddocuments.models.*
import uk.gov.hmrc.uploaddocuments.stubs.{ExternalApiStubs, UpscanInitiateStubs}
import uk.gov.hmrc.uploaddocuments.support.JsEnabled
import play.api.libs.ws.DefaultBodyReadables.readableAsString
import play.api.libs.ws.writeableOf_urlEncodedSimpleForm

class ChooseMultipleFilesControllerISpec extends ControllerISpecBase with UpscanInitiateStubs with ExternalApiStubs {

  "ChooseMultipleFilesController" when {

    "GET /choose-files" when {

      "no existing files are pre-popped" should {

        "show the upload multiple files page when cookie set" in {

          setContext()
          setFileUploads()

          givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

          val result = await(requestWithCookies("/choose-files", JsEnabled.COOKIE_JSENABLED -> "true").get())

          result.status shouldBe 200
          result.body should include(htmlEscapedPageTitle("view.upload-multiple-files.title"))
          result.body should include(htmlEscapedMessage("view.upload-multiple-files.heading"))
        }

        "show the upload multiple files page when cookie set and yes/no question is enabled" in {

          setContext(
            FileUploadContext(fileUploadSessionConfig.copy(features = Features(showYesNoQuestionBeforeContinue = true)))
          )
          setFileUploads()

          givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

          val result = await(requestWithCookies("/choose-files", JsEnabled.COOKIE_JSENABLED -> "true").get())

          result.status shouldBe 200
          result.body should include(htmlEscapedPageTitle("view.upload-multiple-files.title"))
          result.body should include(htmlEscapedMessage("view.upload-multiple-files.heading"))
        }

        "show the upload multiple files page with pre-populated no radio button when cookie set and yes/no question is enabled" in {
          setContext(
            FileUploadContext(
              fileUploadSessionConfig
                .copy(features = Features(showYesNoQuestionBeforeContinue = true), prePopulateYesOrNoForm = Some(false))
            )
          )
          setFileUploads()

          givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

          val result = await(requestWithCookies("/choose-files", JsEnabled.COOKIE_JSENABLED -> "true").get())

          result.status shouldBe 200
          result.body should include(htmlEscapedPageTitle("view.upload-multiple-files.title"))
          result.body should include(htmlEscapedMessage("view.upload-multiple-files.heading"))
          result.body should include regex """value=\"no\"\s*checked"""
        }

        "show the upload multiple files page with pre-populated yes radio button when cookie set and yes/no question is enabled" in {
          setContext(
            FileUploadContext(
              fileUploadSessionConfig
                .copy(features = Features(showYesNoQuestionBeforeContinue = true), prePopulateYesOrNoForm = Some(true))
            )
          )
          setFileUploads()

          givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

          val result = await(requestWithCookies("/choose-files", JsEnabled.COOKIE_JSENABLED -> "true").get())

          result.status shouldBe 200
          result.body should include(htmlEscapedPageTitle("view.upload-multiple-files.title"))
          result.body should include(htmlEscapedMessage("view.upload-multiple-files.heading"))
          result.body should include regex """value=\"yes\"\s*checked"""
        }

        "show the single files page when cookie set but the feature is turned off" in {

          val journeyContext = fileUploadSessionConfig.copy(features = Features(showUploadMultiple = false))

          setContext(FileUploadContext(journeyContext))
          setFileUploads()

          givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

          val callbackUrl =
            appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/$getJourneyId"
          givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

          val result = await(requestWithCookies("/choose-files", JsEnabled.COOKIE_JSENABLED -> "true").get())

          result.status shouldBe 200
          result.body should include(htmlEscapedPageTitle("view.upload-file.first.title"))
          result.body should include(htmlEscapedMessage("view.upload-file.first.heading"))
        }

        "show the upload single file per page when no cookie set" in {

          setContext()
          setFileUploads()

          givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

          val callbackUrl =
            appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/$getJourneyId"
          givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

          // Follows a redirect which then renders the Choose Single File page
          val result = await(request("/choose-files").get())

          result.status shouldBe 200
          result.body should include(htmlEscapedPageTitle("view.upload-file.first.title"))
          result.body should include(htmlEscapedMessage("view.upload-file.first.heading"))

          getFileUploads() shouldBe Some(
            FileUploads(files =
              Seq(
                FileUpload.Initiated(
                  Nonce.Any,
                  Timestamp.Any,
                  "11370e18-6e24-453e-b45a-76d3e32ea33d",
                  Some(
                    UploadRequest(
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
                    )
                  )
                )
              )
            )
          )
        }
      }

      "existing files are pre-popped" should {

        "show the summary page when JS-Detection cookie set but the feature is turned off" in {

          val journeyContext = fileUploadSessionConfig.copy(features = Features(showUploadMultiple = false))

          setContext(FileUploadContext(journeyContext, userWantsToUploadNextFile = false))
          setFileUploads(nFileUploads(1))

          givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

          val callbackUrl =
            appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/$getJourneyId"
          givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

          val result = await(requestWithCookies("/choose-files", JsEnabled.COOKIE_JSENABLED -> "true").get())

          result.status shouldBe 200
          result.body should include(htmlEscapedPageTitle("view.summary.singular.title"))
          result.body should include(htmlEscapedMessage("view.summary.singular.title"))
        }

        "show the upload first single file page when JS-Detection cookie set but the feature is turned off" in {

          val journeyContext = fileUploadSessionConfig.copy(features = Features(showUploadMultiple = false))

          setContext(FileUploadContext(journeyContext, userWantsToUploadNextFile = true))
          setFileUploads(nFileUploads(0))

          givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

          val callbackUrl =
            appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/$getJourneyId"
          givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

          val result = await(requestWithCookies("/choose-files", JsEnabled.COOKIE_JSENABLED -> "true").get())

          result.status shouldBe 200
          result.body should include(htmlEscapedPageTitle("view.upload-file.first.title"))
          result.body should include(htmlEscapedMessage("view.upload-file.first.title"))
        }

        "show the upload next single file page when JS-Detection cookie set but the feature is turned off" in {

          val journeyContext = fileUploadSessionConfig.copy(features = Features(showUploadMultiple = false))

          setContext(FileUploadContext(journeyContext, userWantsToUploadNextFile = true))
          setFileUploads(nFileUploads(1))

          givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

          val callbackUrl =
            appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/$getJourneyId"
          givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

          val result = await(requestWithCookies("/choose-files", JsEnabled.COOKIE_JSENABLED -> "true").get())

          result.status shouldBe 200
          result.body should include(htmlEscapedPageTitle("view.upload-file.next.title"))
          result.body should include(htmlEscapedMessage("view.upload-file.next.title"))
        }

        "show the upload summary page when js is disabled" in {

          setContext()
          setFileUploads(nFileUploads(1))

          givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

          val callbackUrl =
            appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/$getJourneyId"
          givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

          // Follows a redirect which then renders the Choose Single File page
          val result = await(request("/choose-files").get())

          result.status shouldBe 200
          result.body should include(htmlEscapedPageTitle("view.summary.singular.title"))
          result.body should include(htmlEscapedMessage("view.summary.singular.title"))
        }

        "show the upload first single file per page when js is disabled" in {

          setContext()
          setFileUploads(nFileUploads(0))

          givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

          val callbackUrl =
            appConfig.baseInternalCallbackUrl + s"/internal/callback-from-upscan/journey/$getJourneyId"
          givenUpscanInitiateSucceeds(callbackUrl, hostUserAgent)

          // Follows a redirect which then renders the Choose Single File page
          val result = await(request("/choose-files").get())

          result.status shouldBe 200
          result.body should include(htmlEscapedPageTitle("view.upload-file.first.title"))
          result.body should include(htmlEscapedMessage("view.upload-file.first.title"))
        }
      }
    }

    "POST /choose-files" should {
      "redirect to the continueUrl if answer is no and non empty file uploads" in {

        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenEmptyUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-empty"),
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full")
            )
        )

        setContext(context)
        setFileUploads(nonEmptyFileUploads)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val expected = givenSomePage(200, "/continue-url")
        val result   = await(request("/choose-files").post(Map("choice" -> "no")))

        result.status shouldBe 200
        result.body shouldBe expected
      }

      "redirect to the continueUrl if answer is no and empty file uploads and no continueWhenEmptyUrl" in {

        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full")
            )
        )

        setContext(context)
        setFileUploads(FileUploads())

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val expected = givenSomePage(200, "/continue-url")
        val result   = await(request("/choose-files").post(Map("choice" -> "no")))

        result.status shouldBe 200
        result.body shouldBe expected
      }

      "redirect to the continueWhenEmptyUrl if answer is no and empty file uploads" in {

        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenEmptyUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-empty"),
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full")
            )
        )

        setContext(context)
        setFileUploads(FileUploads())

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val expected = givenSomePage(200, "/continue-url-if-empty")
        val result   = await(request("/choose-files").post(Map("choice" -> "no")))

        result.status shouldBe 200
        result.body shouldBe expected
      }

      "redirect to the continueUrl if answer is no and full file uploads and no continueWhenFullUrl" in {

        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenEmptyUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-empty")
            )
        )

        setContext(context)
        setFileUploads(nFileUploads(context.config.maximumNumberOfFiles))

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val expected = givenSomePage(200, "/continue-url")
        val result   = await(request("/choose-files").post(Map("choice" -> "no")))

        result.status shouldBe 200
        result.body shouldBe expected
      }

      "redirect to the continueWhenFullUrl if answer is no and full file uploads" in {

        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenEmptyUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-empty"),
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full")
            )
        )

        setContext(context)
        setFileUploads(nFileUploads(context.config.maximumNumberOfFiles))

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val expected = givenSomePage(200, "/continue-url-if-full")
        val result   = await(request("/choose-files").post(Map("choice" -> "no")))

        result.status shouldBe 200
        result.body shouldBe expected
      }

      "redirect to the backlinkUrl if answer is yes and non empty file uploads" in {

        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenEmptyUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-empty"),
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full")
            )
        )

        setContext(context)
        setFileUploads(nonEmptyFileUploads)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val expected = givenSomePage(200, "/backlink-url")
        val result   = await(request("/choose-files").post(Map("choice" -> "yes")))

        result.status shouldBe 200
        result.body shouldBe expected
      }

      "redirect to the backlinkUrl if answer is yes and empty file uploads and no continueWhenEmptyUrl" in {

        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full")
            )
        )

        setContext(context)
        setFileUploads(FileUploads())

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val expected = givenSomePage(200, "/backlink-url")
        val result   = await(request("/choose-files").post(Map("choice" -> "yes")))

        result.status shouldBe 200
        result.body shouldBe expected
      }

      "redirect to the backlinkUrl if answer is yes and empty file uploads" in {

        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenEmptyUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-empty"),
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full")
            )
        )

        setContext(context)
        setFileUploads(FileUploads())

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val expected = givenSomePage(200, "/backlink-url")
        val result   = await(request("/choose-files").post(Map("choice" -> "yes")))

        result.status shouldBe 200
        result.body shouldBe expected
      }

      "redirect to the backlinkUrl if answer is yes and full file uploads and no continueWhenFullUrl" in {

        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenEmptyUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-empty")
            )
        )

        setContext(context)
        setFileUploads(nFileUploads(context.config.maximumNumberOfFiles))

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val expected = givenSomePage(200, "/backlink-url")
        val result   = await(request("/choose-files").post(Map("choice" -> "yes")))

        result.status shouldBe 200
        result.body shouldBe expected
      }

      "redirect to the backlinkUrl if answer is yes and full file uploads" in {

        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenEmptyUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-empty"),
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full")
            )
        )

        setContext(context)
        setFileUploads(nFileUploads(context.config.maximumNumberOfFiles))

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val expected = givenSomePage(200, "/backlink-url")
        val result   = await(request("/choose-files").post(Map("choice" -> "yes")))

        result.status shouldBe 200
        result.body shouldBe expected
      }

      "redirect to the continueAfterYesAnswerUrl if answer is yes and non empty file uploads" in {

        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenEmptyUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-empty"),
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full"),
              continueAfterYesAnswerUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-yes")
            )
        )

        setContext(context)
        setFileUploads(nonEmptyFileUploads)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val expected = givenSomePage(200, "/continue-url-if-yes")
        val result   = await(request("/choose-files").post(Map("choice" -> "yes")))

        result.status shouldBe 200
        result.body shouldBe expected
      }

      "redirect to the continueAfterYesAnswerUrl if answer is yes and empty file uploads and no continueWhenEmptyUrl" in {

        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full"),
              continueAfterYesAnswerUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-yes")
            )
        )

        setContext(context)
        setFileUploads(FileUploads())

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val expected = givenSomePage(200, "/continue-url-if-yes")
        val result   = await(request("/choose-files").post(Map("choice" -> "yes")))

        result.status shouldBe 200
        result.body shouldBe expected
      }

      "redirect to the continueAfterYesAnswerUrl if answer is yes and empty file uploads" in {

        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenEmptyUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-empty"),
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full"),
              continueAfterYesAnswerUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-yes")
            )
        )

        setContext(context)
        setFileUploads(FileUploads())

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val expected = givenSomePage(200, "/continue-url-if-yes")
        val result   = await(request("/choose-files").post(Map("choice" -> "yes")))

        result.status shouldBe 200
        result.body shouldBe expected
      }

      "redirect to the continueAfterYesAnswerUrl if answer is yes and full file uploads and no continueWhenFullUrl" in {

        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenEmptyUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-empty"),
              continueAfterYesAnswerUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-yes")
            )
        )

        setContext(context)
        setFileUploads(nFileUploads(context.config.maximumNumberOfFiles))

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val expected = givenSomePage(200, "/continue-url-if-yes")
        val result   = await(request("/choose-files").post(Map("choice" -> "yes")))

        result.status shouldBe 200
        result.body shouldBe expected
      }

      "redirect to the continueAfterYesAnswerUrl if answer is yes and full file uploads" in {

        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenEmptyUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-empty"),
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full"),
              continueAfterYesAnswerUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-yes")
            )
        )

        setContext(context)
        setFileUploads(nFileUploads(context.config.maximumNumberOfFiles))

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val expected = givenSomePage(200, "/continue-url-if-yes")
        val result   = await(request("/choose-files").post(Map("choice" -> "yes")))

        result.status shouldBe 200
        result.body shouldBe expected
      }

      "render error page if answer is neither yes nor no" in {

        val context = FileUploadContext(fileUploadSessionConfig)

        setContext(context)
        setFileUploads(nFileUploads(1))

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result =
          await(
            requestWithCookies("/choose-files")
              .post(
                Map(
                  "choice"    -> "foo",
                  "csrfToken" -> "6d1b707b96b03cd3ecc5440f1cd670a5f9aeb306-1759117895205-54e2b6cf40e0a2b941904b3e"
                )
              )
          )

        result.status shouldBe 400
        result.body should include(htmlEscapedPageTitle("view.upload-multiple-files.title"))
        result.body should include(htmlEscapedMessage("view.upload-multiple-files.heading"))
      }

      "render page again if missing continue url" in {

        val context =
          FileUploadContext(fileUploadSessionConfig.copy(backlinkUrl = None, continueAfterYesAnswerUrl = None))

        setContext(context)
        setFileUploads(nFileUploads(1))

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))

        val result =
          await(
            requestWithCookies("/choose-files").post(
              Map("choice" -> "yes")
            )
          )

        result.status shouldBe 200
        result.body should include(htmlEscapedPageTitle("view.upload-multiple-files.title"))
        result.body should include(htmlEscapedMessage("view.upload-multiple-files.heading"))
      }
    }
  }
}
