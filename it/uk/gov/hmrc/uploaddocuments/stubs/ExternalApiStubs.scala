package uk.gov.hmrc.uploaddocuments.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json
import uk.gov.hmrc.uploaddocuments.connectors.FileUploadResultPushConnector
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
    val content: String = UUID.randomUUID().toString()
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

  def givenResultPushEndpoint(path: String, payload: FileUploadResultPushConnector.Payload, status: Int): Unit =
    stubFor(
      post(urlPathEqualTo(path))
        .withRequestBody(equalToJson(Json.stringify(Json.toJson(payload))))
        .willReturn(aResponse().withStatus(status))
    )

  def verifyResultPushHasHappened(path: String, times: Int = 1) {
    verify(times, postRequestedFor(urlEqualTo(path)))
  }

  def verifyResultPushHasNotHappened(path: String) {
    verify(0, postRequestedFor(urlEqualTo(path)))
  }

}
