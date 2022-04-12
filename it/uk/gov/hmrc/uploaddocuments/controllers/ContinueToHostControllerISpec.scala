package uk.gov.hmrc.uploaddocuments.controllers

import uk.gov.hmrc.uploaddocuments.journeys.State
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.repository.NewJourneyCacheRepository.DataKeys
import uk.gov.hmrc.uploaddocuments.stubs.ExternalApiStubs

import scala.concurrent.ExecutionContext.Implicits.global

class ContinueToHostControllerISpec extends ControllerISpecBase with ExternalApiStubs {

  "ContinueToHostController" when {

    "GET /continue-to-host" should {
      "redirect to the continueUrl if non empty file uploads" in {
        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenEmptyUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-empty"),
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full")
            )
        )
        val state = State.ContinueToHost(
          context,
          fileUploads = nonEmptyFileUploads
        )

        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.journeyContextDataKey, context))
        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.uploadedFiles, nonEmptyFileUploads))

        sessionStateService.setState(state)
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val expected = givenSomePage(200, "/continue-url")

        val result = await(request("/continue-to-host").get())

        result.status shouldBe 200
        result.body shouldBe expected

        sessionStateService.getState shouldBe State.ContinueToHost(
          context,
          fileUploads = nonEmptyFileUploads
        )
      }

      "redirect to the continueUrl if empty file uploads and no continueWhenEmptyUrl" in {
        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full")
            )
        )
        val state = State.ContinueToHost(context, fileUploads = FileUploads())

        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.journeyContextDataKey, context))
        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.uploadedFiles, FileUploads()))
        sessionStateService.setState(state)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val expected = givenSomePage(200, "/continue-url")

        val result = await(request("/continue-to-host").get())

        result.status shouldBe 200
        result.body shouldBe expected

        sessionStateService.getState shouldBe State.ContinueToHost(context, fileUploads = FileUploads())
      }

      "redirect to the continueWhenEmptyUrl if empty file uploads" in {
        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenEmptyUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-empty"),
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full")
            )
        )
        val state = State.ContinueToHost(context, fileUploads = FileUploads())

        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.journeyContextDataKey, context))
        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.uploadedFiles, FileUploads()))
        sessionStateService.setState(state)
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val expected = givenSomePage(200, "/continue-url-if-empty")

        val result = await(request("/continue-to-host").get())

        sessionStateService.getState shouldBe State.ContinueToHost(context, fileUploads = FileUploads())

        result.status shouldBe 200
        result.body shouldBe expected
      }

      "redirect to the continueUrl if full file uploads and no continueWhenFullUrl" in {
        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenEmptyUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-empty")
            )
        )
        val state = State.ContinueToHost(context, fileUploads = nFileUploads(context.config.maximumNumberOfFiles))

        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.journeyContextDataKey, context))
        await(
          newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(
            DataKeys.uploadedFiles,
            nFileUploads(context.config.maximumNumberOfFiles)
          )
        )
        sessionStateService.setState(state)
        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val expected = givenSomePage(200, "/continue-url")

        val result = await(request("/continue-to-host").get())

        result.status shouldBe 200
        result.body shouldBe expected

        sessionStateService.getState shouldBe State.ContinueToHost(
          context,
          fileUploads = nFileUploads(context.config.maximumNumberOfFiles)
        )
      }

      "redirect to the continueWhenFullUrl if full file uploads" in {
        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenEmptyUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-empty"),
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full")
            )
        )
        val state = State.ContinueToHost(context, fileUploads = nFileUploads(context.config.maximumNumberOfFiles))

        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.journeyContextDataKey, context))
        await(
          newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(
            DataKeys.uploadedFiles,
            nFileUploads(context.config.maximumNumberOfFiles)
          )
        )
        sessionStateService.setState(state)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val expected = givenSomePage(200, "/continue-url-if-full")

        val result = await(request("/continue-to-host").get())

        result.status shouldBe 200
        result.body shouldBe expected

        sessionStateService.getState shouldBe State.ContinueToHost(
          context,
          fileUploads = nFileUploads(context.config.maximumNumberOfFiles)
        )
      }
    }

    "POST /continue-to-host" should {
      "redirect to the continueUrl if answer is no and non empty file uploads" in {
        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenEmptyUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-empty"),
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full")
            )
        )
        val state = State.ContinueToHost(
          context,
          fileUploads = nonEmptyFileUploads
        )

        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.journeyContextDataKey, context))
        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.uploadedFiles, nonEmptyFileUploads))
        sessionStateService.setState(state)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val expected = givenSomePage(200, "/continue-url")

        val result = await(request("/continue-to-host").post(Map("choice" -> "no")))

        result.status shouldBe 200
        result.body shouldBe expected

        sessionStateService.getState shouldBe State.ContinueToHost(
          context,
          fileUploads = nonEmptyFileUploads
        )
      }

      "redirect to the continueUrl if answer is no and empty file uploads and no continueWhenEmptyUrl" in {
        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full")
            )
        )
        val state = State.ContinueToHost(context, fileUploads = FileUploads())

        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.journeyContextDataKey, context))
        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.uploadedFiles, FileUploads()))
        sessionStateService.setState(state)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val expected = givenSomePage(200, "/continue-url")

        val result = await(request("/continue-to-host").post(Map("choice" -> "no")))

        result.status shouldBe 200
        result.body shouldBe expected

        sessionStateService.getState shouldBe State.ContinueToHost(context, fileUploads = FileUploads())
      }

      "redirect to the continueWhenEmptyUrl if answer is no and empty file uploads" in {
        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenEmptyUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-empty"),
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full")
            )
        )
        val state = State.ContinueToHost(context, fileUploads = FileUploads())

        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.journeyContextDataKey, context))
        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.uploadedFiles, FileUploads()))
        sessionStateService.setState(state)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val expected = givenSomePage(200, "/continue-url-if-empty")

        val result = await(request("/continue-to-host").post(Map("choice" -> "no")))

        sessionStateService.getState shouldBe State.ContinueToHost(context, fileUploads = FileUploads())

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
        val state = State.ContinueToHost(context, fileUploads = nFileUploads(context.config.maximumNumberOfFiles))

        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.journeyContextDataKey, context))
        await(
          newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(
            DataKeys.uploadedFiles,
            nFileUploads(context.config.maximumNumberOfFiles)
          )
        )
        sessionStateService.setState(state)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val expected = givenSomePage(200, "/continue-url")

        val result = await(request("/continue-to-host").post(Map("choice" -> "no")))

        result.status shouldBe 200
        result.body shouldBe expected

        sessionStateService.getState shouldBe State.ContinueToHost(
          context,
          fileUploads = nFileUploads(context.config.maximumNumberOfFiles)
        )
      }

      "redirect to the continueWhenFullUrl if answer is no and full file uploads" in {
        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenEmptyUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-empty"),
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full")
            )
        )
        val state = State.ContinueToHost(context, fileUploads = nFileUploads(context.config.maximumNumberOfFiles))

        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.journeyContextDataKey, context))
        await(
          newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(
            DataKeys.uploadedFiles,
            nFileUploads(context.config.maximumNumberOfFiles)
          )
        )
        sessionStateService.setState(state)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val expected = givenSomePage(200, "/continue-url-if-full")

        val result = await(request("/continue-to-host").post(Map("choice" -> "no")))

        result.status shouldBe 200
        result.body shouldBe expected

        sessionStateService.getState shouldBe State.ContinueToHost(
          context,
          fileUploads = nFileUploads(context.config.maximumNumberOfFiles)
        )
      }

      "redirect to the backlinkUrl if answer is yes and non empty file uploads" in {
        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenEmptyUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-empty"),
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full")
            )
        )
        val state = State.ContinueToHost(
          context,
          fileUploads = nonEmptyFileUploads
        )

        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.journeyContextDataKey, context))
        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.uploadedFiles, nonEmptyFileUploads))
        sessionStateService.setState(state)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val expected = givenSomePage(200, "/backlink-url")

        val result = await(request("/continue-to-host").post(Map("choice" -> "yes")))

        result.status shouldBe 200
        result.body shouldBe expected

        sessionStateService.getState shouldBe State.Initialized(
          context,
          fileUploads = nonEmptyFileUploads
        )
      }

      "redirect to the backlinkUrl if answer is yes and empty file uploads and no continueWhenEmptyUrl" in {
        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full")
            )
        )
        val state = State.ContinueToHost(context, fileUploads = FileUploads())

        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.journeyContextDataKey, context))
        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.uploadedFiles, FileUploads()))
        sessionStateService.setState(state)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val expected = givenSomePage(200, "/backlink-url")

        val result = await(request("/continue-to-host").post(Map("choice" -> "yes")))

        result.status shouldBe 200
        result.body shouldBe expected

        sessionStateService.getState shouldBe State.Initialized(context, fileUploads = FileUploads())
      }

      "redirect to the backlinkUrl if answer is yes and empty file uploads" in {
        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenEmptyUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-empty"),
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full")
            )
        )
        val state = State.ContinueToHost(context, fileUploads = FileUploads())

        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.journeyContextDataKey, context))
        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.uploadedFiles, FileUploads()))
        sessionStateService.setState(state)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val expected = givenSomePage(200, "/backlink-url")

        val result = await(request("/continue-to-host").post(Map("choice" -> "yes")))

        sessionStateService.getState shouldBe State.Initialized(context, fileUploads = FileUploads())

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
        val state = State.ContinueToHost(context, fileUploads = nFileUploads(context.config.maximumNumberOfFiles))

        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.journeyContextDataKey, context))
        await(
          newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(
            DataKeys.uploadedFiles,
            nFileUploads(context.config.maximumNumberOfFiles)
          )
        )
        sessionStateService.setState(state)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val expected = givenSomePage(200, "/backlink-url")

        val result = await(request("/continue-to-host").post(Map("choice" -> "yes")))

        result.status shouldBe 200
        result.body shouldBe expected

        sessionStateService.getState shouldBe State.Initialized(
          context,
          fileUploads = nFileUploads(context.config.maximumNumberOfFiles)
        )
      }

      "redirect to the backlinkUrl if answer is yes and full file uploads" in {
        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenEmptyUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-empty"),
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full")
            )
        )
        val state = State.ContinueToHost(context, fileUploads = nFileUploads(context.config.maximumNumberOfFiles))

        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.journeyContextDataKey, context))
        await(
          newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(
            DataKeys.uploadedFiles,
            nFileUploads(context.config.maximumNumberOfFiles)
          )
        )
        sessionStateService.setState(state)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val expected = givenSomePage(200, "/backlink-url")

        val result = await(request("/continue-to-host").post(Map("choice" -> "yes")))

        result.status shouldBe 200
        result.body shouldBe expected

        sessionStateService.getState shouldBe State.Initialized(
          context,
          fileUploads = nFileUploads(context.config.maximumNumberOfFiles)
        )
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
        val state = State.ContinueToHost(
          context,
          fileUploads = nonEmptyFileUploads
        )

        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.journeyContextDataKey, context))
        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.uploadedFiles, nonEmptyFileUploads))
        sessionStateService.setState(state)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val expected = givenSomePage(200, "/continue-url-if-yes")

        val result = await(request("/continue-to-host").post(Map("choice" -> "yes")))

        result.status shouldBe 200
        result.body shouldBe expected

        sessionStateService.getState shouldBe State.Initialized(
          context,
          fileUploads = nonEmptyFileUploads
        )
      }

      "redirect to the continueAfterYesAnswerUrl if answer is yes and empty file uploads and no continueWhenEmptyUrl" in {
        val context = FileUploadContext(
          fileUploadSessionConfig
            .copy(
              continueWhenFullUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-full"),
              continueAfterYesAnswerUrl = Some(s"$wireMockBaseUrlAsString/continue-url-if-yes")
            )
        )
        val state = State.ContinueToHost(context, fileUploads = FileUploads())

        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.journeyContextDataKey, context))
        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.uploadedFiles, FileUploads()))
        sessionStateService.setState(state)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val expected = givenSomePage(200, "/continue-url-if-yes")

        val result = await(request("/continue-to-host").post(Map("choice" -> "yes")))

        result.status shouldBe 200
        result.body shouldBe expected

        sessionStateService.getState shouldBe State.Initialized(context, fileUploads = FileUploads())
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
        val state = State.ContinueToHost(context, fileUploads = FileUploads())

        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.journeyContextDataKey, context))
        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.uploadedFiles, FileUploads()))
        sessionStateService.setState(state)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val expected = givenSomePage(200, "/continue-url-if-yes")

        val result = await(request("/continue-to-host").post(Map("choice" -> "yes")))

        sessionStateService.getState shouldBe State.Initialized(context, fileUploads = FileUploads())

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
        val state = State.ContinueToHost(context, fileUploads = nFileUploads(context.config.maximumNumberOfFiles))

        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.journeyContextDataKey, context))
        await(
          newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(
            DataKeys.uploadedFiles,
            nFileUploads(context.config.maximumNumberOfFiles)
          )
        )
        sessionStateService.setState(state)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val expected = givenSomePage(200, "/continue-url-if-yes")

        val result = await(request("/continue-to-host").post(Map("choice" -> "yes")))

        result.status shouldBe 200
        result.body shouldBe expected

        sessionStateService.getState shouldBe State.Initialized(
          context,
          fileUploads = nFileUploads(context.config.maximumNumberOfFiles)
        )
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
        val state = State.ContinueToHost(context, fileUploads = nFileUploads(context.config.maximumNumberOfFiles))

        await(newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(DataKeys.journeyContextDataKey, context))
        await(
          newJourneyRepo.put(sessionStateService.getJourneyId(hc).get)(
            DataKeys.uploadedFiles,
            nFileUploads(context.config.maximumNumberOfFiles)
          )
        )
        sessionStateService.setState(state)

        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
        val expected = givenSomePage(200, "/continue-url-if-yes")

        val result = await(request("/continue-to-host").post(Map("choice" -> "yes")))

        result.status shouldBe 200
        result.body shouldBe expected

        sessionStateService.getState shouldBe State.Initialized(
          context,
          fileUploads = nFileUploads(context.config.maximumNumberOfFiles)
        )
      }
    }

  }
}
