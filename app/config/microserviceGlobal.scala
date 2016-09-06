/*
 * Copyright 2016 HM Revenue & Customs
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

package config

import com.typesafe.config.Config
import models.ResubmissionLock
import net.ceedubs.ficus.Ficus._
import org.joda.time.{Duration, Days, DateTime}
import play.api.{Application, Configuration, Logger, Play}
import repositories.Repositories
import services.MonitoringService
import services.resubmission.SchedulerService
import uk.gov.hmrc.play.audit.filters.AuditFilter
import uk.gov.hmrc.play.auth.controllers.AuthParamsControllerConfig
import uk.gov.hmrc.play.auth.microservice.filters.AuthorisationFilter
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode}
import uk.gov.hmrc.play.http.logging.filters.LoggingFilter
import uk.gov.hmrc.play.microservice.bootstrap.DefaultMicroserviceGlobal
import config.ApplicationConfig._
import scala.concurrent.ExecutionContext.Implicits.global

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}

object AuthParamsControllerConfiguration extends AuthParamsControllerConfig {
  lazy val controllerConfigs = ControllerConfiguration.controllerConfigs
}

object MicroserviceAuditFilter extends AuditFilter with AppName {
  override val auditConnector = MicroserviceAuditConnector
  override def controllerNeedsAuditing(controllerName: String): Boolean = ControllerConfiguration.paramsForController(controllerName).needsAuditing
}

object MicroserviceLoggingFilter extends LoggingFilter {
  override def controllerNeedsLogging(controllerName: String): Boolean = ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object MicroserviceAuthFilter extends AuthorisationFilter {
  override lazy val authParamsConfig = AuthParamsControllerConfiguration
  override lazy val authConnector = MicroserviceAuthConnector
  override def controllerNeedsAuth(controllerName: String): Boolean = ControllerConfiguration.paramsForController(controllerName).needsAuth
}

object MicroserviceGlobal extends DefaultMicroserviceGlobal with RunMode {

  override val auditConnector = MicroserviceAuditConnector

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig(s"$env.microservice.metrics")

  override val loggingFilter = MicroserviceLoggingFilter

  override val microserviceAuditFilter = MicroserviceAuditFilter

  override val authFilter = Some(MicroserviceAuthFilter)

  override def onStart(app: Application) = {
    super.onStart(app)

    val monitoringService: MonitoringService = MonitoringService

    def buildDateTime(year: Int, month: Int, day: Int) = new DateTime(year, month, day, 0, 0, 0)

    if(isSchedulerEnabled) {
      Logger.info("Scheduler is enabled")
      if(schedulerSync && schedulerSchemeRefListEnabled) {
        // check for missing records in jsonStoreInfo and add them if there are such ones
        val lock = ResubmissionLock("ers-scheduler-sync", new Duration(ApplicationConfig.schedulerLockExpireMin * 60000), Repositories.lockRepository)
        lock.tryToAcquireOrRenewLock {
          monitoringService.syncMetadataAndJsonStoreInfoBySchemeRef(schedulerSchemeRefList)
        }
      }
      SchedulerService.run
    }

    if(ApplicationConfig.monitoringEnabled) {

      // Log schemeRefs of JsonStoreInfo by status
      ApplicationConfig.monitoringStatuses.map { status =>
        monitoringService.jsonStoreInfoState(status)
      }

      // Log schemeRefs for metadata that doesn't have related presubmission data
      if(ApplicationConfig.monitoringPresubmission) {
        val initialtDate: DateTime = buildDateTime(
          ApplicationConfig.monitoringPresubmissionInitYear,
          ApplicationConfig.monitoringPresubmissionInitMonth,
          ApplicationConfig.monitoringPresubmissionInitDay
        )
        for (i <- 0 to ApplicationConfig.monitoringPresubmissionPeriodDays) {
          val startDate: DateTime = initialtDate.plusDays(i)
          val endDate: DateTime = startDate.plusDays(1)
          monitoringService.missingPresubmissionData(startDate, endDate)
        }
      }

    }

    // add missing records for JsonStoreInfo for given period
    if(ApplicationConfig.syncCompleteStoreInfo) {
      val initialDate: DateTime = buildDateTime(
        ApplicationConfig.syncCompleteStoreInfoInitYear,
        ApplicationConfig.syncCompleteStoreInfoInitMonth,
        ApplicationConfig.syncCompleteStoreInfoInitDay
      )
      val finaltDate: DateTime = buildDateTime(
        ApplicationConfig.syncCompleteStoreInfoFinalYear,
        ApplicationConfig.syncCompleteStoreInfoFinalMonth,
        ApplicationConfig.syncCompleteStoreInfoFinalDay
      )

      val endDate: DateTime = if(finaltDate.isAfterNow) DateTime.now() else finaltDate
      val numberOfDays = Days.daysBetween(initialDate, endDate).getDays()

      for(i <- 0 to numberOfDays) {
        val lock = ResubmissionLock(s"syncLock${i}", new Duration(3600000), Repositories.lockRepository)
        lock.tryToAcquireOrRenewLock {
          val startDate: DateTime = initialDate.plusDays(i)
          val endDate: DateTime = startDate.plusDays(1)
          monitoringService.syncMetadataAndJsonStoreInfo(startDate, endDate)
        }
      }
    }

  }
}
