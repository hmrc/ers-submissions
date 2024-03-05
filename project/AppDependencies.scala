import sbt.*

object AppDependencies {
  private val bootstrapVersion = "7.23.0"
  private val alpakkaVersion = "4.0.0"
  private val mongoVersion = "1.7.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"        %% "bootstrap-backend-play-28"          % bootstrapVersion,
    "uk.gov.hmrc.mongo"  %% "hmrc-mongo-play-28"                 % mongoVersion,
    "uk.gov.hmrc"        %% "domain"                             % "8.3.0-play-28",
    "com.lightbend.akka" %% "akka-stream-alpakka-csv"            % alpakkaVersion,
    "com.enragedginger"  %% "akka-quartz-scheduler"              % "1.9.3-akka-2.6.x",
    "org.typelevel"      %% "cats-core"                          % "2.10.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-test-play-28"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-test-play-28" % mongoVersion,
    "org.scalatest"                %% "scalatest"               % "3.2.18",
    "com.vladsch.flexmark"          % "flexmark-all"            % "0.64.8",
    "org.scalatestplus"            %% "mockito-5-10"            % "3.2.18.0",
    "com.typesafe.akka"            %% "akka-testkit"            % "2.6.21",
    "org.wiremock"                  % "wiremock-standalone"     % "3.4.2",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"    % "2.16.1"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test
}
