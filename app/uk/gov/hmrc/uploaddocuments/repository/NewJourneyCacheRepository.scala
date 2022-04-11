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

package uk.gov.hmrc.uploaddocuments.repository

import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}
import uk.gov.hmrc.uploaddocuments.models.{FileUploadContext, FileUploadInitializationRequest, FileUploads}
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NewJourneyCacheRepository @Inject() (
  mongoComponent: MongoComponent,
  timestampSupport: TimestampSupport,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends CacheRepository(
      mongoComponent = mongoComponent,
      collectionName = "upload-customs-documents-journeys",
      ttl = appConfig.mongoSessionExpiration,
      timestampSupport = timestampSupport
    )

object NewJourneyCacheRepository {
  object DataKeys {
    val journeyConfigDataKey = DataKey[FileUploadContext]("journeyConfig")
    val uploadedFiles = DataKey[FileUploads]("uploadedFiles")
  }
}