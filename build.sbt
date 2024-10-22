
import scoverage.ScoverageKeys
import play.routes.compiler.InjectedRoutesGenerator

import sbt.*
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin

import play.sbt.routes.RoutesKeys.routesGenerator
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

ThisBuild / majorVersion := 2
ThisBuild / scalaVersion := "2.13.12"

val appName = "ers-submissions"

lazy val scoverageSettings = {
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;.*ERSRequest.*;models/.data/..*;prod.*;app.*;models.*;.*BuildInfo.*;view.*;.*Connector.*;repositories.*;.*Config;.*Global.*;prod.Routes;testOnlyDoNotUseInAppConf.*;.*Configuration;.*AuthFilter;.*AuditFilter;.*LoggingFilter;.*Metrics;.*WSHttp.*",
    ScoverageKeys.coverageMinimumStmtTotal := 86,
    ScoverageKeys.coverageFailOnMinimum := true,
  )
}

lazy val testSettings = Seq(
  javaOptions ++= Seq(
    "-Dconfig.resource=test.application.conf"
  )
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(scoverageSettings)
  .settings(scalaSettings)
  .settings(defaultSettings())
  .settings(
    scalaVersion := "2.13.15",
    libraryDependencies ++= AppDependencies(),
    libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always),
    routesGenerator := InjectedRoutesGenerator
  )
  .settings(inConfig(Test)(testSettings))
  .settings(majorVersion := 1)
  .settings(PlayKeys.playDefaultPort := 9292)

scalacOptions ++= Seq(
  "-Wconf:src=routes/.*:s"
)

lazy val it = project
  .enablePlugins(PlayScala)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .dependsOn(microservice % "test->test")
  .settings(testSettings)

addCommandAlias("scalastyleAll", "all scalastyle Test/scalastyle")
