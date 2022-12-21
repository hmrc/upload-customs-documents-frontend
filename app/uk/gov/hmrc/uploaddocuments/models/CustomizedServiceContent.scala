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

package uk.gov.hmrc.uploaddocuments.models

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.uploaddocuments.models.CustomizedServiceContent._
import uk.gov.hmrc.uploaddocuments.support.HtmlCleaner

final case class CustomizedServiceContent(
  serviceName: Option[String] = None,
  title: Option[String] = None,
  private val descriptionHtml: Option[String] = None,
  serviceUrl: Option[String] = None,
  accessibilityStatementUrl: Option[String] = None,
  phaseBanner: Option[PhaseBanner] = None,
  phaseBannerUrl: Option[String] = None,
  userResearchBannerUrl: Option[String] = None,
  signOutUrl: Option[String] = None,
  timedOutUrl: Option[String] = None,
  keepAliveUrl: Option[String] = None,
  timeoutSeconds: Option[Int] = None,
  countdownSeconds: Option[Int] = None,
  pageTitleClasses: Option[String] = None,
  allowedFilesTypesHint: Option[String] = None,
  contactFrontendServiceId: Option[String] = None,
  fileUploadedProgressBarLabel: Option[String] = None,
  chooseFirstFileLabel: Option[String] = None,
  chooseNextFileLabel: Option[String] = None,
  addAnotherDocumentButtonText: Option[String] = None,
  yesNoQuestionText: Option[String] = None,
  yesNoQuestionRequiredError: Option[String] = None
) {
  lazy val safeDescriptionHtml: Option[String] = descriptionHtml.map(HtmlCleaner.cleanBlock)
}

object CustomizedServiceContent {

  sealed trait PhaseBanner
  object PhaseBanner extends EnumerationFormats[PhaseBanner] {
    case object alpha extends PhaseBanner
    case object beta extends PhaseBanner
    override val values: Set[PhaseBanner] = Set(alpha, beta)
  }

  implicit val format: Format[CustomizedServiceContent] = Json.format[CustomizedServiceContent]

}
