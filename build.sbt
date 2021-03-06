
import scoverage.ScoverageKeys
import play.routes.compiler.InjectedRoutesGenerator
import sbt.Keys._
import sbt._
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

import uk.gov.hmrc._
import DefaultBuildSettings._
import play.sbt.routes.RoutesKeys.routesGenerator
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "ers-submissions"

lazy val scoverageSettings = {
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;.*ERSRequest.*;models/.data/..*;prod.*;app.*;models.*;.*BuildInfo.*;view.*;.*Connector.*;repositories.*;.*Config;.*Global.*;prod.Routes;testOnlyDoNotUseInAppConf.Routes;.*Configuration;.*AuthFilter;.*AuditFilter;.*LoggingFilter;.*Metrics;.*WSHttp.*",
    ScoverageKeys.coverageMinimum := 75,
    ScoverageKeys.coverageFailOnMinimum := false,
    parallelExecution in Test := false
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(scoverageSettings : _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    targetJvm := "jvm-1.8",
    scalaVersion := "2.12.12",
    libraryDependencies ++= AppDependencies(),
    routesGenerator := InjectedRoutesGenerator
  )
  .configs(IntegrationTest)
  .settings(integrationTestSettings())
  .settings(majorVersion := 1)
  .settings(PlayKeys.playDefaultPort := 9292)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427

scalacOptions ++= Seq(
  "-P:silencer:pathFilters=views;routes"
)
