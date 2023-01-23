/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.uploaddocuments.utils.LoggerUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JourneyContextService @Inject() (repo: JourneyCacheRepository)(implicit ec: ExecutionContext) extends LoggerUtil {

  def getJourneyContext()(implicit journeyId: JourneyId): Future[Option[FileUploadContext]] =
    repo.get(journeyId.value)(DataKeys.journeyContext)

  def putJourneyContext(journeyContext: FileUploadContext)(implicit journeyId: JourneyId): Future[CacheItem] =
    repo.put(journeyId.value)(DataKeys.journeyContext, journeyContext)

  def withJourneyContext[T](
    journeyNotFoundResult: => Future[T]
  )(
    journeyNotActiveResult: FileUploadContext => Future[T]
  )(body: FileUploadContext => Future[T])(implicit journeyId: JourneyId): Future[T] =
    getJourneyContext().flatMap(_.fold {
      Logger.error("[withFiles] No files exist for the supplied journeyID, redirecting user to gov.uk")
      Logger.debug(s"[withFiles] journeyId: '$journeyId'")
      journeyNotFoundResult
    } { c =>
      if (c.active)
        body(c)
      else {
        Logger.info("[withFiles] Files are already not available, sending user off to the host.")
        Logger.debug(s"[withFiles] journeyId: '$journeyId'")
        journeyNotActiveResult(c)
      }
    })
}
