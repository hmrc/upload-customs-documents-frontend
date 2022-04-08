package uk.gov.hmrc.uploaddocuments.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.uploaddocuments.support.WireMockSupport
import uk.gov.hmrc.uploaddocuments.models.UploadRequest

trait UpscanInitiateStubs {
  me: WireMockSupport =>

  val testUploadRequest: UploadRequest =
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
        "x-amz-meta-callback-url" -> "https://myservice.com/callback",
        "x-amz-signature"         -> "xxxx",
        "success_action_redirect" -> "https://myservice.com/nextPage",
        "error_action_redirect"   -> "https://myservice.com/errorPage"
      )
    )

  def givenUpscanInitiateSucceeds(callbackUrl: String, userAgent: String): StubMapping =
    stubFor(
      post(urlEqualTo(s"/upscan/v2/initiate"))
        .withHeader("User-Agent", containing("upload-customs-documents-frontend"))
        .withHeader(HeaderNames.CONTENT_TYPE, containing("application/json"))
        .withHeader("User-Agent", equalTo(userAgent))
        .withRequestBody(
          matchingJsonPath("callbackUrl", containing(callbackUrl))
        )
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(s"""{
              |    "reference": "11370e18-6e24-453e-b45a-76d3e32ea33d",
              |    "uploadRequest": {
              |        "href": "https://bucketName.s3.eu-west-2.amazonaws.com",
              |        "fields": {
              |            "Content-Type": "application/xml",
              |            "acl": "private",
              |            "key": "xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
              |            "policy": "xxxxxxxx==",
              |            "x-amz-algorithm": "AWS4-HMAC-SHA256",
              |            "x-amz-credential": "ASIAxxxxxxxxx/20180202/eu-west-2/s3/aws4_request",
              |            "x-amz-date": "yyyyMMddThhmmssZ",
              |            "x-amz-meta-callback-url": "$callbackUrl",
              |            "x-amz-signature": "xxxx",
              |            "success_action_redirect": "https://myservice.com/nextPage",
              |            "error_action_redirect": "https://myservice.com/errorPage"
              |        }
              |    }
              |}""".stripMargin)
        )
    )

}
