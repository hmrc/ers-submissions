import sbt.*

object AppDependencies {
  private val bootstrapVersion = "7.21.0"
  private val alpakkaVersion = "3.0.4"
  private val mongoVersion = "1.3.0"

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"        %% "bootstrap-backend-play-28"          % bootstrapVersion,
    "uk.gov.hmrc.mongo"  %% "hmrc-mongo-play-28"                 % mongoVersion,
    "uk.gov.hmrc"        %% "domain"                             % "8.3.0-play-28",
    "com.lightbend.akka" %% "akka-stream-alpakka-csv"            % alpakkaVersion,
    "com.enragedginger"  %% "akka-quartz-scheduler"              % "1.9.3-akka-2.6.x",
    "org.typelevel"      %% "cats-core"                          % "2.9.0"
  )

  private val scalaTestVersion = "3.2.16"
  private val flexmarkAllVersion = "0.64.8"

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-test-play-28"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-test-play-28" % mongoVersion,
    "org.scalatest"                %% "scalatest"               % "3.2.16",
    "com.vladsch.flexmark"          % "flexmark-all"            % "0.64.8",
    "org.scalatestplus"            %% "mockito-4-11"            % "3.2.16.0",
    "com.typesafe.akka"            %% "akka-testkit"            % "2.6.21",
    "com.github.tomakehurst"        % "wiremock-standalone"     % "2.27.2",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"    % "2.15.2"
  ).map(_ % "test, it")

  def all: Seq[ModuleID] = compile ++ test
}
