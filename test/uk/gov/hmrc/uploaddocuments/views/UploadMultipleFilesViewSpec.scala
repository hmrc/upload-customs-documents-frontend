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

package uk.gov.hmrc.uploaddocuments.views

import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Call
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.support.UnitSpec

import java.time.ZonedDateTime

class UploadMultipleFilesViewSpec extends UnitSpec with GuiceOneAppPerSuite with Injecting {

  val view = app.injector.instanceOf[uk.gov.hmrc.uploaddocuments.views.html.UploadMultipleFilesView]

  val fakeRequest                                = FakeRequest()
  given messages: Messages                       = app.injector.instanceOf[MessagesApi].preferred(fakeRequest)
  given features: Features                       = Features()
  given serviceContent: CustomizedServiceContent = CustomizedServiceContent()

  def render(
    files: FileUploads,
    uploadReq: Option[UploadRequest],
    anotherType: Option[String],
    content: CustomizedServiceContent = CustomizedServiceContent(),
    showMinimumError: Boolean = false,
    showFileRequiredError: Boolean = false
  ) =
    Jsoup.parse(
      view(
        maximumNumberOfFiles = 4,
        maximumFileSizeBytes = 9000000L,
        allowedFileTypesHint = "PDF, JPG, PNG",
        newFileDescription = Some("Calculation worksheet"),
        uploadRequest = uploadReq,
        fileUploads = files,
        removeFileCall = ref => Call("GET", s"/remove/$ref"),
        previewFileCall = (ref, name) => Call("GET", s"/preview/$ref/$name"),
        statusCall = ref => Call("GET", s"/file-verification/$ref/status"),
        continueAction = Call("GET", "/continue-to-host"),
        filePostedUrl = "/file-posted",
        uploadAnotherTypeUrl = anotherType,
        filePickerAcceptFilter = ".pdf,.jpg",
        backLink = None,
        showMinimumError = showMinimumError,
        showFileRequiredError = showFileRequiredError
      )(fakeRequest, messages, features, content).body
    )

  val acceptedFile = FileUpload.Accepted(
    Nonce(1),
    Timestamp.Any,
    "ref-a",
    "url",
    ZonedDateTime.parse("2020-01-01T00:00:00Z"),
    "sum",
    "invoice.pdf",
    "application/pdf",
    1,
    description = Some("Commercial invoice")
  )
  val postedFile = FileUpload.Posted(Nonce(2), Timestamp.Any, "ref-p")
  val rejectedFile =
    FileUpload.Rejected(Nonce(4), Timestamp.Any, "ref-r", S3UploadError("ref-r", "EntityTooLarge", "too big"))
  val duplicateFile =
    FileUpload.Duplicate(Nonce(5), Timestamp.Any, "ref-d", "checksum", "invoice.pdf", "invoice.pdf")
  val uploadReq = UploadRequest("https://s3", Map("k" -> "v"))

