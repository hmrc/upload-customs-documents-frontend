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

import play.api.mvc.PathBindable
import uk.gov.hmrc.uploaddocuments.models

case class JourneyId(value: String) extends AnyVal {
  override def toString: String = value
}

object JourneyId {
  implicit lazy val pathBindable: PathBindable[JourneyId] =
    new PathBindable[JourneyId] {
      override def bind(key: String, value: String): Either[String, models.JourneyId] =
        if (value.nonEmpty) Right(JourneyId(value)) else Left("journeyId is empty")

      override def unbind(key: String, journeyId: JourneyId): String = journeyId.value
    }
}
