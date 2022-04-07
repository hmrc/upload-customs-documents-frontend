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

  "authorisedWithEnrolment" should {

    "authorize when enrolment granted" in {
      givenAuthorisedForEnrolment(Enrolment("serviceName", "serviceKey", "serviceIdentifierFoo"), required = true)
      val result = TestController.testAuthorizedWithEnrolment("serviceName", "serviceKey")
      status(result) shouldBe 200
      bodyOf(result) should be("12345-credId,serviceIdentifierFoo")
    }

    "redirect to government gateway login when insufficient enrollments" in {
      givenRequestIsNotAuthorised("InsufficientEnrolments")
      val result = TestController.testAuthorizedWithEnrolment("serviceName", "serviceKey")
      status(result) shouldBe 303
      redirectLocation(result).get should include(
        "/bas-gateway/sign-in?continue_url=%2F&origin=upload-customs-documents-frontend"
      )
    }

    "redirect to government gateway login when authorization fails" in {
      givenRequestIsNotAuthorised("IncorrectCredentialStrength")
      val result = TestController.testAuthorizedWithEnrolment("serviceName", "serviceKey")
      status(result) shouldBe 303
      redirectLocation(result).get should include(
        "/bas-gateway/sign-in?continue_url=%2F&origin=upload-customs-documents-frontend"
      )
    }
  }

  "authorisedWithoutEnrolment" should {

    "authorize when insufficient enrollments" in {
      givenAuthorisedWithoutEnrolments
      val result = TestController.testAuhorizedWithoutEnrolment
      status(result) shouldBe 200
      bodyOf(result) should be("12345-credId,none")
    }

    "authorize stride users" in {
      givenAuthorisedAsStrideUser
      val result = TestController.testAuhorizedWithoutEnrolment
      status(result) shouldBe 200
      bodyOf(result) should be("12345-credId,none")
    }

    "redirect to government gateway login when authorization fails" in {
      givenRequestIsNotAuthorised("IncorrectCredentialStrength")
      val result = TestController.testAuhorizedWithoutEnrolment
      status(result) shouldBe 303
      redirectLocation(result).get should include(
        "/bas-gateway/sign-in?continue_url=%2F&origin=upload-customs-documents-frontend"
      )
    }
  }

  "authorisedWithoutEnrolmentReturningForbidden" should {

    "authorize when insufficient enrollments" in {
      givenAuthorisedWithoutEnrolments
      val result = TestController.testAuhorizedWithoutEnrolmentReturningForbidden
      status(result) shouldBe 200
      bodyOf(result) should be("12345-credId,none")
    }

    "authorize stride users" in {
      givenAuthorisedAsStrideUser
      val result = TestController.testAuhorizedWithoutEnrolmentReturningForbidden
      status(result) shouldBe 200
      bodyOf(result) should be("12345-credId,none")
    }

    "redirect to government gateway login when authorization fails" in {
      givenRequestIsNotAuthorised("IncorrectCredentialStrength")
      val result = TestController.testAuhorizedWithoutEnrolmentReturningForbidden
      status(result) shouldBe 403
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

    def testAuthorizedWithEnrolment[A](serviceName: String, identifierKey: String): Result =
      await(super.authorisedWithEnrolment(serviceName, identifierKey) { case (uid, res) =>
        Future.successful(Ok(uid.getOrElse("none") + "," + res.getOrElse("none")))
      })

    def testAuhorizedWithoutEnrolment[A]: Result =
      await(super.authorisedWithoutEnrolment { case (uid, res) =>
        Future.successful(Ok(uid.getOrElse("none") + "," + res.getOrElse("none")))
      })

    def testAuhorizedWithoutEnrolmentReturningForbidden[A]: Result =
      await(super.authorisedWithoutEnrolmentReturningForbidden { case (uid, res) =>
        Future.successful(Ok(uid.getOrElse("none") + "," + res.getOrElse("none")))
      })
  }

}
