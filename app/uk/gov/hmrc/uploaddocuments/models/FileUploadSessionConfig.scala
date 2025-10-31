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

import play.api.libs.functional.syntax.*
import play.api.libs.json.{Format, JsPath, JsValue}

import UrlValidator.*

final case class FileUploadSessionConfig(
  nonce: Nonce, // unique secret shared by the host and upload microservices
  continueUrl: String, // url to continue after uploading the files
  backlinkUrl: Option[String] = None, // backlink url
  callbackUrl: String, // url where to post uploaded files
  continueAfterYesAnswerUrl: Option[String] =
    None, // optional url to continue after user selects YES answer in the form
  continueWhenFullUrl: Option[String] = None, // optional url to continue after all possible files has been uploaded
  continueWhenEmptyUrl: Option[String] = None, // optional url to continue after none file uploaded
  sendoffUrl: Option[String] =
    None, // optional url where to send off the user when the session has been cleared by the host service
  minimumNumberOfFiles: Int = defaultMinimumNumberOfFiles,
  maximumNumberOfFiles: Int = defaultMaximumNumberOfFiles,
  initialNumberOfEmptyRows: Int = defaultInitialNumberOfEmptyRows,
  maximumFileSizeBytes: Long = defaultMaximumFileSizeBytes,
  allowedContentTypes: String = defaultAllowedContentTypes,
  allowedFileExtensions: Option[String] = None,
  prePopulateYesOrNoForm: Option[Boolean] = None, // prepopulates yes form value when true and no form value when false
  cargo: Option[JsValue] = None, // opaque data carried through, from and to the host service,
  newFileDescription: Option[String] = None, // description of the new file added,
  features: Features = Features(), // upload feature switches
  content: CustomizedServiceContent = CustomizedServiceContent() // page content customizations
) {

  final def getContinueWhenFullUrl: String    = continueWhenFullUrl.getOrElse(continueUrl)
  final def getContinueWhenEmptyUrl: String   = continueWhenEmptyUrl.getOrElse(continueUrl)
  final def getFilePickerAcceptFilter: String = allowedContentTypes + allowedFileExtensions.map("," + _).getOrElse("")

  final def isValid: Boolean =
    isValidCallbackUrl(callbackUrl) &&
      isValidFrontendUrl(continueUrl) &&
      backlinkUrl.forall(isValidFrontendUrl) &&
      continueWhenFullUrl.forall(isValidFrontendUrl) &&
      continueWhenEmptyUrl.forall(isValidFrontendUrl) &&
      continueAfterYesAnswerUrl.forall(isValidFrontendUrl) &&
      allowedContentTypes.nonEmpty &&
      minimumNumberOfFiles >= 0 &&
      maximumNumberOfFiles >= 1 &&
      maximumNumberOfFiles >= minimumNumberOfFiles &&
      maximumFileSizeBytes > 0
}

object FileUploadSessionConfig {

  final val defaultMinimumNumberOfFiles: Int     = 1
  final val defaultMaximumNumberOfFiles: Int     = 10
  final val defaultInitialNumberOfEmptyRows: Int = 3
  final val defaultMaximumFileSizeBytes          = 10 * 1024 * 1024
  final val defaultAllowedContentTypes           = "image/jpeg,image/png,application/pdf,text/plain"

  implicit val format: Format[FileUploadSessionConfig] =
    Format(
      ((JsPath \ "nonce").read[Nonce]
        and (JsPath \ "continueUrl").read[String]
        and (JsPath \ "backlinkUrl").readNullable[String]
        and (JsPath \ "callbackUrl").read[String]
        and (JsPath \ "continueAfterYesAnswerUrl").readNullable[String]
        and (JsPath \ "continueWhenFullUrl").readNullable[String]
        and (JsPath \ "continueWhenEmptyUrl").readNullable[String]
        and (JsPath \ "sendoffUrl").readNullable[String]
        and (JsPath \ "minimumNumberOfFiles").readWithDefault[Int](defaultMinimumNumberOfFiles)
        and (JsPath \ "maximumNumberOfFiles").readWithDefault[Int](defaultMaximumNumberOfFiles)
        and (JsPath \ "initialNumberOfEmptyRows").readWithDefault[Int](defaultInitialNumberOfEmptyRows)
        and (JsPath \ "maximumFileSizeBytes").readWithDefault[Long](defaultMaximumFileSizeBytes)
        and (JsPath \ "allowedContentTypes").readWithDefault[String](defaultAllowedContentTypes)
        and (JsPath \ "allowedFileExtensions").readNullable[String]
        and (JsPath \ "prePopulateYesOrNoForm").readNullable[Boolean]
        and (JsPath \ "cargo").readNullable[JsValue]
        and (JsPath \ "newFileDescription").readNullable[String]
        and (JsPath \ "features").readWithDefault[Features](Features())
        and (JsPath \ "content")
          .readWithDefault[CustomizedServiceContent](CustomizedServiceContent()))(FileUploadSessionConfig.apply _),
      ((JsPath \ "nonce").write[Nonce]
        and (JsPath \ "continueUrl").write[String]
        and (JsPath \ "backlinkUrl").writeNullable[String]
        and (JsPath \ "callbackUrl").write[String]
        and (JsPath \ "continueAfterYesAnswerUrl").writeNullable[String]
        and (JsPath \ "continueWhenFullUrl").writeNullable[String]
        and (JsPath \ "continueWhenEmptyUrl").writeNullable[String]
        and (JsPath \ "sendoffUrl").writeNullable[String]
        and (JsPath \ "minimumNumberOfFiles").write[Int]
        and (JsPath \ "maximumNumberOfFiles").write[Int]
        and (JsPath \ "initialNumberOfEmptyRows").write[Int]
        and (JsPath \ "maximumFileSizeBytes").write[Long]
        and (JsPath \ "allowedContentTypes").write[String]
        and (JsPath \ "allowedFileExtensions").writeNullable[String]
        and (JsPath \ "prePopulateYesOrNoForm").writeNullable[Boolean]
        and (JsPath \ "cargo").writeNullable[JsValue]
        and (JsPath \ "newFileDescription").writeNullable[String]
        and (JsPath \ "features").write[Features]
        and (JsPath \ "content")
          .write[CustomizedServiceContent])(unlift((o: FileUploadSessionConfig) => Some(Tuple.fromProductTyped(o))))
    )

}
