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

import uk.gov.hmrc.uploaddocuments.support.UnitSpec
import uk.gov.hmrc.uploaddocuments.support.HtmlCleaner

class HtmlCleanerSpec extends UnitSpec {

  "HtmlCleaner" should {
    "allow https hyperlinks and set target and rel attributes" in {
      HtmlCleaner.cleanBlock("") shouldBe ""
      HtmlCleaner.cleanBlock("a bc") shouldBe "a bc"

      HtmlCleaner.cleanBlock(
        """<a href="https://gov.uk" rel="noreferrer noopener" target="_blank">test</a>"""
      ) shouldBe """<a href="https://gov.uk" rel="noreferrer noopener" target="_blank">test</a>"""

      HtmlCleaner.cleanBlock(
        """<a href="https://gov.uk">test</a>"""
      ) shouldBe """<a href="https://gov.uk" rel="noreferrer noopener" target="_blank">test</a>"""

      HtmlCleaner.cleanBlock(
        """<a href="http://gov.uk">test</a>"""
      ) shouldBe """<a href="http://gov.uk" rel="noreferrer noopener" target="_blank">test</a>"""

      HtmlCleaner.cleanBlock(
        """<a href="ftp://gov.uk">test</a>"""
      ) shouldBe """<a rel="noreferrer noopener" target="_blank">test</a>"""
    }

  }
}
