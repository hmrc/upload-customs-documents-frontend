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

package uk.gov.hmrc.uploaddocuments.controllers

import play.api.data.Forms.{of, optional, text}
import play.api.data.Mapping
import play.api.data.format.Formats._
import play.api.data.validation._
import uk.gov.hmrc.uploaddocuments.models._

object FormFieldMappings {

  val normalizedText: Mapping[String] = of[String].transform(_.replaceAll("\\s", ""), identity)
  val uppercaseNormalizedText: Mapping[String] = normalizedText.transform(_.toUpperCase, identity)
  val validDomain: String = """.*@([a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]{2,4})+)"""
  val nonAllowedCharTypes =
    List(Character.CONTROL, Character.SURROGATE, Character.FORMAT, Character.PRIVATE_USE)

  def nonEmpty(fieldName: String): Constraint[String] =
    Constraint[String]("constraint.required") { s =>
      Option(s)
        .filter(!_.trim.isEmpty)
        .fold[ValidationResult](Invalid(ValidationError(s"error.$fieldName.required")))(_ => Valid)
    }

  def haveLength(fieldName: String, expectedLength: Int): Constraint[String] =
    Constraint[String]("constraint.length") { s =>
      Option(s)
        .filter(_.length == expectedLength)
        .fold[ValidationResult](Invalid(ValidationError(s"error.$fieldName.invalid-length")))(_ => Valid)
    }

  def constraint[A](fieldName: String, errorType: String, predicate: A => Boolean): Constraint[A] =
    Constraint[A](s"constraint.$fieldName.$errorType") { s =>
      Option(s)
        .filter(predicate)
        .fold[ValidationResult](Invalid(ValidationError(s"error.$fieldName.$errorType")))(_ => Valid)
    }

  def constraintNoErrorType[A](fieldName: String, predicate: A => Boolean): Constraint[A] =
    Constraint[A](s"constraint.$fieldName") { s =>
      Option(s)
        .filter(predicate)
        .fold[ValidationResult](Invalid(ValidationError(s"error.$fieldName")))(_ => Valid)
    }

  def first[A](cs: Constraint[A]*): Constraint[A] =
    Constraint[A](s"constraints.sequence.${cs.map(_.name).mkString(".")}") { s =>
      cs.foldLeft[ValidationResult](Valid) { (r, c) =>
        r match {
          case Valid => c.apply(s)
          case r     => r
        }
      }
    }

  def all[A](cs: Constraint[A]*): Constraint[A] =
    Constraint[A](s"constraints.sequence.${cs.map(_.name).mkString(".")}") { s =>
      cs.foldLeft[ValidationResult](Valid) { (r, c) =>
        r match {
          case Valid => c.apply(s)
          case r @ Invalid(e1) =>
            c.apply(s) match {
              case Valid       => r
              case Invalid(e2) => Invalid(e1 ++ e2)
            }
        }
      }
    }

  def some[A](c: Constraint[A]): Constraint[Option[A]] =
    Constraint[Option[A]](s"${c.name}.optional") {
      case Some(s) => c.apply(s)
      case None    => Valid
    }

  def enumMapping[A: EnumerationFormats](fieldName: String): Mapping[A] =
    optional(text)
      .verifying(constraint[Option[String]](fieldName, "required", _.isDefined))
      .transform[String](_.get, Option.apply)
      .verifying(constraint(fieldName, "invalid-option", implicitly[EnumerationFormats[A]].isValidKey))
      .transform(implicitly[EnumerationFormats[A]].valueOf(_).get, implicitly[EnumerationFormats[A]].keyOf(_).get)

  def booleanMapping(fieldName: String, trueValue: String, falseValue: String): Mapping[Boolean] =
    optional(text)
      .verifying(constraint[Option[String]](fieldName, "required", _.exists(s => s == trueValue || s == falseValue)))
      .transform[Boolean](_.contains(trueValue), b => if (b) Some(trueValue) else Some(falseValue))

  val allowedSpecialNameCharacterSet = Set(' ', '/', '\\', '_', '-', '&', '+', '\'', '.', '’')

  val yesNoMapping: Mapping[Boolean] = booleanMapping("choice", "yes", "no")

  val allowedResponseSpecialCharacters = " ()/+-*=^.;_&#@!?\"'{}[]\\~|%£$€"

  def isAllowedResponseCharacter(c: Char): Boolean =
    Character.isLetterOrDigit(c) || allowedResponseSpecialCharacters.contains(c)

}
