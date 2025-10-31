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

import play.api.libs.json.Format

import java.time.*
import java.time.format.DateTimeFormatter

/** Testing and serialization friendly timestamp wrapper. */
sealed trait Timestamp {

  val value: Long

  def isAfter(other: Timestamp, minGapMillis: Long): Boolean

  def duration: Long = System.currentTimeMillis() - value

  final override def hashCode(): Int = value.toInt

  final override def toString: String =
    DateTimeFormatter.ISO_LOCAL_TIME
      .format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneId.of("UTC")))
}

object Timestamp {

  final def now: Timestamp = Strict(System.currentTimeMillis())

  final def apply(value: Long): Timestamp = Strict(value)

  object Any extends Timestamp {
    final val value: Long = 0

    final override def equals(obj: scala.Any): Boolean =
      obj.isInstanceOf[Timestamp]

    final def isAfter(other: Timestamp, minGapMillis: Long): Boolean =
      true
  }

  final case class Strict(value: Long) extends Timestamp {
    override def equals(obj: scala.Any): Boolean =
      obj match {
        case strict: Strict => strict.value == value
        case Any            => true
        case _              => false
      }

    def isAfter(other: Timestamp, minGapMillis: Long): Boolean =
      other match {
        case _: Strict => (other.value + minGapMillis) < value
        case Any       => true
      }
  }

  implicit final val formats: Format[Timestamp] =
    SimpleDecimalFormat[Timestamp](s => Timestamp(s.toLongExact), n => BigDecimal(n.value))
}
