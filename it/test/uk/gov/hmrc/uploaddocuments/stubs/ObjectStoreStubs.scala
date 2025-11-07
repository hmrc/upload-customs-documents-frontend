/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.uploaddocuments.stubs

import com.github.tomakehurst.wiremock.client.WireMock.*
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
