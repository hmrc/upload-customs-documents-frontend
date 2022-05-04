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

package uk.gov.hmrc.uploaddocuments.utils

import org.slf4j.{Logger, LoggerFactory}
import play.api.{LoggerLike, MarkerContext}

trait LoggerUtil {

  final lazy val className = this.getClass.getSimpleName.stripSuffix("$")

  final val logger: LoggerLike = new LoggerLike {
    override val logger: Logger = LoggerFactory.getLogger(s"uk.gov.hmrc.uploaddocuments.$className")
  }

  private final def prefixLog(msg: String): String =
    s"[$className]" + (if (msg.startsWith("[")) msg else " " + msg)

  object Logger {

    def debug(message: => String)(implicit mc: MarkerContext): Unit = logger.debug(prefixLog(message))
    def debug(message: => String, error: => Throwable)(implicit mc: MarkerContext): Unit = logger.debug(prefixLog(message), error)

    def info(message: => String)(implicit mc: MarkerContext): Unit = logger.info(prefixLog(message))
    def info(message: => String, error: => Throwable)(implicit mc: MarkerContext): Unit = logger.info(prefixLog(message), error)

    def warn(message: => String)(implicit mc: MarkerContext): Unit = logger.warn(prefixLog(message))
    def warn(message: => String, error: => Throwable)(implicit mc: MarkerContext): Unit = logger.warn(prefixLog(message), error)

    def error(message: => String)(implicit mc: MarkerContext): Unit = logger.error(prefixLog(message))
    def error(message: => String, error: => Throwable)(implicit mc: MarkerContext): Unit = logger.error(prefixLog(message), error)
  }
}
