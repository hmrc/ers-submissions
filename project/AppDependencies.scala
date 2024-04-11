import sbt.*

object AppDependencies {
  private val bootstrapVersion = "8.5.0"
  private val pekkoVersion = "1.0.2"
  private val mongoVersion = "1.8.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"    % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"           % mongoVersion,
    "uk.gov.hmrc"             %% "domain-play-30"               % "9.0.0",
    "org.apache.pekko"        %% "pekko-connectors-csv"         % pekkoVersion,
    "org.typelevel"           %% "cats-core"                    % "2.10.0",
    "io.github.samueleresca"  %% "pekko-quartz-scheduler"       % "1.2.0-pekko-1.0.x"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-test-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-test-play-30" % mongoVersion,
    "org.scalatest"                %% "scalatest"               % "3.2.18",
    "com.vladsch.flexmark"          % "flexmark-all"            % "0.64.8",
    "org.apache.pekko"             %% "pekko-testkit"           % pekkoVersion,
    "com.fasterxml.jackson.module" %% "jackson-module-scala"    % "2.17.0",
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
