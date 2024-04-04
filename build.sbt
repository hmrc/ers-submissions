
import scoverage.ScoverageKeys
import play.routes.compiler.InjectedRoutesGenerator
import sbt.Keys.*
import sbt.*
import uk.gov.hmrc.DefaultBuildSettings.itSettings

import play.sbt.routes.RoutesKeys.routesGenerator

val appName = "ers-submissions"

ThisBuild / majorVersion := 2
ThisBuild / scalaVersion := "2.13.12"

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
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(scoverageSettings)
  .settings(
    libraryDependencies ++= AppDependencies(),
    routesGenerator := InjectedRoutesGenerator
  )
  .settings(inConfig(Test)(testSettings))
  .settings(PlayKeys.playDefaultPort := 9292)

scalacOptions ++= Seq(
  "-Wconf:src=routes/.*:s"
)

lazy val it = project

  .enablePlugins(PlayScala)

  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(itSettings())
  .settings(libraryDependencies ++= AppDependencies.itDependencies)

// For Apache Pekko 1.0.x and Apache Pekko Typed Actors 1.0.x and Scala 2.12.x, 2.13.x, 3.1.x
libraryDependencies += "io.github.samueleresca" %% "pekko-quartz-scheduler" % "1.2.0-pekko-1.0.x"

addCommandAlias("scalastyleAll", "all scalastyle Test/scalastyle it/Test/scalastyle")
