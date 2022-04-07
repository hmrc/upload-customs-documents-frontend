package uk.gov.hmrc.uploaddocuments.controllers

import play.api.mvc.{AnyContent, Call, Cookie, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.support.{TestAppConfig, UnitSpec}

import java.util.UUID

class RouterSpec extends RouterSpecSetup {

  implicit val journeyId = UUID.randomUUID.toString

  val wireMockBaseUrlAsString: String = "http://foo:4321"

  val router = new Router(TestAppConfig(wireMockBaseUrlAsString, 4231))

  val fileUploadSessionConfig =
    FileUploadSessionConfig(
      nonce = Nonce.random,
      continueUrl = s"$wireMockBaseUrlAsString/continue-url",
      backlinkUrl = s"$wireMockBaseUrlAsString/backlink-url",
      callbackUrl = s"$wireMockBaseUrlAsString/result-post-url"
    )

  val FILES_LIMIT = fileUploadSessionConfig.maximumNumberOfFiles

  "Router" when {

    "preferUploadMultipleFiles" should {
      "return false when jsenabled cookie NOT set" in {
        router.preferUploadMultipleFiles(FakeRequest()) shouldBe false
      }

      "return true when jsenabled cookie set" in {
        router.preferUploadMultipleFiles(
          FakeRequest().withCookies(Cookie(router.COOKIE_JSENABLED, "true"))
        ) shouldBe true
      }
    }

    "successRedirect" should {
      "return /file-verification when jsenabled cookie NOT set" in {
        router.successRedirect(journeyId)(FakeRequest()) should endWith(
          "/base.external.callback/file-verification"
        )
      }

      "return /journey/:journeyId/file-verification when jsenabled cookie set" in {
        router.successRedirect(journeyId)(
          fakeRequest(Cookie(router.COOKIE_JSENABLED, "true"))
        ) should endWith(
          s"/base.external.callback/journey/$journeyId/file-verification"
        )
      }
    }

    "errorRedirect" should {
      "return /file-rejected when jsenabled cookie NOT set" in {
        router.errorRedirect(journeyId)(FakeRequest()) should endWith(
          "/base.external.callback/file-rejected"
        )
      }

      "return /journey/:journeyId/file-rejected when jsenabled cookie set" in {
        router.errorRedirect(journeyId)(
          fakeRequest(Cookie(router.COOKIE_JSENABLED, "true"))
        ) should endWith(
          s"/base.external.callback/journey/$journeyId/file-rejected"
        )
      }
    }

  }
}

trait RouterSpecSetup extends UnitSpec {

  final def fakeRequest(cookies: Cookie*)(implicit
    journeyId: String
  ): Request[AnyContent] =
    fakeRequest("GET", "/", cookies: _*)

  final def fakeRequest(method: String, path: String, cookies: Cookie*)(implicit
    journeyId: String
  ): Request[AnyContent] =
    FakeRequest(Call(method, path))
      .withCookies(cookies: _*)
      .withSession(SessionKeys.sessionId -> journeyId)
}
