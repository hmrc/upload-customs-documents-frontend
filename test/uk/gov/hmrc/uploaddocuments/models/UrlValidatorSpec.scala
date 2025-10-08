/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.uploaddocuments.models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class UrlValidatorSpec extends AnyWordSpec with Matchers {

  "UrlValidatorSpec" should {
    "validate frontend url is valid" in {
      UrlValidator.isValidFrontendUrl("https://www.gov.uk") shouldBe true
      UrlValidator.isValidFrontendUrl("https://www.gov.uk/foo") shouldBe true
      UrlValidator.isValidFrontendUrl("https://www.gov.uk/foo/bar") shouldBe true
      UrlValidator.isValidFrontendUrl("http://localhost:10110") shouldBe true
      UrlValidator.isValidFrontendUrl("http://localhost:10110/foo") shouldBe true
      UrlValidator.isValidFrontendUrl("http://localhost:10110/foo/bar") shouldBe true
    }

    "validate frontend url is invalid" in {
      UrlValidator.isValidFrontendUrl("http://www.gov.uk") shouldBe false
      UrlValidator.isValidFrontendUrl("http://www.gov.uk/foo") shouldBe false
      UrlValidator.isValidFrontendUrl("http://www.gov.uk/foo/bar") shouldBe false
      UrlValidator.isValidFrontendUrl("www.gov.uk/foo/bar") shouldBe false
      UrlValidator.isValidFrontendUrl("/foo/bar") shouldBe false
      UrlValidator.isValidFrontendUrl("") shouldBe false
    }

    "validate callback url is valid" in {
      UrlValidator.isValidCallbackUrl("https://foo.protected.mdtp") shouldBe true
      UrlValidator.isValidCallbackUrl("https://foo.protected.mdtp/foo") shouldBe true
      UrlValidator.isValidCallbackUrl("https://foo.protected.mdtp/foo/bar") shouldBe true
      UrlValidator.isValidCallbackUrl("http://localhost:9570") shouldBe true
      UrlValidator.isValidCallbackUrl("http://localhost:9570/foo") shouldBe true
      UrlValidator.isValidCallbackUrl("http://localhost:9570/foo/bar") shouldBe true
    }

    "validate callback url is invalid" in {
      UrlValidator.isValidCallbackUrl("http://foo.protected.mdtp") shouldBe false
      UrlValidator.isValidCallbackUrl("http://foo.protected.mdtp/foo") shouldBe false
      UrlValidator.isValidCallbackUrl("http://foo.protected.mdtp/foo/bar") shouldBe false
      UrlValidator.isValidCallbackUrl("foo.protected.mdtp/foo/bar") shouldBe false
      UrlValidator.isValidCallbackUrl("/foo/bar") shouldBe false
      UrlValidator.isValidCallbackUrl("") shouldBe false
    }

    "validate is relative url" in {
      UrlValidator.isReleativeUrl("/foo/bar") shouldBe true
      UrlValidator.isReleativeUrl("http://foo.protected.mdtp/foo/bar") shouldBe false
    }
  }
}
