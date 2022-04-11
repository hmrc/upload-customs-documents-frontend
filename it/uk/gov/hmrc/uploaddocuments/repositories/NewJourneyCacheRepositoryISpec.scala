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

package uk.gov.hmrc.uploaddocuments.repositories

import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.uploaddocuments.models.{FileUploadInitializationRequest, FileUploadSessionConfig, Nonce}
import uk.gov.hmrc.uploaddocuments.repository.NewJourneyCacheRepository
import uk.gov.hmrc.uploaddocuments.support.{BaseISpec, MongoHelpers}

class NewJourneyCacheRepositoryISpec extends BaseISpec with MongoHelpers {

  lazy val app = appBuilder.build()

  lazy val repo: NewJourneyCacheRepository = app.injector.instanceOf[NewJourneyCacheRepository]

  val journeyId = "foo"

  val dummyConfig = FileUploadSessionConfig(
    Nonce.toNonce(12345),
    "continueUrl",
    "backlinkUrl",
    "callbackUrl"
  )
  val dummyRequest = FileUploadInitializationRequest(
    dummyConfig,
    Seq()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    removeAll(repo)
    await(repo.collection.countDocuments().toFuture()) shouldBe 0
  }

  ".journeyConfigDataKey" must {

    "have the correct key and model type" in {
      repo.journeyConfigDataKey shouldBe DataKey[FileUploadInitializationRequest]("journeyConfig")
    }
  }

  ".storeJourneyConfig(id: String)(data: FileUploadInitializationRequest)" when {

    "no record exists" must {

      "upsert the first" in {

        await(repo.storeJourneyConfig(journeyId)(dummyRequest))
        await(repo.collection.countDocuments().toFuture()) shouldBe 1

        await(repo.get[FileUploadInitializationRequest](journeyId)(repo.journeyConfigDataKey)) shouldBe Some(
          dummyRequest
        )
      }
    }

    "record already exists" must {

      "update existing" in {

        await(repo.storeJourneyConfig(journeyId)(dummyRequest))
        await(repo.collection.countDocuments().toFuture()) shouldBe 1

        val updatedModel = dummyRequest.copy(config = dummyConfig.copy(initialNumberOfEmptyRows = 10))

        await(repo.storeJourneyConfig(journeyId)(updatedModel))
        await(repo.collection.countDocuments().toFuture()) shouldBe 1

        await(repo.get[FileUploadInitializationRequest](journeyId)(repo.journeyConfigDataKey)) shouldBe Some(
          updatedModel
        )
      }
    }
  }

  ".getJourneyConfig(id: String)" when {

    "record exists" must {

      "return the journey config" in {

        await(repo.storeJourneyConfig(journeyId)(dummyRequest))
        await(repo.collection.countDocuments().toFuture()) shouldBe 1

        await(repo.getJourneyConfig(journeyId)) shouldBe Some(dummyRequest)
      }
    }

    "record does not exist" must {

      "return None" in {
        await(repo.getJourneyConfig(journeyId)) shouldBe None
      }
    }
  }
}
