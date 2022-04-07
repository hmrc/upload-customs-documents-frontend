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

package uk.gov.hmrc.uploaddocuments.services

import akka.actor.{ActorSystem, Scheduler}
import akka.pattern.FutureTimeoutSupport
import com.typesafe.config.Config
import play.api.libs.json.Format
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.uploaddocuments.journeys.StateFormats
import uk.gov.hmrc.uploaddocuments.repository.CacheRepository
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

import uk.gov.hmrc.uploaddocuments.journeys.Transition
import uk.gov.hmrc.uploaddocuments.journeys.State
import uk.gov.hmrc.uploaddocuments.journeys.IsTransient

trait SessionStateService {

  final val journeyKey = "FileUploadJourney"
  final val stateFormats: Format[State] = StateFormats.formats
  final val default: State = State.Uninitialized

  def currentSessionState(implicit rc: HeaderCarrier, ec: ExecutionContext): Future[Option[(State, List[State])]]

  /** Applies transition to the current state and returns new state or error */
  def updateSessionState(
    transition: Transition[State]
  )(implicit rc: HeaderCarrier, ec: ExecutionContext): Future[(State, List[State])]

  def cleanBreadcrumbs(implicit rc: HeaderCarrier, ec: ExecutionContext): Future[List[State]]

  /** Return the current state if matches expected type or apply the transition. */
  final def getCurrentOrUpdateSessionState[S <: State: ClassTag](
    transition: Transition[State]
  )(implicit rc: HeaderCarrier, ec: ExecutionContext): Future[(State, List[State])] =
    currentSessionState.flatMap {
      case Some(sb @ (state, breadcrumbs)) if is[S](state) => Future.successful(sb)
      case _                                               => updateSessionState(transition)
    }

  /** Wait for state until timeout. */
  final def waitForSessionState[S <: State: ClassTag](intervalInMiliseconds: Long, timeoutNanoTime: Long)(
    ifTimeout: => Future[(State, List[State])]
  )(implicit rc: HeaderCarrier, scheduler: Scheduler, ec: ExecutionContext): Future[(State, List[State])] =
    currentSessionState.flatMap {
      case Some(sb @ (state: State, _)) if is[S](state) =>
        Future.successful(sb)
      case _ =>
        if (System.nanoTime() > timeoutNanoTime) {
          ifTimeout
        } else
          ScheduleAfter(intervalInMiliseconds) {
            waitForSessionState[S](intervalInMiliseconds * 2, timeoutNanoTime)(ifTimeout)
          }
    }

  private def is[S <: State: ClassTag](state: State): Boolean =
    implicitly[ClassTag[S]].runtimeClass.isAssignableFrom(state.getClass)

  final def updateBreadcrumbs(
    newState: State,
    currentState: State,
    currentBreadcrumbs: List[State]
  ): List[State] =
    if (currentState.isInstanceOf[IsTransient])
      currentBreadcrumbs
    else if (newState.getClass == currentState.getClass)
      currentBreadcrumbs
    else if (currentBreadcrumbs.nonEmpty && currentBreadcrumbs.head.getClass() == newState.getClass())
      currentBreadcrumbs.tail
    else currentState :: currentBreadcrumbs.take(3)

}

@Singleton
case class MongoDBCachedSessionStateService @Inject() (
  cacheRepository: CacheRepository,
  config: Config,
  appConfig: AppConfig,
  actorSystem: ActorSystem
) extends EncryptedSessionCache[State, HeaderCarrier] with SessionStateService {

  final val baseKeyProvider: KeyProvider = KeyProvider(config)

  override final val keyProviderFromContext: HeaderCarrier => KeyProvider =
    hc => KeyProvider(baseKeyProvider, None)

  final def getJourneyId(hc: HeaderCarrier): Option[String] =
    hc.extraHeaders.find(_._1 == journeyKey).map(_._2)

  override val trace: Boolean = appConfig.trace

}

object ScheduleAfter extends FutureTimeoutSupport {

  /** Delay execution of the future by given miliseconds */
  def apply[T](
    delayInMiliseconds: Long
  )(body: => Future[T])(implicit scheduler: Scheduler, ec: ExecutionContext): Future[T] =
    after(duration = FiniteDuration(delayInMiliseconds, "ms"), using = scheduler)(body)
}
