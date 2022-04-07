package uk.gov.hmrc.uploaddocuments.services

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.uploaddocuments.models.{FileUploadContext, FileUploadInitializationRequest, FileUploadSessionConfig, FileUploads, Nonce}
import uk.gov.hmrc.uploaddocuments.support.{AppISpec, SHA256}

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.uploaddocuments.models.HostService

import uk.gov.hmrc.uploaddocuments.journeys.State
import uk.gov.hmrc.uploaddocuments.journeys.JourneyModel
import uk.gov.hmrc.uploaddocuments.journeys.IsTransient

class MongoDBCachedSesionStateServiceISpec extends AppISpec {

  lazy val service: MongoDBCachedSessionStateService =
    app.injector.instanceOf[MongoDBCachedSessionStateService]

  implicit val hc: HeaderCarrier =
    HeaderCarrier()
      .withExtraHeaders("FileUploadJourney" -> SHA256.compute(UUID.randomUUID.toString))

  val fileUploadContext =
    FileUploadContext(config =
      FileUploadSessionConfig(
        Nonce.random,
        "http://localhost:1111/foo",
        "https://tax.service.gov.uk/bar",
        "https://service.public.mdtp/zoo"
      )
    )
  val request =
    FileUploadInitializationRequest(config = fileUploadContext.config, existingFiles = Seq.empty)

  "MongoDBCachedSessionStateService" should {
    "apply initialize transition" in {
      await(service.updateSessionState(JourneyModel.initialize(HostService.Any)(request))) shouldBe (
        (
          State.Initialized(fileUploadContext, FileUploads()),
          List(State.Uninitialized)
        )
      )
    }

    "keep breadcrumbs when no change in state" in {
      Breadcrumbs.updateBreadcrumbs(
        State.Initialized(fileUploadContext, FileUploads()),
        State.Initialized(fileUploadContext, FileUploads()),
        Nil,
        (s: State) => s.isInstanceOf[IsTransient]
      ) shouldBe Nil
    }

    "update breadcrumbs when new state" in {
      Breadcrumbs.updateBreadcrumbs(
        State.Initialized(fileUploadContext, FileUploads()),
        State.ContinueToHost(fileUploadContext, FileUploads()),
        Nil,
        (s: State) => s.isInstanceOf[IsTransient]
      ) shouldBe List(
        State.ContinueToHost(fileUploadContext, FileUploads(List()))
      )
    }

    "trim breadcrumbs when returning back to the previous state" in {
      Breadcrumbs.updateBreadcrumbs(
        State.Initialized(fileUploadContext, FileUploads()),
        State.ContinueToHost(fileUploadContext, FileUploads()),
        List(State.Initialized(fileUploadContext, FileUploads())),
        (s: State) => s.isInstanceOf[IsTransient]
      ) shouldBe Nil
    }
  }

}
