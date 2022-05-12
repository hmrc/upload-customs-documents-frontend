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

package uk.gov.hmrc.uploaddocuments.controllers.actions

import com.google.inject.Inject
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.uploaddocuments.models.requests.{AuthRequest, JourneyContextRequest}
import uk.gov.hmrc.uploaddocuments.services.JourneyContextService
import uk.gov.hmrc.uploaddocuments.utils.LoggerUtil
import uk.gov.hmrc.uploaddocuments.wiring.AppConfig

import scala.concurrent.{ExecutionContext, Future}

trait JourneyContextAction extends ActionFunction[AuthRequest, JourneyContextRequest]


class JourneyContextActionI @Inject()(val journeyContextService: JourneyContextService,
                                      appConfig: AppConfig)
                                   (implicit val executionContext: ExecutionContext) extends JourneyContextAction with LoggerUtil {

  override def invokeBlock[A](request: AuthRequest[A], block: JourneyContextRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(session = request.session, request = request)
    journeyContextService.withJourneyContext(Future.successful(Redirect(appConfig.govukStartUrl))) { context =>
      block(JourneyContextRequest(request, context))
    }(request.journeyId)
  }
}
