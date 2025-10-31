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

package uk.gov.hmrc.uploaddocuments.support

class UploadLogFormatSpec extends UnitSpec {

  " UploadLog formats" should {

    "serialize and deserialize Success" in new JsonFormatTest[Success](info) {
      validateJsonFormat(
        """{"service":"a","fileMimeType":"b","fileSize":12345,"success":true,"duration":123}""".stripMargin,
        Success(service = "a", fileMimeType = "b", fileSize = 12345, success = true, duration = Some(123))
      )
    }

    "serialize and deserialize Failure" in new JsonFormatTest[Failure](info) {
      validateJsonFormat(
        """{"service":"a","error":"b","description":"c","success":false,"duration":123}""".stripMargin,
        Failure(service = "a", error = "b", description = "c", success = false, duration = Some(123))
      )
    }

  }
}
