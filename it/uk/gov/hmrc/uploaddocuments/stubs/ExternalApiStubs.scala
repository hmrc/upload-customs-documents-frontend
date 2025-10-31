package uk.gov.hmrc.uploaddocuments.stubs

import com.github.tomakehurst.wiremock.client.WireMock.*
import play.api.libs.json.Json
import uk.gov.hmrc.uploaddocuments.models.fileUploadResultPush.Payload
import uk.gov.hmrc.uploaddocuments.support.WireMockSupport

import java.util.UUID

trait ExternalApiStubs {
  me: WireMockSupport =>

  def stubForFileDownload(status: Int, bytes: Array[Byte], fileName: String): String = {
    val url = s"$wireMockBaseUrlAsString/bucket/$fileName"

    stubFor(
      get(urlPathEqualTo(s"/bucket/$fileName"))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withHeader("Content-Type", "application/octet-stream")
            .withHeader("Content-Length", s"${bytes.length}")
            .withBody(bytes)
        )
    )

    url
  }

  def stubForFileDownloadFailure(status: Int, fileName: String): String = {
    val url = s"$wireMockBaseUrlAsString/bucket/$fileName"

    stubFor(
      get(urlPathEqualTo(s"/bucket/$fileName"))
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )

    url
  }

  def givenSomePage(status: Int, path: String): String = {
    val content: String = UUID.randomUUID().toString
    stubFor(
      get(urlPathEqualTo(path))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withHeader("Content-Type", "text/plain")
            .withBody(content)
        )
    )
    content
  }

  def givenSomeAction(status: Int, path: String): String = {
    val content: String = UUID.randomUUID().toString
    stubFor(
      post(urlPathEqualTo(path))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withHeader("Content-Type", "text/plain")
            .withBody(content)
        )
    )
    content
  }

  def givenResultPushEndpoint(path: String, payload: Payload, status: Int): Unit =
    stubFor(
      post(urlPathEqualTo(path))
        .withRequestBody(equalToJson(Json.stringify(Json.toJson(payload))))
        .willReturn(aResponse().withStatus(status))
    )

  def verifyResultPushHasHappened(path: String, times: Int = 1): Unit =
    verify(times, postRequestedFor(urlEqualTo(path)))

  def verifyResultPushHasNotHappened(path: String): Unit =
    verify(0, postRequestedFor(urlEqualTo(path)))

  def stubForFileUpload(status: Int, fileName: String): String = {
    val url = s"$wireMockBaseUrlAsString/bucket/$fileName"

    stubFor(
      post(urlPathEqualTo(s"/bucket/$fileName"))
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )

    url
  }

}
