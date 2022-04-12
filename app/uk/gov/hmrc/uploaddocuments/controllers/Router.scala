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

import play.api.data.Form
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.uploaddocuments.journeys.State
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

import javax.inject.{Inject, Singleton}

@Singleton
class Router @Inject() (appConfig: AppConfig) {

  /** This cookie is set by the script on each request coming from one of our own pages open in the browser.
    */
  final val COOKIE_JSENABLED = "jsenabled"

  final val baseInternalCallbackUrl: String = appConfig.baseInternalCallbackUrl

  final def preferUploadMultipleFiles(implicit rh: RequestHeader): Boolean =
    rh.cookies.get(COOKIE_JSENABLED).isDefined

  final def redirectTo(stateAndBreadcrumbs: (State, List[State])): Result =
    Redirect(routeTo(stateAndBreadcrumbs._1))

  final def redirectWithForm[A](formWithErrors: Form[A])(stateAndBreadcrumbsOpt: Option[(State, List[State])]): Result =
    Redirect(
      stateAndBreadcrumbsOpt
        .map { case (s, _) => routeTo(s) }
        .getOrElse(routes.StartController.start)
    )
      .flashing(Flash {
        val data = formWithErrors.data
        // dummy parameter required if empty data
        if (data.isEmpty) Map("dummy" -> "") else data
      })

  final def routeTo(state: State): Call =
    state match {
      case State.Initialized(context, fileUploads) =>
        Call(
          "GET",
          context.config.continueAfterYesAnswerUrl
            .getOrElse(context.config.backlinkUrl)
        )

      case State.ContinueToHost(context, fileUploads) => routes.ContinueToHostController.continueToHost
      case _: State.UploadMultipleFiles               => routes.ChooseMultipleFilesController.showChooseMultipleFiles
      case _: State.UploadSingleFile                  => routes.ChooseSingleFileController.showChooseFile
      case _: State.WaitingForFileVerification => routes.FileVerificationController.showWaitingForFileVerification
      case _: State.Summary                    => routes.SummaryController.showSummary
      case _: State.SwitchToUploadSingleFile   => routes.ChooseSingleFileController.showChooseFile
      case _                                   => Call("GET", appConfig.govukStartUrl)
    }

  final def callbackFromUpscan(journeyId: String, nonce: String) =
    appConfig.baseInternalCallbackUrl +
      internal.routes.CallbackFromUpscanController.callbackFromUpscan(journeyId, nonce).url

  final def successRedirect(journeyId: String)(implicit rh: RequestHeader): String =
    appConfig.baseExternalCallbackUrl + (rh.cookies.get(COOKIE_JSENABLED) match {
      case Some(_) => routes.FileVerificationController.asyncWaitingForFileVerification(journeyId)
      case None    => routes.FileVerificationController.showWaitingForFileVerification
    })

  final def successRedirectWhenUploadingMultipleFiles(journeyId: String): String =
    appConfig.baseExternalCallbackUrl + routes.FilePostedController.asyncMarkFileUploadAsPosted(journeyId)

  final def errorRedirect(journeyId: String)(implicit rh: RequestHeader): String =
    appConfig.baseExternalCallbackUrl + (rh.cookies.get(COOKIE_JSENABLED) match {
      case Some(_) => routes.FileRejectedController.asyncMarkFileUploadAsRejected(journeyId)
      case None    => routes.FileRejectedController.markFileUploadAsRejected
    })

}
