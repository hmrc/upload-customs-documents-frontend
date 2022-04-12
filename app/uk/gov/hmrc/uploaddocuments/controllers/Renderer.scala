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

import play.api.mvc.Results._
import play.api.mvc._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.uploaddocuments.journeys.State

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

/** Component responsible for translating given session state into the action result. */
@Singleton
class Renderer @Inject() (router: Router) {

  final def acknowledgeFileUploadRedirect = resultOf { case state =>
    (state match {
      case _: State.UploadMultipleFiles        => Created
      case _: State.Summary                    => Created
      case _: State.WaitingForFileVerification => Accepted
      case _                                   => NoContent
    }).withHeaders(HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
  }

  def backlink(breadcrumbs: List[State]): Call =
    breadcrumbs.headOption
      .map(router.routeTo)
      .getOrElse(router.routeTo(State.Uninitialized))

  def resultOf(f: PartialFunction[State, Result]): ((State, List[State])) => Result =
    (stateAndBreadcrumbs: (State, List[State])) =>
      f.applyOrElse(stateAndBreadcrumbs._1, (_: State) => play.api.mvc.Results.NotImplemented)

  def asyncResultOf(f: PartialFunction[State, Future[Result]]): State => Future[Result] = f(_)

}
