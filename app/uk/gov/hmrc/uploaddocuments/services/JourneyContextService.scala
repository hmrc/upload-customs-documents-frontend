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

import uk.gov.hmrc.mongo.cache.CacheItem
import uk.gov.hmrc.uploaddocuments.models.{FileUploadContext, JourneyId}
import uk.gov.hmrc.uploaddocuments.repository.JourneyCacheRepository
import uk.gov.hmrc.uploaddocuments.repository.JourneyCacheRepository.DataKeys

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class JourneyContextService @Inject()(repo: JourneyCacheRepository) {

  def getJourneyContext()(implicit journeyId: JourneyId): Future[Option[FileUploadContext]] =
    repo.get(journeyId.value)(DataKeys.journeyContext)

  def putJourneyContext(journeyContext: FileUploadContext)
                                         (implicit journeyId: JourneyId): Future[CacheItem] =
    repo.put(journeyId.value)(DataKeys.journeyContext, journeyContext)
}
