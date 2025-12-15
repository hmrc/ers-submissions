import sbt.*
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin

ThisBuild / majorVersion := 1
ThisBuild / scalaVersion := "2.13.16"

lazy val testSettings = Seq(
  javaOptions ++= Seq(
    "-Dconfig.resource=test.application.conf"
  )
)

lazy val microservice = Project("ers-submissions", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(CodeCoverageSettings())
  .settings(libraryDependencies ++= AppDependencies())
  .settings(inConfig(Test)(testSettings))
  .settings(PlayKeys.playDefaultPort := 9292)

scalacOptions ++= Seq(
  "-Wconf:src=routes/.*:s"
)

lazy val it = project
  .enablePlugins(PlayScala)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .dependsOn(microservice % "test->test")
  .settings(testSettings)
