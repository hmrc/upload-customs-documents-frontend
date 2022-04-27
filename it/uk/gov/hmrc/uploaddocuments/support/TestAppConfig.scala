package uk.gov.hmrc.uploaddocuments.support

import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.concurrent.duration._

case class TestAppConfig(
  wireMockBaseUrl: String,
  wireMockPort: Int
) extends AppConfig {

  override val appName: String = "upload-customs-documents"
  override val baseInternalCallbackUrl: String = "http://base.internal.callback"
  override val baseExternalCallbackUrl: String = "http://base.external.callback"
  override val authBaseUrl: String = wireMockBaseUrl
  override val upscanInitiateBaseUrl: String = wireMockBaseUrl
  override val mongoSessionExpiration: Duration = 1.hour
  override val contactHost: String = wireMockBaseUrl
  override val contactFormServiceIdentifier: String = "dummy"
  override val signOutUrl: String = wireMockBaseUrl + "/dummy-sign-out-url"
  override val timeout: Int = 10
  override val countdown: Int = 2
  override val govukStartUrl: String = wireMockBaseUrl + "/dummy-start-url"

  override val fileUploadResultPushRetryIntervals: Seq[FiniteDuration] =
    Seq(FiniteDuration(10, "ms"), FiniteDuration(20, "ms"))

  override val upscanInitialWaitTime: Duration = Duration(2, TimeUnit.SECONDS)
  override val upscanWaitInterval: Duration = Duration(500, TimeUnit.MILLISECONDS)

}
