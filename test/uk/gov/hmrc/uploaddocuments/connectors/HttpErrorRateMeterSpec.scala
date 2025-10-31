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

package uk.gov.hmrc.uploaddocuments.connectors

import com.codahale.metrics.MetricRegistry
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.http.*

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.uploaddocuments.support.UnitSpec

class HttpErrorRateMeterSpec extends UnitSpec {

  private trait TestHarness extends HttpErrorRateMeter {
    override val kenshooRegistry: MetricRegistry = new MetricRegistry()
  }

  private def meterCount(registry: MetricRegistry, name: String): Long =
    Option(registry.getMeters.get(name)).map(_.getCount).getOrElse(0L)

  "meterName" should {
    "return 4xx meter name for status < 500" in new TestHarness {
      meterName("upstream-a", 400) shouldBe "Http4xxErrorCount-upstream-a"
      meterName("upstream-b", 418) shouldBe "Http4xxErrorCount-upstream-b"
      meterName("upstream-c", 499) shouldBe "Http4xxErrorCount-upstream-c"
    }

    "return 5xx meter name for status >= 500" in new TestHarness {
      meterName("svc-a", 500) shouldBe "Http5xxErrorCount-svc-a"
      meterName("svc-b", 503) shouldBe "Http5xxErrorCount-svc-b"
      meterName("svc-c", 599) shouldBe "Http5xxErrorCount-svc-c"
    }
  }

  "countErrors" should {
    "not record anything for successful 2xx/3xx responses" in new TestHarness {
      val f = Future.successful(HttpResponse(200, "", Map.empty))
      val g = countErrors("test-service")(f)
      await(g).status shouldBe 200
      kenshooRegistry.getMeters.isEmpty shouldBe true
    }

    "record 4xx meter when response is successful with 4xx status" in new TestHarness {
      val f = Future.successful(HttpResponse(404, "", Map.empty))
      val g = countErrors("file-store")(f)
      await(g).status shouldBe 404

      val name = "Http4xxErrorCount-file-store"
      meterCount(kenshooRegistry, name) shouldBe 1L
    }

    "record 5xx meter when response is successful with 5xx status" in new TestHarness {
      val f = Future.successful(HttpResponse(503, "", Map.empty))
      val g = countErrors("file-store")(f)
      await(g).status shouldBe 503

      val name = "Http5xxErrorCount-file-store"
      meterCount(kenshooRegistry, name) shouldBe 1L
    }

    "record 4xx meter when future fails with UpstreamErrorResponse 4xx" in new TestHarness {
      val ex = UpstreamErrorResponse("boom", 429, 429, Map.empty)
      val f  = Future.failed[HttpResponse](ex)
      val g  = countErrors("file-store")(f)

      // swallow failure to allow assertions on meter
      Try(await(g)).isFailure shouldBe true

      val name = "Http4xxErrorCount-file-store"
      meterCount(kenshooRegistry, name) shouldBe 1L
    }

    "record 5xx meter when future fails with UpstreamErrorResponse 5xx" in new TestHarness {
      val ex = UpstreamErrorResponse("boom", 502, 502, Map.empty)
      val f  = Future.failed[HttpResponse](ex)
      val g  = countErrors("document-service")(f)
      Try(await(g)).isFailure shouldBe true

      val name = "Http5xxErrorCount-document-service"
      meterCount(kenshooRegistry, name) shouldBe 1L
    }

    "record 4xx meter when future fails with HttpException" in new TestHarness {
      val ex = new HttpException("teapot", 418)
      val f  = Future.failed[HttpResponse](ex)
      val g  = countErrors("teapot")(f)
      Try(await(g)).isFailure shouldBe true

      val name = "Http4xxErrorCount-teapot"
      meterCount(kenshooRegistry, name) shouldBe 1L
    }

    "record 5xx meter when future fails with other Throwable" in new TestHarness {
      val ex = new RuntimeException("unexpected")
      val f  = Future.failed[HttpResponse](ex)
      val g  = countErrors("unknown")(f)
      Try(await(g)).isFailure shouldBe true

      val name = "Http5xxErrorCount-unknown"
      meterCount(kenshooRegistry, name) shouldBe 1L
    }
  }
}
