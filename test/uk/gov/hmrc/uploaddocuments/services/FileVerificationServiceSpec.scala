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

package uk.gov.hmrc.uploaddocuments.services

import akka.actor.{ActorSystem, Scheduler}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.uploaddocuments.journeys.TestData
import uk.gov.hmrc.uploaddocuments.models._
import uk.gov.hmrc.uploaddocuments.support.UnitSpec

import java.util.concurrent.TimeUnit
import scala.concurrent.Future
import scala.concurrent.duration._

class FileVerificationServiceSpec extends UnitSpec with MockFileUploadService with TestData {

  override implicit val defaultTimeout: FiniteDuration = 20 seconds

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
  implicit val hc = HeaderCarrier()
  implicit val jid = journeyId

  implicit val scheduler: Scheduler = ActorSystem("FileVerificationTestsActor").scheduler

  object TestFileVerificationService extends FileVerificationService(mockFileUploadService)

  class TestFixture() {

    val timeoutTime = System.nanoTime() + Duration.apply(10, TimeUnit.SECONDS).toNanos

    def actual: Future[String] =
      TestFileVerificationService.waitForUpscanResponse(acceptedFileUpload.reference, 500, timeoutTime)(
        readyResult = file => s"ResponseReceived for ${file.reference}",
        ifTimeout = "TimedOut"
      )
  }

  "FileVerificationService" when {

    "calling .waitForUpscanResponse()" when {

      "the file does NOT exist" must {

        "throw Match Error" in new TestFixture {

          mockWithFiles(journeyId)(Some(FileUploads()))

          intercept[MatchError](await(actual))
        }
      }

      "the file exists" when {

        "the wait for a response dos not time out" when {

          "the file is marked as ready immediately" must {

            "execute the whenReady result" in new TestFixture {

              mockWithFiles(journeyId)(Some(FileUploads(Seq(acceptedFileUpload))))

              await(actual) shouldBe s"ResponseReceived for ${acceptedFileUpload.reference}"
            }
          }

          "the file is marked not ready 3 times, then becomes ready the 4th time" must {

            "eventually, execute the whenReady result" in new TestFixture {

              val postedFileSameReference = fileUploadPosted.copy(reference = acceptedFileUpload.reference)

              mockWithFiles(journeyId)(Some(FileUploads(Seq(postedFileSameReference)))).repeat(3)
              mockWithFiles(journeyId)(Some(FileUploads(Seq(acceptedFileUpload))))

              await(actual) shouldBe s"ResponseReceived for ${acceptedFileUpload.reference}"
            }
          }
        }

        "the wait for a response times out" when {

          "the file is continually marked as not ready" must {

            "eventually, execute the timeout result" in new TestFixture {

              val postedFileSameReference = fileUploadPosted.copy(reference = acceptedFileUpload.reference)

              mockWithFiles(journeyId)(Some(FileUploads(Seq(postedFileSameReference)))).repeat(6)

              await(actual) shouldBe "TimedOut"
            }
          }
        }
      }
    }
  }
}
