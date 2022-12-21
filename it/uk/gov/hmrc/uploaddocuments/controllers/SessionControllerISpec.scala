package uk.gov.hmrc.uploaddocuments.controllers

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, _}
import play.api.Application
import uk.gov.hmrc.uploaddocuments.support.ServerISpec

class SessionControllerISpec extends SessionControllerISpecSetup() {

  "SessionController" when {

    "GET /timedout" should {
      "display the timed out page" in {
        val result = await(requestWithoutSessionId("/timedout").get())
        result.status shouldBe 200
        result.body should include(htmlEscapedMessage("view.timedout.title"))
      }
    }

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
    }

    "GET /keep-alive" should {
      "respond with an empty json body" in {
        val result = await(requestWithoutSessionId("/keep-alive").get())
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

}
