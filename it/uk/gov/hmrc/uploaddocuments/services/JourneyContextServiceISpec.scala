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

import org.mongodb.scala.bson.BsonDocument
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.uploaddocuments.repository.JourneyCacheRepository
import uk.gov.hmrc.uploaddocuments.repository.JourneyCacheRepository.DataKeys
import uk.gov.hmrc.uploaddocuments.support.TestData._
import uk.gov.hmrc.uploaddocuments.support.{AppISpec, LogCapturing}

class JourneyContextServiceISpec extends AppISpec with LogCapturing with BeforeAndAfterEach {

  implicit lazy val ec = scala.concurrent.ExecutionContext.Implicits.global

  lazy val repo = app.injector.instanceOf[JourneyCacheRepository]
  lazy val testjourneyContextService = app.injector.instanceOf[JourneyContextService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repo.collection.deleteMany(BsonDocument()).toFuture())
    await(repo.collection.countDocuments().toFuture()) shouldBe 0
  }

  "JourneyContextService" when {

    "calling .putJourneyContext()" should {

      "insert when no existing journey present" in {

        await(repo.collection.countDocuments().toFuture()) shouldBe 0

        await(testjourneyContextService.putJourneyContext(fileUploadContext)(journeyId))

        await(repo.collection.countDocuments().toFuture()) shouldBe 1
        await(repo.get(journeyId)(DataKeys.journeyContext)) shouldBe Some(fileUploadContext)
      }

      "update when existing journey present" in {

        await(repo.collection.countDocuments().toFuture()) shouldBe 0

        await(testjourneyContextService.putJourneyContext(fileUploadContext)(journeyId))
        await(repo.collection.countDocuments().toFuture()) shouldBe 1

        val updatedRecord = fileUploadContext.copy(config = fileUploadContext.config.copy(continueUrl = "foo"))

        await(testjourneyContextService.putJourneyContext(updatedRecord)(journeyId))

        await(repo.collection.countDocuments().toFuture()) shouldBe 1
        await(repo.get(journeyId)(DataKeys.journeyContext)) shouldBe Some(updatedRecord)
      }
    }

    "calling .getJourneyContext()" should {

      "return None when no journey exists" in {

        await(repo.collection.countDocuments().toFuture()) shouldBe 0

        await(testjourneyContextService.getJourneyContext()(journeyId)) shouldBe None
      }

      "return FileUploadContext when journey exist" in {

        await(repo.collection.countDocuments().toFuture()) shouldBe 0

        await(testjourneyContextService.putJourneyContext(fileUploadContext)(journeyId))
        await(repo.collection.countDocuments().toFuture()) shouldBe 1

        await(testjourneyContextService.getJourneyContext()(journeyId)) shouldBe Some(fileUploadContext)
      }
    }
  }
}
