package uk.gov.hmrc.uploaddocuments.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.uploaddocuments.support.WireMockSupport

trait ObjectStoreStubs {
  me: WireMockSupport =>

  def givenObjectStorePresignedUrl(url: String, size: Int, md5Hash: String): ObjectStoreStubs = {
    stubFor(
      post(urlEqualTo("/object-store/ops/presigned-url"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""|{
                          |  "downloadUrl": "$url",
                          |  "contentLength": $size,
                          |  "contentMD5": "$md5Hash"
                          |}""".stripMargin)
        )
    )
    this
  }

  def givenObjectStorePresignedUrlFails(status: Int): ObjectStoreStubs = {
    stubFor(
      post(urlEqualTo("/object-store/ops/presigned-url"))
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )
    this
  }

}
