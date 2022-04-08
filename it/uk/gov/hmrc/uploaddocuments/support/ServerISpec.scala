package uk.gov.hmrc.uploaddocuments.support

import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.api.mvc.SessionCookieBaker
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCrypto
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

abstract class ServerISpec extends BaseISpec with GuiceOneServerPerSuite {

  override def fakeApplication: Application = appBuilder.build()

  lazy val appConfig = fakeApplication.injector.instanceOf[AppConfig]
  lazy val sessionCookieBaker: SessionCookieBaker = app.injector.instanceOf[SessionCookieBaker]
  lazy val sessionCookieCrypto: SessionCookieCrypto = app.injector.instanceOf[SessionCookieCrypto]

  def wsClient = {
    import play.shaded.ahc.org.asynchttpclient._
    val asyncHttpClientConfig = new DefaultAsyncHttpClientConfig.Builder()
      .setMaxRequestRetry(0)
      .setShutdownQuietPeriod(0)
      .setShutdownTimeout(0)
      .setFollowRedirect(true)
      .build
    val asyncHttpClient = new DefaultAsyncHttpClient(asyncHttpClientConfig)
    new StandaloneAhcWSClient(asyncHttpClient)
  }

  val baseUrl: String = s"http://localhost:$port/upload-customs-documents"
  val backchannelBaseUrl: String = s"http://localhost:$port/internal"

  def requestWithoutSessionId(path: String) =
    wsClient
      .url(s"$baseUrl$path")

  def backchannelRequestWithoutSessionId(path: String) =
    wsClient
      .url(s"$backchannelBaseUrl$path")

}
