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

package uk.gov.hmrc.uploaddocuments.wiring

import java.nio.charset.StandardCharsets

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import org.scalamock.scalatest.MockFactory
import play.api.libs.json.{JsObject, JsString, Json}
import uk.gov.hmrc.uploaddocuments.support.UnitSpec

class JsonEncoderSpec extends UnitSpec with MockFactory {

  trait Fixture {
    final val underTest = new JsonEncoder()
  }

  "JsonEncoder.appName" should {
    "return value from config when appName is set" in new Fixture {
      val original = System.getProperty("appName")
      try {
        System.setProperty("appName", "ucdf-frontend-test")
        underTest.appName shouldBe "ucdf-frontend-test"
      } finally if (original == null) System.clearProperty("appName") else System.setProperty("appName", original)
    }
  }

  "JsonEncoder.decodeMessage" should {
    "populate ucdf and message for valid json{...} input" in new Fixture {
      val node        = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode()
      val jsonPayload = "json{\"a\":1, \"b\":\"x\"}"

      underTest.decodeMessage(node, jsonPayload)

      node.get("message").asText() shouldBe "{\"a\":1, \"b\":\"x\"}"
      node.get("ucdf").isObject shouldBe true
      node.get("ucdf").get("a").asInt() shouldBe 1
      node.get("ucdf").get("b").asText() shouldBe "x"
    }

    "fallback to original message when json prefix but invalid JSON" in new Fixture {
      val node = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode()
      val bad  = "json{not-a-json}"

      underTest.decodeMessage(node, bad)

      node.get("message").asText() shouldBe bad
      node.get("ucdf") shouldBe null
    }

    "set message field when no json prefix" in new Fixture {
      val node = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode()
      val msg  = "plain text message"

      underTest.decodeMessage(node, msg)

      node.get("message").asText() shouldBe msg
      node.get("ucdf") shouldBe null
    }
  }

  "JsonEncoder headerBytes/footerBytes" should {
    "return system line separator bytes" in new Fixture {
      val sep = System.lineSeparator().getBytes(StandardCharsets.UTF_8)
      underTest.headerBytes() shouldBe sep
      underTest.footerBytes() shouldBe sep
    }
  }

  "JsonEncoder.encode" should {
    "produce JSON with expected fields and MDC and simple message" in new Fixture {
      val event = mock[ILoggingEvent]

      (event.getFormattedMessage _).expects().returning("hello")
      (event.getTimeStamp _).expects().returning(1730310000000L)
      (event.getLoggerName _).expects().returning("uk.gov.hmrc.TestLogger")
      (event.getThreadName _).expects().returning("test-thread")
      (event.getLevel _).expects().returning(Level.INFO)
      (event.getThrowableProxy _).expects().returning(null)

      val mdc = new java.util.HashMap[String, String]()
      mdc.put("X-Request-ID", "abc123")
      (event.getMDCPropertyMap _).expects().returning(mdc)

      val bytes = underTest.encode(event)
      val text  = new String(bytes, StandardCharsets.UTF_8)

      text.endsWith(System.lineSeparator()) shouldBe true

      val json = Json.parse(text.trim).as[JsObject]

      (json \ "message").as[JsString].value shouldBe "hello"
      (json \ "logger").as[JsString].value shouldBe "uk.gov.hmrc.TestLogger"
      (json \ "thread").as[JsString].value shouldBe "test-thread"
      (json \ "level").as[JsString].value shouldBe "INFO"

      // keys from MDC are lowercased by encoder
      (json \ "x-request-id").as[JsString].value shouldBe "abc123"

      // presence checks (values environment-dependent)
      (json \ "app").isDefined shouldBe true
      (json \ "hostname").isDefined shouldBe true
      (json \ "timestamp").isDefined shouldBe true
    }

    "embed ucdf when message starts with json{...}" in new Fixture {
      val event = mock[ILoggingEvent]

      (event.getFormattedMessage _).expects().returning("json{\"a\":1}")
      (event.getTimeStamp _).expects().returning(1730310000000L)
      (event.getLoggerName _).expects().returning("uk.gov.hmrc.TestLogger")
      (event.getThreadName _).expects().returning("test-thread")
      (event.getLevel _).expects().returning(Level.DEBUG)
      (event.getThrowableProxy _).expects().returning(null)

      val mdc = new java.util.HashMap[String, String]()
      (event.getMDCPropertyMap _).expects().returning(mdc)

      val text = new String(underTest.encode(event), StandardCharsets.UTF_8).trim
      val json = Json.parse(text).as[JsObject]

      (json \ "message").as[JsString].value shouldBe "{\"a\":1}"
      (json \ "ucdf").isDefined shouldBe true
      (json \ "ucdf" \ "a").as[Int] shouldBe 1
    }
  }
}
