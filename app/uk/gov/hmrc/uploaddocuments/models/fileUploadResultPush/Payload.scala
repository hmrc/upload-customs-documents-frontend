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

package uk.gov.hmrc.uploaddocuments.models.fileUploadResultPush

import play.api.libs.json.{Format, JsValue, Json, Writes}
import uk.gov.hmrc.uploaddocuments.models._

case class Payload(nonce: Nonce, uploadedFiles: Seq[UploadedFile], cargo: Option[JsValue])

object Payload {

  def apply(request: FileUploadResultPushModel, baseUrl: String): Payload =
    Payload(
      request.nonce,
      request.uploadedFiles
        .map(file => file.copy(previewUrl = Some(baseUrl + filePreviewPathFor(file.upscanReference, file.fileName)))),
      request.context
    )

  private def filePreviewPathFor(reference: String, fileName: String): String =
    uk.gov.hmrc.uploaddocuments.controllers.routes.PreviewController
      .previewFileUploadByReference(reference, fileName)
      .url

  implicit val format: Format[Payload] = Json.format[Payload]

  //Used by logging as we can't leak the internal AWS Download URL to Kibana (even in QA/Staging)
  val writeNoDownloadUrl: Writes[Payload] = Writes { model =>
    Json.toJson(Payload(
      model.nonce,
      model.uploadedFiles.map(_.copy(downloadUrl = "")),
      model.cargo
    ))(format)
  }
}
