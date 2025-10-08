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

package uk.gov.hmrc.uploaddocuments.support

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.MessagesImpl
import play.api.i18n.Lang
import play.api.i18n.DefaultMessagesApi

class EnhancedMessagesSpec extends AnyWordSpec with Matchers {

  val messages = MessagesImpl(
    Lang("en"),
    DefaultMessagesApi(
      messages = Map("en" -> Map("bar" -> "foo {2}"))
    )
  )

  val underTest = new EnhancedMessages(messages, Map("foo" -> "bar {0} {1} {2}"))

  "EnhancedMessages" should {
    "translate a message" in {
      underTest.translate("foo", Seq("a", "b", "c")) shouldBe Some("bar a b c")
      underTest.translate("bar", Seq("a", "b", "c")) shouldBe Some("foo c")
    }

    "get a message from a map" in {
      underTest("bar") shouldBe "foo {2}"
      underTest("bar", "a", "b", "c") shouldBe "foo c"
      underTest("foo", "a", "b", "c") shouldBe "bar a b c"
    }

    "get first defined message from a map" in {
      underTest.messages(Seq("bar", "foo")) shouldBe "foo {2}"
      underTest.messages(Seq("bar", "foo"), "a", "b", "c") shouldBe "foo c"
      underTest.messages(Seq("baz", "bar", "foo"), "a", "b", "c") shouldBe "foo c"
      underTest.messages(Seq("foo", "bar"), "a", "b", "c") shouldBe "bar a b c"
      underTest.messages(Seq("baz", "foo", "bar"), "a", "b", "c") shouldBe "bar a b c"
      underTest.messages(Seq("baz", "zoo", "zaz"), "a", "b", "c") shouldBe "zaz"
      intercept[Exception] {
        underTest.messages(Seq.empty, "a")
      }
    }

    "check if a message is defined" in {
      underTest.isDefinedAt("bar") shouldBe true
      underTest.isDefinedAt("foo") shouldBe true
      underTest.isDefinedAt("baz") shouldBe false
      underTest.isDefinedAt("zoo") shouldBe false
      underTest.isDefinedAt("zaz") shouldBe false
    }

    "convert to java messages" in {
      underTest.asJava.at("bar") shouldBe "foo {2}"
      underTest.asJava.at("bar", "a", "b", "c") shouldBe "foo c"
      underTest.asJava.at("foo", "a", "b", "c") shouldBe "foo"
    }
  }
}
