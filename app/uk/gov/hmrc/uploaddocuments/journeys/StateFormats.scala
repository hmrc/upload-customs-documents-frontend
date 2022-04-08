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

package uk.gov.hmrc.uploaddocuments.journeys

import play.api.libs.json._
import uk.gov.hmrc.uploaddocuments.journeys.State._
import uk.gov.hmrc.uploaddocuments.support.IdentityUtils

object StateFormats {

  val initializedFormat = Json.format[Initialized]
  val uploadMultipleFilesFormat = Json.format[UploadMultipleFiles]
  val uploadSingleFileFormat = Json.format[UploadSingleFile]
  val waitingForFileVerificationFormat = Json.format[WaitingForFileVerification]
  val summaryFormat = Json.format[Summary]
  val switchToUploadSingleFileFormat = Json.format[SwitchToUploadSingleFile]
  val continueToHostFormat = Json.format[ContinueToHost]

  private val serializeStateProperties: PartialFunction[State, JsValue] = {
    case s: Initialized                => initializedFormat.writes(s)
    case s: UploadMultipleFiles        => uploadMultipleFilesFormat.writes(s)
    case s: UploadSingleFile           => uploadSingleFileFormat.writes(s)
    case s: WaitingForFileVerification => waitingForFileVerificationFormat.writes(s)
    case s: Summary                    => summaryFormat.writes(s)
    case s: SwitchToUploadSingleFile   => switchToUploadSingleFileFormat.writes(s)
    case s: ContinueToHost             => continueToHostFormat.writes(s)
  }

  private def deserializeState(stateName: String, properties: JsValue): JsResult[State] =
    stateName match {
      case "Uninitialized"              => JsSuccess(Uninitialized)
      case "Initialized"                => initializedFormat.reads(properties)
      case "UploadMultipleFiles"        => uploadMultipleFilesFormat.reads(properties)
      case "UploadSingleFile"           => uploadSingleFileFormat.reads(properties)
      case "WaitingForFileVerification" => waitingForFileVerificationFormat.reads(properties)
      case "Summary"                    => summaryFormat.reads(properties)
      case "SwitchToUploadSingleFile"   => switchToUploadSingleFileFormat.reads(properties)
      case "ContinueToHost"             => continueToHostFormat.reads(properties)
      case _                            => JsError(s"Unknown state name $stateName")
    }

  final val reads: Reads[State] = new Reads[State] {
    override def reads(json: JsValue): JsResult[State] =
      json match {
        case obj: JsObject =>
          (obj \ "state")
            .asOpt[String]
            .map(s => (obj \ "properties").asOpt[JsValue].map(p => (s, p)).getOrElse((s, JsNull))) match {
            case Some((stateName, properties)) => deserializeState(stateName, properties)
            case None                          => JsError("Missing state field")
          }

        case o => JsError(s"Cannot parse State from $o, must be JsObject.")
      }
  }

  final val writes: Writes[State] = new Writes[State] {
    override def writes(state: State): JsValue =
      if (serializeStateProperties.isDefinedAt(state)) serializeStateProperties(state) match {
        case JsNull => Json.obj("state" -> IdentityUtils.identityOf(state))
        case properties =>
          Json.obj("state" -> IdentityUtils.identityOf(state), "properties" -> properties)
      }
      else Json.obj("state" -> IdentityUtils.identityOf(state))
  }

  final def formats: Format[State] = Format(reads, writes)
}
