package uk.gov.hmrc.uploaddocuments.controllers

import play.api.libs.ws.{DefaultWSCookie, StandaloneWSRequest}
import play.api.mvc._
import play.api.test.FakeRequest
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.http.{HeaderCarrier, SessionId, SessionKeys}
import uk.gov.hmrc.mongo.cache.CacheItem
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.repository.JourneyCacheRepository
import uk.gov.hmrc.uploaddocuments.repository.JourneyCacheRepository.DataKeys
import uk.gov.hmrc.uploaddocuments.support.{SHA256, ServerISpec, TestData}

trait ControllerISpecBase extends ServerISpec {

  val journeyId = "sadasdjkasdhuqyhwa326176318346674e764764"
  val sessionId = SessionId(journeyId)

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(sessionId))
  def getJourneyId: String       = SHA256.compute(journeyId)

  lazy val newJourneyRepo = app.injector.instanceOf[JourneyCacheRepository]

  import play.api.i18n._
  implicit val messages: Messages = MessagesImpl(Lang("en"), app.injector.instanceOf[MessagesApi])

  final def fakeRequest(cookies: Cookie*)(implicit hc: HeaderCarrier): Request[AnyContent] =
    fakeRequest("GET", "/", cookies: _*)

  final def fakeRequest(method: String, path: String, cookies: Cookie*)(
    implicit hc: HeaderCarrier): Request[AnyContent] =
    FakeRequest(Call(method, path))
      .withCookies(cookies: _*)
      .withSession(SessionKeys.sessionId -> hc.sessionId.map(_.value).getOrElse(""))

  final def request(path: String)(implicit hc: HeaderCarrier): StandaloneWSRequest = {
    val sessionCookie =
      sessionCookieBaker
        .encodeAsCookie(Session(Map(SessionKeys.sessionId -> hc.sessionId.map(_.value).getOrElse(""))))
    wsClient
      .url(s"$baseUrl$path")
      .withCookies(
        DefaultWSCookie(sessionCookie.name, sessionCookieCrypto.crypto.encrypt(PlainText(sessionCookie.value)).value)
      )
      .addHttpHeaders(play.api.http.HeaderNames.USER_AGENT -> "it-test")
  }

  final def backchannelRequest(path: String)(implicit hc: HeaderCarrier): StandaloneWSRequest = {
    val sessionCookie =
      sessionCookieBaker
        .encodeAsCookie(Session(Map(SessionKeys.sessionId -> hc.sessionId.map(_.value).getOrElse(""))))
    wsClient
      .url(s"$backchannelBaseUrl$path")
      .withCookies(
        DefaultWSCookie(
          sessionCookie.name,
          sessionCookieCrypto.crypto.encrypt(PlainText(sessionCookie.value)).value
        )
      )
      .addHttpHeaders(play.api.http.HeaderNames.USER_AGENT -> "it-test")
  }

  final def requestWithCookies(path: String, cookies: (String, String)*)(
    implicit hc: HeaderCarrier): StandaloneWSRequest = {
    val sessionCookie =
      sessionCookieBaker
        .encodeAsCookie(Session(Map(SessionKeys.sessionId -> hc.sessionId.map(_.value).getOrElse(""))))

    wsClient
      .url(s"$baseUrl$path")
      .withCookies(
        cookies.map(c => DefaultWSCookie(c._1, c._2)) :+ DefaultWSCookie(
          sessionCookie.name,
          sessionCookieCrypto.crypto.encrypt(PlainText(sessionCookie.value)).value
        ): _*
      )
      .addHttpHeaders(play.api.http.HeaderNames.USER_AGENT -> "it-test")
  }

  final val nonEmptyFileUploads: FileUploads = FileUploads(Seq(TestData.acceptedFileUpload))

  final def nFileUploads(n: Int): FileUploads = FileUploads(files = for (_ <- 1 to n) yield TestData.acceptedFileUpload)

  final def hostUserAgent: String = HostService.Any.userAgent

  final def FILES_LIMIT: Int = fileUploadSessionConfig.maximumNumberOfFiles

  final def setContext(context: FileUploadContext = FileUploadContext(fileUploadSessionConfig)): CacheItem =
    await(newJourneyRepo.put(getJourneyId)(DataKeys.journeyContext, context))

  final def setFileUploads(files: FileUploads = FileUploads()): CacheItem =
    await(newJourneyRepo.put(getJourneyId)(DataKeys.uploadedFiles, files))

  final def getContext(): Option[FileUploadContext] =
    await(newJourneyRepo.get(getJourneyId)(DataKeys.journeyContext))

  final def getFileUploads(): Option[FileUploads] =
    await(newJourneyRepo.get(getJourneyId)(DataKeys.uploadedFiles))

}
