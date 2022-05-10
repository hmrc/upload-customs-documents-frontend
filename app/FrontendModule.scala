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

import com.google.inject.AbstractModule
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.http.ws.UploadHttpClient
import uk.gov.hmrc.uploaddocuments.connectors.FrontendAuthConnector
import uk.gov.hmrc.uploaddocuments.controllers.actions.{JourneyContextAction, JourneyContextActionI, SessionAction, SessionActionI}

class FrontendModule(val environment: Environment, val configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[HttpGet]).to(classOf[UploadHttpClient])
    bind(classOf[HttpPost]).to(classOf[UploadHttpClient])
    bind(classOf[AuthConnector]).to(classOf[FrontendAuthConnector])
    bind(classOf[SessionAction]).to(classOf[SessionActionI])
    bind(classOf[JourneyContextAction]).to(classOf[JourneyContextActionI])
  }
}
