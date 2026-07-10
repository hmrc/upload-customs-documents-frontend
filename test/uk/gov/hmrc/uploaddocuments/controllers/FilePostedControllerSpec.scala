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

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.{Configuration, Environment}
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.uploaddocuments.models.{FileUploads, JourneyId}
import uk.gov.hmrc.uploaddocuments.services.FileUploadService
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.*

class FilePostedControllerSpec extends AnyWordSpec with Matchers {

  given sys: ActorSystem  = ActorSystem("FilePostedControllerSpec")
  given mat: Materializer = Materializer(sys)

  val testAppConfig: AppConfig = new AppConfig {
    override val baseInternalCallbackUrl: String                         = ""
    override val baseExternalCallbackUrl: String                         = ""
    override val authBaseUrl: String                                     = "http://auth-unused"
    override val upscanInitiateBaseUrl: String                           = ""
    override val mongoSessionExpiration: Duration                        = 1.hour
    override val govukStartUrl: String                                   = "http://gov.uk/start"
    override val contactHost: String                                     = ""
    override val contactFormServiceIdentifier: String                    = ""
    override val signOutUrl: String                                      = ""
    override val timeout: Int                                            = 10
    override val countdown: Int                                          = 2
    override val fileUploadResultPushRetryIntervals: Seq[FiniteDuration] = Seq.empty
    override val upscanInitiateRetryIntervals: Seq[FiniteDuration]       = Seq.empty
    override val upscanInitialWaitTime: Duration                         = 2.seconds
    override val upscanWaitInterval: Duration                            = 500.milliseconds
    override def lockReleaseCheckInterval: Duration                      = 500.milliseconds
    override def lockTimeout: Duration                                   = 2.seconds
  }

  // BaseControllerComponents with no-op auth connector: whenAuthenticated is overridden below
  // so authConnector.authorise is never invoked in these tests.
  val components: BaseControllerComponents = new BaseControllerComponents(
    appConfig = testAppConfig,
    authConnector = null,
    environment = Environment.simple(),
    configuration = Configuration.empty,
    messagesControllerComponents = stubMessagesControllerComponents()
  )

  /** A subclass of FilePostedController that bypasses the real auth check. whenAuthenticated is not final, so we
    * override it to call the body directly. whenInSession is final but resolves from the HeaderCarrier sessionId
    * (provided via the FakeRequest session below).
    */
  def makeController(svc: FileUploadService): FilePostedController =
    new FilePostedController(components, svc) {
      override protected def whenAuthenticated[A](body: => Future[Result])(using
        request: Request[A],
        hc: HeaderCarrier,
        ec: scala.concurrent.ExecutionContext
      ): Future[Result] = body
    }

  /** FakeRequest carrying a session ID so that whenInSession resolves to a JourneyId. */
  def fakeGet(uri: String): FakeRequest[play.api.mvc.AnyContentAsEmpty.type] =
    FakeRequest("GET", uri).withSession(SessionKeys.sessionId -> "test-session-id-12345")

  "FilePostedController.markFileUploadAsPosted" should {

    "redirect to /choose-files when a valid key query param is provided" in {
      val svc = new FileUploadService(null, null, null, null, null) {
        override def markFileAsPosted(key: String)(using journeyId: JourneyId): Future[Option[FileUploads]] =
          Future.successful(Some(FileUploads()))
      }
      val controller = makeController(svc)
      val result     = call(controller.markFileUploadAsPosted, fakeGet("/file-posted?key=test-file-key"))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.ChooseMultipleFilesController.showChooseMultipleFiles.url)
    }

    "return BadRequest when no key query parameter is provided" in {
      val svc = new FileUploadService(null, null, null, null, null) {
        override def markFileAsPosted(key: String)(using journeyId: JourneyId): Future[Option[FileUploads]] =
          Future.successful(None)
      }
      val controller = makeController(svc)
      val result     = call(controller.markFileUploadAsPosted, fakeGet("/file-posted"))
      status(result) shouldBe BAD_REQUEST
    }
  }
}
