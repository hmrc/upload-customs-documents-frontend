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

package uk.gov.hmrc.uploaddocuments.connectors

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.after
import play.api.Configuration
import uk.gov.hmrc.mdc.Mdc
import uk.gov.hmrc.uploaddocuments.utils.LoggerUtil

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait Retries extends LoggerUtil {

  protected def actorSystem: ActorSystem

  def retry[A](intervals: FiniteDuration*)(shouldRetry: Try[A] => Boolean, retryReason: A => String)(
    block: => Future[A]
  )(implicit ec: ExecutionContext): Future[A] = {
    def loop(remainingIntervals: Seq[FiniteDuration])(mdcData: Map[String, String])(block: => Future[A]): Future[A] =
      // scheduling will loose MDC data. Here we explicitly ensure it is available on block.
      Mdc
        .withMdc(block, mdcData)
        .flatMap(result =>
          if (remainingIntervals.nonEmpty && shouldRetry(Success(result))) {
            val delay = remainingIntervals.head
            Logger.warn(
              s"Will retry [${intervals.size - remainingIntervals.size + 1}] in $delay due to ${retryReason(result)}"
            )
            after(delay, actorSystem.scheduler)(loop(remainingIntervals.tail)(mdcData)(block))
          } else {
            Future.successful(result)
          }
        )
        .recoverWith { case e: Throwable =>
          if (remainingIntervals.nonEmpty && shouldRetry(Failure(e))) {
            val delay = remainingIntervals.head
            Logger.warn(
              s"Will retry [${intervals.size - remainingIntervals.size + 1}] in $delay due to ${e.getClass.getName}: ${e.getMessage}"
            )
            after(delay, actorSystem.scheduler)(loop(remainingIntervals.tail)(mdcData)(block))
          } else {
            Logger.error(s"After [${intervals.size + 1}] retries failing with ${e.getClass.getName}: ${e.getMessage}")
            Future.failed(e)
          }
        }
    loop(intervals)(Mdc.mdcData)(block)
  }

}

object Retries {

  def getConfIntervals(key: String, configuration: Configuration): Seq[FiniteDuration] =
    configuration
      .getOptional[Seq[String]](s"$key.retryIntervals")
      .map(_.map(Duration.create).map(d => FiniteDuration(d.length, d.unit)))
      .getOrElse(Seq.empty)

}
