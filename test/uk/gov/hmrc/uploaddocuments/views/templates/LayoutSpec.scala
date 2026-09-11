/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.uploaddocuments.views.templates

import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.{FakeRequest, Injecting}
import play.twirl.api.Html
import uk.gov.hmrc.uploaddocuments.models.{CustomizedServiceContent, Features}
import uk.gov.hmrc.uploaddocuments.support.UnitSpec
import uk.gov.hmrc.uploaddocuments.views.html.templates.Layout

class LayoutSpec extends UnitSpec with GuiceOneAppPerSuite with Injecting {

  val layout: Layout = app.injector.instanceOf[Layout]

  given fakeRequest: FakeRequest[_]       = FakeRequest()
  given messages: Messages                = app.injector.instanceOf[MessagesApi].preferred(fakeRequest)
  given features: Features                = Features()
  given content: CustomizedServiceContent = CustomizedServiceContent()

  def render(timeout: Boolean = true, backLink: Option[String] = None) =
    Jsoup.parse(
      layout(pageTitle = Some("My Page"), timeout = timeout, backLink = backLink)(Html("<p id='c'>hi</p>")).body
    )

  "Layout" should {

    "render the page title with the service name and GOV.UK suffix" in {
      val doc = render()
      doc.title should include("My Page")
      doc.title should include(messages("site.serviceName"))
      doc.title should include(messages("site.govuk"))
    }

    "render the supplied content block" in {
      render().select("#c").text shouldBe "hi"
    }

    "render a back link with data-module=hmrc-back-link when no explicit URL is supplied" in {
      val link = render(backLink = None).select("#back-link")
      link.size shouldBe 1
      link.attr("data-module") shouldBe "hmrc-back-link"
      link.attr("href") shouldBe "#"
    }

    "render a back link with the explicit URL and no hmrc-back-link module when a URL is supplied" in {
      val link = render(backLink = Some("/previous")).select("#back-link")
      link.size shouldBe 1
      link.attr("href") shouldBe "/previous"
      link.attr("data-module") shouldBe ""
    }

    "not emit any custom stylesheet link" in {
      render().select("link[href*=stylesheets/application.css]").size shouldBe 0
      render().select("link[href*=stylesheets/print.css]").size shouldBe 0
    }

    "not include the Webpack application.min.js script" in {
      render().select("script[src*=javascripts/application.min.js]").size shouldBe 0
    }

    "render the hmrc-timeout-dialog meta when timeout is true" in {
      render(timeout = true).select("meta[name=hmrc-timeout-dialog]").size shouldBe 1
    }

    "not render the hmrc-timeout-dialog meta when timeout is false" in {
      render(timeout = false).select("meta[name=hmrc-timeout-dialog]").size shouldBe 0
    }
  }
}
