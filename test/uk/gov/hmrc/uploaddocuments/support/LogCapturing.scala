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

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{Level, Logger => LogbackLogger}
import ch.qos.logback.core.read.ListAppender
import org.scalatest.Assertion
import play.api.LoggerLike

import scala.jdk.CollectionConverters.ListHasAsScala

trait LogCapturing { _: UnitSpec =>

  def withCaptureOfLoggingFrom(logger: LogbackLogger)(body: (=> List[ILoggingEvent]) => Unit): Unit = {
    val appender = new ListAppender[ILoggingEvent]()
    appender.setContext(logger.getLoggerContext)
    appender.start()
    logger.addAppender(appender)
    logger.setLevel(Level.ALL)
    logger.setAdditive(true)
    body(appender.list.asScala.toList)
  }

  def withCaptureOfLoggingFrom(logger: LoggerLike)(body: (=> List[ILoggingEvent]) => Unit): Unit =
    withCaptureOfLoggingFrom(logger.logger.asInstanceOf[LogbackLogger])(body)

  def logExists(msg: String)(logs: List[ILoggingEvent]): Assertion =
    assert(
      logs.exists(_.getMessage.contains(msg)),
      s"The log msg '$msg' did not appear within the captured log messages."
    )

  def logExists(msg: String, nTimes: Int = 1)(logs: List[ILoggingEvent]): Assertion = {
    val numberOfLogs = logs.collect { case log if log.getMessage.contains(msg) => log }.size
    assert(numberOfLogs == nTimes, s"$msg was not $nTimes times within logs. Actually occurred $numberOfLogs times")
  }
}
