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

package uk.gov.hmrc.uploaddocuments.support

import org.scalacheck.Gen
import uk.gov.hmrc.uploaddocuments.models._

import java.time.ZonedDateTime

object Generators {

  final implicit class OptionExt[A](val value: Option[A]) extends AnyVal {
    def existsIn(set: Set[A]): Boolean =
      value.exists(set.contains)
  }

  final val upperCaseChar = Gen.oneOf("ABCDEFGHIJKLMNOPRSUWXYZ".toIndexedSeq)

  final def nonEmptyString(maxSize: Int, noOfSpaces: Int = 0): Gen[String] =
    Gen
      .listOfN(
        noOfSpaces + 1,
        Gen
          .nonEmptyContainerOf[Array, Char](Gen.alphaNumChar)
          .map(_.take(maxSize / (noOfSpaces + 1)))
          .map(String.valueOf)
      )
      .map(_.mkString(" "))

  final val booleanGen                           = Gen.frequency((20, Gen.const(false)), (80, Gen.const(true)))
  final val noneGen                              = Gen.const(None)
  final def some[A](gen: Gen[A]): Gen[Option[A]] = gen.map(Some.apply)

  final def conditional[A](b: Option[Any], gen: Gen[A]): Gen[Option[A]] =
    if (b.isDefined) Gen.option(gen) else noneGen

  final def follows[A](b: Option[Any], gen: Gen[A]): Gen[Option[A]] =
    if (b.isDefined) some(gen) else noneGen

  final def conditional[A](b: Boolean, gen: Gen[A]): Gen[Option[A]] =
    if (b) Gen.option(gen) else noneGen

  final val uploadedFileGen = for {
    upscanReference <- Gen.uuid.map(_.toString())
    downloadUrl     <- Gen.const("https://foo.bar/123")
    uploadTimestamp <- Gen.const(ZonedDateTime.now)
    checksum        <- nonEmptyString(64)
    fileName        <- nonEmptyString(20)
    fileMimeType    <- Gen.oneOf("text/plain", "text/jpeg", "application/pdf", "")
    fileSize        <- Gen.chooseNum(1, 6 * 1024 * 1024)
  } yield UploadedFile(
    upscanReference,
    downloadUrl,
    uploadTimestamp,
    checksum,
    fileName,
    fileMimeType,
    fileSize
  )

}
