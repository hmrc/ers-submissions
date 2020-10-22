import sbt.Keys._
import sbt.Tests.{SubProcess, Group}
import sbt._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import scoverage.ScoverageKeys
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning

trait MicroService {

  import uk.gov.hmrc._
  import DefaultBuildSettings._
  import uk.gov.hmrc.SbtAutoBuildPlugin
  import play.sbt.routes.RoutesCompiler.autoImport._
  import play.sbt.routes.RoutesKeys.routesGenerator
  import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
  import uk.gov.hmrc.versioning.SbtGitVersioning
  import uk.gov.hmrc.SbtArtifactory

  val appName: String

  lazy val appDependencies : Seq[ModuleID] = ???
  lazy val plugins : Seq[Plugins] = Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  lazy val playSettings : Seq[Setting[_]] = Seq.empty

  lazy val scoverageSettings = {
    import scoverage.ScoverageSbtPlugin._
    Seq(
      // Semicolon-separated list of regexs matching classes to exclude
      ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;.*ERSRequest.*;models/.data/..*;prod.*;app.*;models.*;.*BuildInfo.*;view.*;.*Connector.*;repositories.*;.*Config;.*Global.*;prod.Routes;testOnlyDoNotUseInAppConf.Routes;.*Configuration;.*AuthFilter;.*AuditFilter;.*LoggingFilter;.*Metrics;.*WSHttp.*",
      ScoverageKeys.coverageMinimum := 75,
      ScoverageKeys.coverageFailOnMinimum := false,
      parallelExecution in Test := false
    )
  }

  lazy val microservice = Project(appName, file("."))
    .enablePlugins(plugins : _*)
    .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning)
    .enablePlugins(SbtDistributablesPlugin)
    .settings(playSettings ++ scoverageSettings : _*)
    .settings(playSettings : _*)
    .settings(scalaSettings: _*)
    .settings(publishingSettings: _*)
    .settings(defaultSettings(): _*)
    .settings(
      targetJvm := "jvm-1.8",
      scalaVersion := "2.11.12",
      libraryDependencies ++= appDependencies,
      parallelExecution in Test := false,
      fork in Test := false,
      retrieveManaged := true,
      routesGenerator := StaticRoutesGenerator
    )
    .settings(Repositories.playPublishingSettings : _*)
    .configs(IntegrationTest)
    .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
    .settings(integrationTestSettings())
    .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
    .settings(
      evictionWarningOptions in update := EvictionWarningOptions.default.withWarnTransitiveEvictions(false)
        .withWarnDirectEvictions(false)
        .withWarnScalaVersionEviction(false)
    )
    .settings(majorVersion := 1)
}

private object Repositories {

  import uk.gov.hmrc._
  import PublishingSettings._

  lazy val playPublishingSettings : Seq[sbt.Setting[_]] = sbtrelease.ReleasePlugin.releaseSettings ++ Seq(

    credentials += SbtCredentials,

    publishArtifact in(Compile, packageDoc) := false,
    publishArtifact in(Compile, packageSrc) := false
  ) ++
    publishAllArtefacts
}
