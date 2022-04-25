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

import uk.gov.hmrc.mongo.cache.{CacheIdType, DataKey, MongoCacheRepository}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}
import uk.gov.hmrc.uploaddocuments.models.{FileUploadContext, FileUploads}
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class JourneyCacheRepository @Inject()(
  mongoComponent: MongoComponent,
  timestampSupport: TimestampSupport,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends MongoCacheRepository(
      mongoComponent   = mongoComponent,
      collectionName   = "upload-customs-documents-journeys",
      ttl              = appConfig.mongoSessionExpiration,
      timestampSupport = timestampSupport,
      replaceIndexes   = true,
      cacheIdType      = CacheIdType.SimpleCacheId
    )

object JourneyCacheRepository {
  object DataKeys {
    val journeyContext: DataKey[FileUploadContext] = DataKey[FileUploadContext]("journeyConfig")
    val uploadedFiles: DataKey[FileUploads]        = DataKey[FileUploads]("uploadedFiles")
  }
}
