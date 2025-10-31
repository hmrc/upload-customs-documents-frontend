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

package uk.gov.hmrc.uploaddocuments.views

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class RichOptionStringSpec extends AnyWordSpec with Matchers {

  import CommonUtilsHelper.RichOptionString

  "RichOptionString.mapNonEmpty" should {
    "map Some(nonEmpty) using the provided function" in {
      Some("abc").mapNonEmpty(_.length) shouldBe Some(3)
    }

    "return None for Some(\"\")" in {
      Some("").mapNonEmpty(_.length) shouldBe None
    }

    "return None for None" in {
      (None: Option[String]).mapNonEmpty(_.length) shouldBe None
    }
  }

  "RichOptionString.foldNonEmpty" should {
    "apply the function for Some(nonEmpty)" in {
      Some("abcd").foldNonEmpty(0)(_.length) shouldBe 4
    }

    "return default for Some(\"\")" in {
      Some("").foldNonEmpty(42)(_.length) shouldBe 42
    }

    "return default for None" in {
      (None: Option[String]).foldNonEmpty("default")(_.toUpperCase) shouldBe "default"
    }
  }

  "RichOptionString.getNonEmptyOrElse" should {
    "return the original string for Some(nonEmpty)" in {
      Some("value").getNonEmptyOrElse("default") shouldBe "value"
    }

    "return default for Some(\"\")" in {
      Some("").getNonEmptyOrElse("default") shouldBe "default"
    }

    "return default for None" in {
      (None: Option[String]).getNonEmptyOrElse("default") shouldBe "default"
    }

    "allow non-String default due to type widening" in {
      val result: Any = (None: Option[String]).getNonEmptyOrElse(99)
      result shouldBe 99
    }
  }
}
