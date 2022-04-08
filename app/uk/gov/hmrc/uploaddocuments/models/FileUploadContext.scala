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

import play.api.libs.json.{Format, JsString, JsSuccess, Json, Reads, Writes}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import play.api.mvc.Headers
import play.api.mvc.RequestHeader
import java.util.UUID
import play.api.i18n.Messages
import uk.gov.hmrc.uploaddocuments.support.EnhancedMessages

final case class FileUploadContext(
  config: FileUploadSessionConfig,
  hostService: HostService = HostService.Any
) {
  def isValid: Boolean = config.isValid && hostService.userAgent.nonEmpty

  implicit val content: CustomizedServiceContent = config.content
  implicit val features: Features = config.features

  def messages(implicit m: Messages): Messages =
    if (config.content.yesNoQuestionRequiredError.isDefined)
      new EnhancedMessages(m, Map("error.choice.required" -> config.content.yesNoQuestionRequiredError.getOrElse("")))
    else m
}

sealed trait HostService {
  def userAgent: String
  def populate(hc: HeaderCarrier): HeaderCarrier
}

object HostService {

  final case class InitializationRequestHeaders(
    private val headers: Seq[(String, String)]
  ) extends HostService {

    final override val userAgent: String =
      headers
        .find(_._1 == play.api.http.HeaderNames.USER_AGENT)
        .map(_._2)
        .getOrElse("")

    final override def populate(hc: HeaderCarrier): HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSession(Headers(headers: _*), None)

    final override def equals(obj: scala.Any): Boolean =
      if (obj.isInstanceOf[Any]) true
      else if (obj.isInstanceOf[InitializationRequestHeaders])
        obj.asInstanceOf[InitializationRequestHeaders].headers.equals(this.headers)
      else false

    final override val hashCode: Int = 0
    final override val toString: String = s"HostService.InitializationRequestHeaders(...)"
  }

  object InitializationRequestHeaders {

    implicit val reads: Reads[InitializationRequestHeaders] =
      Json.reads[InitializationRequestHeaders]

    implicit val writes: Writes[InitializationRequestHeaders] =
      Json.writes[InitializationRequestHeaders]
  }

  object Any extends HostService {
    final override val userAgent: String = UUID.randomUUID.toString
    final override def populate(hc: HeaderCarrier): HeaderCarrier = hc
    final override def equals(obj: Any): Boolean = if (obj.isInstanceOf[HostService]) true else false
    final override val hashCode: Int = 0
    final override val toString: String = "HostService.Any"
  }

  def from(rh: RequestHeader): HostService = {
    val headers = HeaderCarrierConverter
      .fromRequest(rh)
      .headers(HeaderNames.explicitlyIncludedHeaders) :+
      (play.api.http.HeaderNames.USER_AGENT -> rh.headers.get(play.api.http.HeaderNames.USER_AGENT).getOrElse(""))

    InitializationRequestHeaders(headers)
  }

  implicit val format: Format[HostService] =
    Format(
      Reads(value => InitializationRequestHeaders.reads.reads(value).orElse(JsSuccess(Any))),
      Writes.apply {
        case value: InitializationRequestHeaders => InitializationRequestHeaders.writes.writes(value)
        case _                                   => JsString("Any")
      }
    )
}

object FileUploadContext {
  implicit val format: Format[FileUploadContext] = Json.format[FileUploadContext]
}
