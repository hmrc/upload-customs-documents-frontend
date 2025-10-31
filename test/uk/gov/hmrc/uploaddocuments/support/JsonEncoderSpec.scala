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

import uk.gov.hmrc.uploaddocuments.wiring.JsonEncoder
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.TextNode

import scala.jdk.CollectionConverters
import com.fasterxml.jackson.databind.JsonNode

class JsonEncoderSpec extends UnitSpec with LogCapturing {

  val encoder = new JsonEncoder()
  val jnf     = JsonNodeFactory.instance

  "JsonEncoder" should {
    "decode empty message into a text node" in {
      val node = new ObjectNode(jnf)
      encoder.decodeMessage(node, "")
      node.get("message") shouldBe new TextNode("")
    }

    "decode non-json message into a text node" in {
      val node = new ObjectNode(jnf)
      encoder.decodeMessage(node, "foobar")
      node.get("message") shouldBe new TextNode("foobar")
    }

    "decode json message into an object node" in {
      val node = new ObjectNode(jnf)
      encoder.decodeMessage(node, """json{"foo":"bar"}""")
      node.get("ucdf") shouldBe new ObjectNode(
        jnf,
        CollectionConverters.MapHasAsJava[String, JsonNode](Map("foo" -> new TextNode("bar"))).asJava
      )
      node.get("message") shouldBe new TextNode("""{"foo":"bar"}""")
    }

    "fallback to decode json message into a text node" in {
      val node    = new ObjectNode(jnf)
      val message = """json{"foo":"bar}"""
      encoder.decodeMessage(node, message)
      node.get("message") shouldBe new TextNode(message)
    }
  }
}
