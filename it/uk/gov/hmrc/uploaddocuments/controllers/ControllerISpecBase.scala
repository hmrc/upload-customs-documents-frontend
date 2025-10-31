package uk.gov.hmrc.uploaddocuments.controllers

import play.api.libs.ws.{DefaultWSCookie, StandaloneWSRequest}
import play.api.mvc.*
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.http.{HeaderCarrier, SessionId, SessionKeys}
import uk.gov.hmrc.mongo.cache.CacheItem
import uk.gov.hmrc.uploaddocuments.models.*
import uk.gov.hmrc.uploaddocuments.repository.JourneyCacheRepository
import uk.gov.hmrc.uploaddocuments.repository.JourneyCacheRepository.DataKeys
import uk.gov.hmrc.uploaddocuments.support.{SHA256, ServerISpec, TestData}
import uk.gov.hmrc.http.HeaderNames

trait ControllerISpecBase extends ServerISpec {

  val journeyId = "sadasdjkasdhuqyhwa326176318346674e764764"
  val sessionId = SessionId(journeyId)

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(sessionId))
  def getJourneyId: String       = SHA256.compute(journeyId)

  lazy val newJourneyRepo = app.injector.instanceOf[JourneyCacheRepository]

  import play.api.i18n.*
  implicit val messages: Messages = MessagesImpl(Lang("en"), app.injector.instanceOf[MessagesApi])

  def sessionCookie(implicit hc: HeaderCarrier) = sessionCookieBaker
    .encodeAsCookie(
      Session(
        Map(
          SessionKeys.sessionId -> hc.sessionId.map(_.value).getOrElse(""),
          SessionKeys.authToken -> "Bearer XYZ"
        )
      )
    )

  def withHeaders(f: => StandaloneWSRequest): StandaloneWSRequest =
    f.addHttpHeaders(
      play.api.http.HeaderNames.USER_AGENT    -> "it-test",
      play.api.http.HeaderNames.AUTHORIZATION -> "Bearer XYZ"
    )

  final def request(path: String)(implicit hc: HeaderCarrier): StandaloneWSRequest =
    withHeaders {
      wsClient
        .url(s"$baseUrl$path")
        .withCookies(
          DefaultWSCookie(
            name = sessionCookie.name,
            value = sessionCookieCrypto.crypto.encrypt(PlainText(sessionCookie.value)).value,
            secure = true
          )
        )
    }

  final def backchannelRequest(path: String)(implicit hc: HeaderCarrier): StandaloneWSRequest =
    withHeaders {
      wsClient
        .url(s"$backchannelBaseUrl$path")
        .withHttpHeaders(HeaderNames.xSessionId -> hc.sessionId.map(_.value).getOrElse(""))
    }

  final def requestWithCookies(path: String, cookies: (String, String)*)(implicit
    hc: HeaderCarrier
  ): StandaloneWSRequest =
    withHeaders {
      wsClient
        .url(s"$baseUrl$path")
        .withCookies(
          cookies.map(c => DefaultWSCookie(name = c._1, value = c._2, secure = true)) :+ DefaultWSCookie(
            name = sessionCookie.name,
            value = sessionCookieCrypto.crypto.encrypt(PlainText(sessionCookie.value)).value,
            secure = true
          ): _*
        )
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
