package uk.gov.hmrc.uploaddocuments.controllers

import play.api.libs.ws.{DefaultWSCookie, StandaloneWSRequest}
import play.api.mvc._
import play.api.test.FakeRequest
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.http.{HeaderCarrier, SessionId, SessionKeys}
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.repository.NewJourneyCacheRepository
import uk.gov.hmrc.uploaddocuments.repository.NewJourneyCacheRepository.DataKeys
import uk.gov.hmrc.uploaddocuments.support.{SHA256, ServerISpec, TestData}

import java.time.ZonedDateTime

trait ControllerISpecBase extends ServerISpec {

  val journeyId = "sadasdjkasdhuqyhwa326176318346674e764764"
  val sessionId = SessionId(journeyId)

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(sessionId))
  def getJourneyId: String = SHA256.compute(journeyId)

  lazy val newJourneyRepo = app.injector.instanceOf[NewJourneyCacheRepository]

  import play.api.i18n._
  implicit val messages: Messages = MessagesImpl(Lang("en"), app.injector.instanceOf[MessagesApi])

  final def fakeRequest(cookies: Cookie*)(implicit
                                          hc: HeaderCarrier
  ): Request[AnyContent] =
    fakeRequest("GET", "/", cookies: _*)

  final def fakeRequest(method: String, path: String, cookies: Cookie*)(implicit
                                                                        hc: HeaderCarrier
  ): Request[AnyContent] =
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
        DefaultWSCookie(
          sessionCookie.name,
          sessionCookieCrypto.crypto.encrypt(PlainText(sessionCookie.value)).value
        )
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

  final def requestWithCookies(path: String, cookies: (String, String)*)(implicit
                                                                         hc: HeaderCarrier
  ): StandaloneWSRequest = {
    val sessionCookie =
      sessionCookieBaker
        .encodeAsCookie(Session(Map(SessionKeys.sessionId -> hc.sessionId.map(_.value).getOrElse(""))))

    wsClient
      .url(s"$baseUrl$path")
      .withCookies(
        (cookies.map(c => DefaultWSCookie(c._1, c._2)) :+ DefaultWSCookie(
          sessionCookie.name,
          sessionCookieCrypto.crypto.encrypt(PlainText(sessionCookie.value)).value
        )): _*
      )
      .addHttpHeaders(play.api.http.HeaderNames.USER_AGENT -> "it-test")
  }

  final def nonEmptyFileUploads = FileUploads(files =
    Seq(
      FileUpload.Accepted(
        Nonce.Any,
        Timestamp.Any,
        "f029444f-415c-4dec-9cf2-36774ec63ab8",
        "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
        ZonedDateTime.parse("2018-04-24T09:30:00Z"),
        "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
        "test.pdf",
        "application/pdf",
        4567890
      )
    )
  )

  final def nFileUploads(n: Int): FileUploads =
    FileUploads(files = for (i <- 1 to n) yield TestData.acceptedFileUpload)

  final def hostUserAgent: String = HostService.Any.userAgent

  final val fileUploadSessionConfig =
    FileUploadSessionConfig(
      nonce = Nonce.random,
      continueUrl = s"$wireMockBaseUrlAsString/continue-url",
      backlinkUrl = s"$wireMockBaseUrlAsString/backlink-url",
      callbackUrl = s"$wireMockBaseUrlAsString/result-post-url"
    )

  final def FILES_LIMIT = fileUploadSessionConfig.maximumNumberOfFiles

  final def setContext(context: FileUploadContext = FileUploadContext(fileUploadSessionConfig)) =
    await(newJourneyRepo.put(getJourneyId)(DataKeys.journeyContextDataKey, context))

  final def setFileUploads(files: FileUploads = FileUploads()) =
    await(newJourneyRepo.put(getJourneyId)(DataKeys.uploadedFiles, files))

  final def getContext() =
    await(newJourneyRepo.get(getJourneyId)(DataKeys.journeyContextDataKey))

  final def getFileUploads() =
    await(newJourneyRepo.get(getJourneyId)(DataKeys.uploadedFiles))

}
