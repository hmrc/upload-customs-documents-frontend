/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.uploaddocuments.services

import com.typesafe.config.ConfigFactory
import org.apache.commons.codec.binary.Base64
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.uploaddocuments.support.UnitSpec

import java.nio.charset.StandardCharsets
import scala.util.Random

class EncryptionSpec extends UnitSpec with ScalaCheckPropertyChecks {

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 1000)

  def testKey: String = {
    val bytes: Array[Byte] = Array.ofDim[Byte](32)
    Random.nextBytes(bytes)
    val base64Key = new String(Base64.encodeBase64(bytes), StandardCharsets.UTF_8)
    base64Key
  }

  case class Wrapper(data1: String, data2: Int)
  object Wrapper {
    implicit val format: Format[Wrapper] = Json.format[Wrapper]
  }

  "Encryption" should {
    "encrypt and decrypt values using provided key" in {
      val keyProvider = KeyProvider(testKey)
      forAll(Gen.alphaNumStr, Gen.chooseNum(Integer.MIN_VALUE, Integer.MAX_VALUE)) { (data1: String, data2: Int) =>
        val original = Wrapper(data1, data2)
        val encrypted: String = Encryption.encrypt(original, keyProvider)
        val decrypted: Wrapper = Encryption.decrypt[Wrapper](encrypted, keyProvider)
        encrypted should not be decrypted
        decrypted shouldBe original
      }
    }

    "encrypt and decrypt values using provided key sequence" in {
      forAll(Gen.alphaNumStr, Gen.chooseNum(Integer.MIN_VALUE, Integer.MAX_VALUE)) { (data1: String, data2: Int) =>
        val keyUsedForEncryption = testKey
        val keyProvider1 = KeyProvider(Seq(keyUsedForEncryption, testKey, testKey))
        val keyProvider2 = KeyProvider(Seq(testKey, testKey, testKey, keyUsedForEncryption, testKey, testKey))
        val original = Wrapper(data1, data2)
        val encrypted: String = Encryption.encrypt(original, keyProvider1)
        val decrypted: Wrapper = Encryption.decrypt[Wrapper](encrypted, keyProvider2)
        encrypted should not be decrypted
        decrypted shouldBe original
      }
    }
  }

  "KeyProviders" should {
    "apply context to existing key" in {
      forAll(Gen.alphaNumStr, Gen.alphaNumStr) { (data, context) =>
        val keyProvider1 = KeyProvider(testKey)
        val e1 = Encryption.encrypt(data, keyProvider1)
        val keyProvider2 = KeyProvider(keyProvider1, Some(context))
        val e2 = Encryption.encrypt(data, keyProvider2)
        e1 should not be e2
        val d2 = Encryption.decrypt[String](e2, keyProvider2)
        d2 shouldBe data
        val keyProvider3 = KeyProvider(keyProvider1, Some(context))
        val e3 = Encryption.encrypt(data, keyProvider3)
        e2 shouldBe e3
        val d3 = Encryption.decrypt[String](e3, keyProvider3)
        d3 shouldBe data
        val keyProvider4 = KeyProvider(keyProvider1, None)
        val e4 = Encryption.encrypt(data, keyProvider4)
        e1 shouldBe e4
        val d4 = Encryption.decrypt[String](e1, keyProvider4)
        d4 shouldBe data
      }
    }

    "apply context to existing key sequence" in {
      forAll(Gen.alphaNumStr, Gen.alphaNumStr) { (data, context) =>
        val keyProvider1 = KeyProvider(Seq(testKey, testKey, testKey))
        val e1 = Encryption.encrypt(data, keyProvider1)
        val keyProvider2 = KeyProvider(keyProvider1, Some(context))
        val e2 = Encryption.encrypt(data, keyProvider2)
        e1 should not be e2
        val d2 = Encryption.decrypt[String](e2, keyProvider2)
        d2 shouldBe data
        val keyProvider3 = KeyProvider(keyProvider1, Some(context))
        val e3 = Encryption.encrypt(data, keyProvider3)
        e2 shouldBe e3
        val d3 = Encryption.decrypt[String](e3, keyProvider3)
        d3 shouldBe data
        val keyProvider4 = KeyProvider(keyProvider1, None)
        val e4 = Encryption.encrypt(data, keyProvider4)
        e1 shouldBe e4
        val d4 = Encryption.decrypt[String](e1, keyProvider4)
        d4 shouldBe data
      }
    }

    "read the AES key from base64 representation" in {
      KeyProvider("+enIUge8OJhNe6JB50+rMJZb/3kTYJRyve3Ydj+9WPo=").keys.head.getAlgorithm() shouldBe "AES"
    }

    "read the keys from the config" in {
      val config = ConfigFactory.parseString("""
        | json.encryption.key = "kaTHT1YluhyGrp+5jlCGbONQoKOt6Y1o6FJ3pirxl7o="
        | json.encryption.previousKeys = [
        |   "Hu4nhJYqdm6rZZjtXos+rWNYnLZ7aZ25NcymJn/ZYTU=",
        |   "5eIgUpCeGOJ9IAlAj3EsQEV3g/H+rLaSOAZCh2aseL4=",
        |   "GsUnHUv/aXmZ8VX3+OJ00BgD7fanG9IoG8cSccmMXFY="
        | ]
      """.stripMargin)
      KeyProvider(config).keys shouldBe KeyProvider(
        Seq(
          "kaTHT1YluhyGrp+5jlCGbONQoKOt6Y1o6FJ3pirxl7o=",
          "Hu4nhJYqdm6rZZjtXos+rWNYnLZ7aZ25NcymJn/ZYTU=",
          "5eIgUpCeGOJ9IAlAj3EsQEV3g/H+rLaSOAZCh2aseL4=",
          "GsUnHUv/aXmZ8VX3+OJ00BgD7fanG9IoG8cSccmMXFY="
        )
      ).keys
    }
  }
}
