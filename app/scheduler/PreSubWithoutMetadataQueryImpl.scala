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

package scheduler

import com.google.inject.Inject
import org.apache.pekko.actor.ActorSystem
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import scheduler.SchedulingActor.PreSubWithoutMetadataQueryClass
import services.PresSubWithoutMetadataQueryService

class PreSubWithoutMetadataQueryImpl @Inject()(
                                                val config: Configuration,
                                                val presSubWithoutMetadataQueryService: PresSubWithoutMetadataQueryService,
                                                val applicationLifecycle: ApplicationLifecycle
                                             ) extends ScheduledJob {

  override def jobName: String = "generate-pre-sub-without-metadata-query"
  val actorSystem: ActorSystem = ActorSystem(jobName)
  val scheduledMessage: PreSubWithoutMetadataQueryClass = PreSubWithoutMetadataQueryClass(presSubWithoutMetadataQueryService)

  schedule
}
