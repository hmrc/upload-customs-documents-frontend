package uk.gov.hmrc.uploaddocuments.controllers

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, _}
import play.api.Application
import uk.gov.hmrc.uploaddocuments.support.ServerISpec
import play.api.libs.ws.DefaultBodyReadables.readableAsString
import java.net.URLEncoder
import scala.util.matching.Regex

class SignOutControllerISpec extends SignOutControllerISpecSetup {

  "SignOutController" when {

    "GET /sign-out/timeout" should {
      "redirect to the timed out page" in {
        givenSignOut()
        val result = await(requestWithoutSessionId("/sign-out/timeout").get())
        result.status shouldBe 200
      }
    }

    "GET /sign-out" should {
      "redirect to the feedback survey" in {
        givenSignOut()
        val result = await(requestWithoutSessionId("/sign-out").get())
        result.status shouldBe 200
      }

      "redirect to the feedback survey if the continue url is not valid" in {
        givenSignOut()
        val result = await(requestWithoutSessionId("/sign-out?continueUrl=http://foo.bar/continue-url").get())
        result.status shouldBe 200
      }

      "redirect to continue url" in {
        givenSignOutAndContinueUrl(s"$wireMockBaseUrlAsString/continue-url")
        val result =
          await(requestWithoutSessionId(s"/sign-out?continueUrl=$wireMockBaseUrlAsString/continue-url").get())
        result.status shouldBe 200
        result.status shouldBe 200
        result.body shouldBe ""
      }
    }
  }
}

trait SignOutControllerISpecSetup extends ServerISpec {

  override def fakeApplication(): Application = appBuilder.build()

  def givenSignOut(): Unit =
    stubFor(
      get(urlPathEqualTo("/dummy-sign-out-url"))
        .willReturn(
          aResponse()
            .withStatus(200)
        )
    )

  def givenSignOutAndContinueUrl(continueUrl: String): Unit =
    stubFor(
      get(urlMatching("\\/dummy-sign-out-url\\?continue\\=" + Regex.quote(URLEncoder.encode(continueUrl, "UTF-8"))))
        .willReturn(
          aResponse()
            .withStatus(200)
        )
    )

}