  "UploadMultipleFilesView" should {

    "enable the govuk file-upload drop-zone with i18n text" in {
      val doc = render(FileUploads(Seq.empty), Some(uploadReq), None)
      doc.select(".govuk-file-upload-wrapper[data-module=govuk-file-upload]").size shouldBe 1
      doc.select(".govuk-file-upload-wrapper input[type=file]").size shouldBe 1
      doc.body().html() should include("Choose file")
      doc.body().html() should include("No file chosen")
    }

    "render an Accepted row with filename, type and a green Uploaded tag" in {
      val doc = render(FileUploads(Seq(acceptedFile)), Some(uploadReq), Some("/another"))
      doc.select(".govuk-summary-list").text should include("invoice.pdf")
      doc.select(".govuk-summary-list").text should include("Commercial invoice")
      doc.select(".govuk-tag--green").text shouldBe "Uploaded"
      doc.select("[data-upload-status-row][data-status-url*=ref-a]").size shouldBe 1
      doc.select("a[href=/preview/ref-a/invoice.pdf]").size shouldBe 1
    }

    "include the upload-status poller script" in {
      render(FileUploads(Seq(postedFile)), Some(uploadReq), None)
        .select("script[src*=upload-status-poller.js]")
        .size shouldBe 1
    }

    "render a Posted row with the current type and a yellow Uploading tag, no preview link" in {
      val doc = render(FileUploads(Seq(postedFile)), Some(uploadReq), Some("/another"))
      doc.select(".govuk-tag--yellow").text shouldBe "Uploading"
      doc.text should include("Calculation worksheet")
      doc.select("a[href*=/preview/ref-p]").size shouldBe 0
    }

    "keep an errored file in the list with an inline error message, red border and no status tag" in {
      val doc = render(FileUploads(Seq(rejectedFile)), Some(uploadReq), None)
      val row = doc.select("#error-ref-r")
      row.size shouldBe 1
      row.hasClass("govuk-form-group--error") shouldBe true
      row.select(".govuk-error-message").text should include("The selected file must be smaller than")
      doc.select(".govuk-tag").size shouldBe 0
      doc.select("a[href=/remove/ref-r]").size shouldBe 1
    }

    "list every errored file in the error summary, each linking to its own row" in {
      val doc   = render(FileUploads(Seq(acceptedFile, rejectedFile, duplicateFile)), Some(uploadReq), None)
      val links = doc.select(".govuk-error-summary__list a")
      links.size shouldBe 2
      doc.select(".govuk-error-summary__list a[href=#error-ref-r]").text should include(
        "The selected file must be smaller than"
      )
      doc.select(".govuk-error-summary__list a[href=#error-ref-d]").text should include(
        "The selected file has already been uploaded"
      )
    }

    "not show an error message on the shared upload input when a file has errored" in {
      val doc = render(FileUploads(Seq(rejectedFile)), Some(uploadReq), None)
      doc.select("#file.govuk-file-upload--error").size shouldBe 0
    }

    "emit the noscript meta-refresh in the document head" in {
      val doc = render(FileUploads(Seq(postedFile)), Some(uploadReq), None)
      doc.head.select("noscript meta[http-equiv=refresh]").size shouldBe 1
      doc.body.select("noscript meta[http-equiv=refresh]").size shouldBe 0
    }

    "emit a noscript meta-refresh only when a non-ready row exists" in {
      render(FileUploads(Seq(postedFile)), Some(uploadReq), None)
        .select("noscript meta[http-equiv=refresh]")
        .size shouldBe 1
      render(FileUploads(Seq(acceptedFile)), Some(uploadReq), None)
        .select("noscript meta[http-equiv=refresh]")
        .size shouldBe 0
    }

    "render the secondary button only when uploadAnotherTypeUrl is defined" in {
      render(FileUploads(Seq(acceptedFile)), Some(uploadReq), Some("/another"))
        .select("a[href=/another]")
        .text should include("Upload another type of document")
      render(
        FileUploads(Seq(acceptedFile)),
        Some(uploadReq),
        None
      ).text should not include "Upload another type of document"
    }

    "hide the upload form when uploadRequest is None (max reached)" in {
      val doc = render(FileUploads(Seq(acceptedFile)), None, None)
      doc.select("input[type=file]").size shouldBe 0
    }

    "show the too-many-files message only when the form is hidden at max" in {
      // render(...) has maximumNumberOfFiles = 4
      val atMax = render(FileUploads(Seq.fill(4)(acceptedFile)), None, None)
      atMax.text should include(messages("view.upload-multiple-files.uploadMoreFilesThanLimit", 4))
      val belowMax = render(FileUploads(Seq(acceptedFile)), Some(uploadReq), None)
      belowMax.text should not include messages("view.upload-multiple-files.uploadMoreFilesThanLimit", 4)
    }

    "show the too-many-files message and hide the form when Posted uploads fill the remaining slots" in {
      // render(...) has maximumNumberOfFiles = 4
      val doc = render(FileUploads(Seq.fill(3)(acceptedFile) :+ postedFile), None, None)
      doc.select("input[type=file]").size shouldBe 0
      doc.text should include(messages("view.upload-multiple-files.uploadMoreFilesThanLimit", 4))
    }

    "use uploadAnotherTypeText when set" in {
      val customContent = CustomizedServiceContent(uploadAnotherTypeText = Some("Add a different document"))
      render(FileUploads(Seq(acceptedFile)), Some(uploadReq), Some("/another"), customContent)
        .select("a[href=/another]")
        .text shouldBe "Add a different document"
    }

    "fall back to the default message when uploadAnotherTypeText is None" in {
      render(FileUploads(Seq(acceptedFile)), Some(uploadReq), Some("/another"))
        .select("a[href=/another]")
        .text should include("Upload another type of document")
    }

    "hide the file name, not the upscan reference, in the remove link's accessible label" in {
      val doc = render(FileUploads(Seq(acceptedFile)), Some(uploadReq), None)
      doc.select("a[href=/remove/ref-a] .govuk-visually-hidden").text shouldBe "invoice.pdf"
      doc.select("a[href=/remove/ref-a]").text should not include "ref-a"
    }

    "render corrected the size and types of a file" in {
      val doc = render(FileUploads(Seq(acceptedFile)), Some(uploadReq), None)
      doc.text should include("Each file can be 9 MB or smaller.")
      doc.text should include("Acceptable file types: PDF, JPG, PNG.")
    }

    "prevent XSS by escaping a filename with html special characters" in {
      val maliciousFile = FileUpload.Accepted(
        Nonce(9),
        Timestamp.Any,
        "ref-xss",
        "url",
        ZonedDateTime.parse("2020-01-01T00:00:00Z"),
        "sum",
        "a<script>b</script>.pdf",
        "application/pdf",
        1
      )
      val doc = render(FileUploads(Seq(maliciousFile)), Some(uploadReq), None)
      doc.select(".govuk-summary-list script").size shouldBe 0
      doc.body().html() should include("a&lt;script&gt;")
      doc.select("a[href*=/preview/ref-xss]").first().text() should include("a<script>")
    }

    "prefix the browser title with Error: only when a file has errored" in {
      render(FileUploads(Seq(rejectedFile)), Some(uploadReq), None).title should startWith(
        messages("error.browser.title.prefix")
      )
      render(FileUploads(Seq(acceptedFile)), Some(uploadReq), None).title should not startWith messages(
        "error.browser.title.prefix"
      )
    }

    "render the minimum-files error in the summary and on the upload input when showMinimumError is set" in {
      val doc = render(FileUploads(Seq.empty), Some(uploadReq), None, showMinimumError = true)
      doc.select(".govuk-error-summary__list a[href=#file]").text should include("Upload a supporting document")
      doc.select("#file.govuk-file-upload--error").size shouldBe 1
      doc.title should startWith(messages("error.browser.title.prefix"))
    }

    "use fileUploadRequiredError override for the minimum-files error when set" in {
      val custom = CustomizedServiceContent(fileUploadRequiredError = Some("Please add a document first"))
      val doc    = render(FileUploads(Seq.empty), Some(uploadReq), None, custom, showMinimumError = true)
      doc.select(".govuk-error-summary__list a[href=#file]").text shouldBe "Please add a document first"
    }

    "not render the minimum-files error when showMinimumError is false" in {
      val doc = render(FileUploads(Seq.empty), Some(uploadReq), None)
      doc.select(".govuk-error-summary__list a[href=#file]").size shouldBe 0
      doc.select("#file.govuk-file-upload--error").size shouldBe 0
    }

    "render the file-required error in the summary and on the upload input when showFileRequiredError is set" in {
      val doc = render(FileUploads(Seq.empty), Some(uploadReq), None, showFileRequiredError = true)
      doc.select(".govuk-error-summary__list a[href=#file]").text should include("Upload a supporting document")
      doc.select("#file.govuk-file-upload--error").size shouldBe 1
      doc.title should startWith(messages("error.browser.title.prefix"))
    }

    "use fileUploadRequiredError override for the file-required error when set" in {
      val custom = CustomizedServiceContent(fileUploadRequiredError = Some("Please add a document first"))
      val doc    = render(FileUploads(Seq.empty), Some(uploadReq), None, custom, showFileRequiredError = true)
      doc.select(".govuk-error-summary__list a[href=#file]").text shouldBe "Please add a document first"
    }

    "render the file-required error as the highlighted box, not an errored row" in {
      val doc = render(FileUploads(Seq.empty), Some(uploadReq), None, showFileRequiredError = true)
      doc.select("#file.govuk-file-upload--error").size shouldBe 1
      doc.select(".govuk-summary-list").size shouldBe 0
    }
  }
}
