
import play.routes.compiler.InjectedRoutesGenerator
import play.sbt.routes.RoutesKeys.routesGenerator
import sbt.*
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

ThisBuild / majorVersion := 2
ThisBuild / scalaVersion := "2.13.16"

val appName = "ers-submissions"

lazy val testSettings = Seq(
  javaOptions ++= Seq(
    "-Dconfig.resource=test.application.conf"
  )
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(CodeCoverageSettings())
  .settings(scalaSettings)
  .settings(defaultSettings())
  .settings(
    scalaVersion := "2.13.16",
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
