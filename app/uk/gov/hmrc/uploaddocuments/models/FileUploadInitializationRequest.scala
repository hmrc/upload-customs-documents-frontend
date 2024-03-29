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

package uk.gov.hmrc.uploaddocuments.models

import play.api.libs.json.{Format, JsPath, Json, Writes}

final case class FileUploadInitializationRequest(config: FileUploadSessionConfig, existingFiles: Seq[UploadedFile])

object FileUploadInitializationRequest {
  implicit val formats: Format[FileUploadInitializationRequest] = Format(
    {
      for {
        config        <- (JsPath \ "config").read[FileUploadSessionConfig]
        existingFiles <- (JsPath \ "existingFiles").readWithDefault[Seq[UploadedFile]](Seq.empty)
      } yield FileUploadInitializationRequest(config, existingFiles)
    },
    Json.writes[FileUploadInitializationRequest]
  )

  //Used by logging as we can't leak the internal AWS Download URL to Kibana (even in QA/Staging)
  val writeNoDownloadUrl: Writes[FileUploadInitializationRequest] = Writes { model =>
    Json.toJson(FileUploadInitializationRequest(
      model.config,
      model.existingFiles.map(_.copy(downloadUrl = ""))
    ))(formats)
  }
}
