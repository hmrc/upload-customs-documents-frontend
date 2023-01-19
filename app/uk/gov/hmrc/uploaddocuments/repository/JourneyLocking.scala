/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.uploaddocuments.repository

import akka.actor.Scheduler
import uk.gov.hmrc.mongo.lock.{LockRepository, LockService}
import uk.gov.hmrc.uploaddocuments.models.JourneyId
import uk.gov.hmrc.uploaddocuments.services.ScheduleAfter
import uk.gov.hmrc.uploaddocuments.utils.LoggerUtil
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

import scala.concurrent.{ExecutionContext, Future}

trait JourneyLocking extends LoggerUtil {

  val lockRepositoryProvider: LockRepository
  val appConfig: AppConfig

  def lockKeeper(implicit journeyId: JourneyId) = LockService(
    lockRepository = lockRepositoryProvider,
    lockId = journeyId.value,
    ttl = appConfig.lockTimeout
  )

  def takeLock[T](timeOutResult: => Future[T])(f: => Future[T])
                (implicit ec: ExecutionContext, scheduler: Scheduler, journeyId: JourneyId): Future[T] = {

    val timeOut = System.nanoTime() + appConfig.lockTimeout.toNanos
    val checkInterval = appConfig.lockReleaseCheckInterval

    def tryLock(): Future[T] = {

      lockKeeper.withLock(f).flatMap {
        case Some(result) =>
          Future.successful(result)
        case None =>
          if(System.nanoTime() > timeOut) {
            Logger.info(s"[tryLock] for journeyId: '$journeyId' was not released - could not process")
            timeOutResult
          } else {
            Logger.warn(s"[tryLock] for journeyId: '$journeyId' was locked. Waiting for ${checkInterval.toMillis} millis before trying again.")
            ScheduleAfter(checkInterval.toMillis) {
              tryLock()
            }
          }
      }.recover {
        case e: Exception =>
          Logger.error(s"[tryLock] for journeyId: '$journeyId' failed with exception ${e.getMessage}")
          throw e
      }
    }

    tryLock()
  }

}