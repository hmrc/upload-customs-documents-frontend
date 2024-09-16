/*
 * Copyright 2024 HM Revenue & Customs
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

// /*
//  * Copyright 2024 HM Revenue & Customs
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  *
//  *     http://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */

// package uk.gov.hmrc.uploaddocuments.repository

// import org.apache.pekko.actor.{ActorSystem, Scheduler}
// import org.scalamock.handlers.CallHandler3
// import org.scalamock.scalatest.MockFactory
// import uk.gov.hmrc.mongo.lock.LockRepository
// import uk.gov.hmrc.uploaddocuments.models.JourneyId
// import uk.gov.hmrc.uploaddocuments.support.{LogCapturing, UnitSpec}
// import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

// import java.util.concurrent.TimeUnit
// import scala.concurrent.Future
// import scala.concurrent.duration.Duration
// import scala.concurrent.ExecutionContext
// import uk.gov.hmrc.mongo.lock.Lock
// import java.time.Instant

// class JourneyLockingSpec extends UnitSpec with MockFactory with LogCapturing {

//   implicit val journeyId: JourneyId = JourneyId("foo")
//   implicit val scheduler: Scheduler = ActorSystem("JourneyLockingTestsActor").scheduler
//   implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

//   class fixture {
//     lazy val mockLockRepo             = mock[LockRepository]
//     lazy val mockAppConfig: AppConfig = mock[AppConfig]

//     object TestJourneyLocking extends JourneyLocking {
//       override val appConfig: AppConfig                   = mockAppConfig
//       override val lockRepositoryProvider: LockRepository = mockLockRepo
//     }

//     (() => mockAppConfig.lockTimeout).expects().returns(Duration(1, TimeUnit.SECONDS)).anyNumberOfTimes()
//     (() => mockAppConfig.lockReleaseCheckInterval)
//       .expects()
//       .returns(Duration(100, TimeUnit.MILLISECONDS))
//       .anyNumberOfTimes()
//     (mockLockRepo
//       .releaseLock(_: String, _: String))
//       .expects(*, *)
//       .returns(Future.successful((): Unit))
//       .anyNumberOfTimes()

//     def mockTakeLock(
//       isTaken: Future[Option[Lock]] = Future.successful(Some(Lock(journeyId.value, "Foo", Instant.now(), Instant.MAX)))
//     ): CallHandler3[String, String, Duration, Future[Option[Lock]]] =
//       (mockLockRepo
//         .takeLock(_: String, _: String, _: Duration))
//         .expects(journeyId.value, *, mockAppConfig.lockTimeout)
//         .returns(isTaken)

//   }

//   "JourneyLocking" when {

//     "a lock is taken first time" must {

//       "return the f result" in new fixture {

//         withCaptureOfLoggingFrom(TestJourneyLocking.logger) { logs =>
//           mockTakeLock().once()

//           val result = TestJourneyLocking.takeLock(Future.successful("TimedOut"))(Future.successful("Executed"))

//           await(result) shouldBe "Executed"
//         }
//       }
//     }

//     "a lock is not taken first time" when {

//       "repeatedly attempts until timeout" must {

//         "return the timeout result" in new fixture {

//           withCaptureOfLoggingFrom(TestJourneyLocking.logger) { logs =>
//             mockTakeLock(Future.successful(None)).repeat(10)
//             val result = TestJourneyLocking.takeLock(Future.successful("TimedOut"))(Future.successful("Executed"))

//             await(result) shouldBe "TimedOut"

//             logExists(
//               s"[tryLock] for journeyId: '$journeyId' was locked. Waiting for 100 millis before trying again.",
//               nTimes = 9
//             )(logs)
//             logExists(s"[tryLock] for journeyId: '$journeyId' was not released - could not process")(logs)
//           }
//         }
//       }

//       "repeatedly attempts until lock is taken" must {

//         "return the f result" in new fixture {

//           withCaptureOfLoggingFrom(TestJourneyLocking.logger) { logs =>
//             mockTakeLock(Future.successful(None)).repeat(4)
//             mockTakeLock().once()

//             val result = TestJourneyLocking.takeLock(Future.successful("TimedOut"))(Future.successful("Executed"))
//             await(result) shouldBe "Executed"

//             logExists(
//               s"[tryLock] for journeyId: '$journeyId' was locked. Waiting for 100 millis before trying again.",
//               nTimes = 4
//             )(logs)
//           }
//         }
//       }
//     }

//     "an unexpected error occurs" must {

//       "throw exception with log" in new fixture {

//         withCaptureOfLoggingFrom(TestJourneyLocking.logger) { logs =>
//           val exception = new Exception("error")
//           mockTakeLock(Future.failed(exception)).once()

//           val result = TestJourneyLocking.takeLock(Future.successful("TimedOut"))(Future.successful("Executed"))

//           intercept[Exception](await(result))

//           logExists(s"[tryLock] for journeyId: '$journeyId' failed with exception ${exception.getMessage}")(logs)
//         }
//       }
//     }
//   }
// }
