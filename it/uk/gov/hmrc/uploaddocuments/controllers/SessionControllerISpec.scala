package uk.gov.hmrc.uploaddocuments.controllers

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, _}
import play.api.Application
import uk.gov.hmrc.uploaddocuments.support.ServerISpec
import play.api.libs.ws.DefaultBodyReadables.readableAsString

class SessionControllerISpec extends SessionControllerISpecSetup() {

  "SessionController" when {

    "GET /timedout" should {
      "display the timed out page" in {
        val result = await(requestWithoutSessionId("/timedout").get())
        result.status shouldBe 200
        result.body should include(htmlEscapedMessage("view.timedout.title"))
      }
    }

    "GET /keep-alive" should {
      "respond with an empty json body" in {
        val result = await(requestWithoutSessionId("/keep-alive").get())
        result.status shouldBe 200
        result.body shouldBe "{}"
      }

      "respond with a redirect to the continue url" in {
        givenContinueUrl()
        val result =
          await(requestWithoutSessionId(s"/keep-alive?continueUrl=$wireMockBaseUrlAsString/continue-url").get())
        result.status shouldBe 200
        result.body shouldBe ""
      }

      "respond with an empty json body if the continue url is not acceptable" in {
        givenContinueUrl()
        val result =
          await(requestWithoutSessionId(s"/keep-alive?continueUrl=http://foo.bar/continue-url").get())
        result.status shouldBe 200
        result.body shouldBe "{}"
      }
    }
  }
}

trait SessionControllerISpecSetup extends ServerISpec {

  override def fakeApplication(): Application = appBuilder.build()

  def givenSignOut(): Unit =
    stubFor(
      get(urlPathEqualTo("/dummy-sign-out-url"))
        .willReturn(
          aResponse()
            .withStatus(200)
        )
    )

  def givenContinueUrl(): Unit =
    stubFor(
      get(urlPathEqualTo("/continue-url"))
        .willReturn(
          aResponse()
            .withStatus(200)
        )
    )

}
