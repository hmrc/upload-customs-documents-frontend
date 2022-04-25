package uk.gov.hmrc.uploaddocuments.controllers

import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration, Environment}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.uploaddocuments.support.AppISpec

import scala.concurrent.Future

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

  override def fakeApplication: Application = appBuilder.build()

  object TestController extends AuthActions {

    override def authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]

    override def config: Configuration = app.injector.instanceOf[Configuration]

    override def env: Environment = app.injector.instanceOf[Environment]

    import scala.concurrent.ExecutionContext.Implicits.global

    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val request = FakeRequest().withSession(SessionKeys.authToken -> "Bearer XYZ")

    def testWhenAuthenticated: Result =
      await(super.whenAuthenticated { Future.successful(Ok("authenticated")) })

    def testWhenAuthenticatedInBackchannel: Result =
      await(super.whenAuthenticatedInBackchannel { Future.successful(Ok("authenticatedBackchannel")) })
  }

}
