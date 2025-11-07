/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.http.HeaderNames
import play.api.mvc.Result
import play.api.mvc.Results.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.{Application, Configuration, Environment}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.uploaddocuments.support.AppISpec

import scala.concurrent.Future
import play.api.mvc.AnyContentAsEmpty

class AuthActionsISpec extends AuthActionISpecSetup {

  "whenAuthenticated" should {

    "authorize when logged in session exists" in {
      givenAuthorisedWithoutEnrolments
      val result = TestController.testWhenAuthenticated
      status(result) shouldBe OK
      bodyOf(result) should be("authenticated")
    }

    "authorize stride users" in {
      givenAuthorisedAsStrideUser
      val result = TestController.testWhenAuthenticated
      status(result) shouldBe OK
      bodyOf(result) should be("authenticated")
    }

    "redirect to government gateway login when authorization fails" in {
      givenRequestIsNotAuthorised("IncorrectCredentialStrength")
      val result = TestController.testWhenAuthenticated
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should include(
        "/bas-gateway/sign-in?continue_url=%2F&origin=upload-customs-documents-frontend"
      )
    }
  }

  "whenAuthenticatedInBackchannel" should {

    "authorize when logged in session exists" in {
      givenAuthorisedWithoutEnrolments
      val result = TestController.testWhenAuthenticatedInBackchannel
      status(result) shouldBe OK
      bodyOf(result) should be("authenticatedBackchannel")
    }

    "authorize stride users" in {
      givenAuthorisedAsStrideUser
      val result = TestController.testWhenAuthenticatedInBackchannel
      status(result) shouldBe OK
      bodyOf(result) should be("authenticatedBackchannel")
    }

    "return forbidden when not authorised" in {
      givenRequestIsNotAuthorised("IncorrectCredentialStrength")
      val result = TestController.testWhenAuthenticatedInBackchannel
      status(result) shouldBe FORBIDDEN
    }
  }
}

trait AuthActionISpecSetup extends AppISpec {

  override def fakeApplication(): Application = appBuilder.build()

  object TestController extends AuthActions {

    override def authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]

    override def config: Configuration = app.injector.instanceOf[Configuration]

    override def env: Environment = app.injector.instanceOf[Environment]

    import scala.concurrent.ExecutionContext.Implicits.global

    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      .withSession(SessionKeys.authToken -> "Bearer XYZ")
      .withHeaders(HeaderNames.AUTHORIZATION -> "Bearer XYZ")

    def testWhenAuthenticated: Result =
      await(super.whenAuthenticated(Future.successful(Ok("authenticated"))))

    def testWhenAuthenticatedInBackchannel: Result =
      await(super.whenAuthenticatedInBackchannel(Future.successful(Ok("authenticatedBackchannel"))))
  }

}
