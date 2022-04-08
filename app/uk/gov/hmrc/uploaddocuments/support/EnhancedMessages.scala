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

import play.api.i18n.Messages
import play.api.i18n.Lang
import play.i18n
import java.text.MessageFormat

class EnhancedMessages(inherited: Messages, custom: Map[String, String]) extends Messages {

  override def lang: Lang = inherited.lang

  override def apply(key: String, args: Any*): String =
    translate(key, args).getOrElse(key)

  override def apply(keys: Seq[String], args: Any*): String =
    keys
      .foldLeft(Option.empty[String])((acc, key) => acc.orElse(translate(key, args)))
      .getOrElse(keys.last)

  override def translate(key: String, args: Seq[Any]): Option[String] =
    custom
      .get(key)
      .map { p =>
        new MessageFormat(p, lang.toLocale)
          .format(args.map(_.asInstanceOf[Object]).toArray)
      }
      .orElse(inherited.translate(key, args))

  override def isDefinedAt(key: String): Boolean =
    custom.contains(key) || inherited.isDefinedAt(key)

  override def asJava: i18n.Messages =
    inherited.asJava

}
