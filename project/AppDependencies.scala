import sbt.*

object AppDependencies {
  private val bootstrapVersion = "9.8.0"
  private val pekkoVersion = "1.0.2"
  private val mongoVersion = "2.5.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"    % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"           % mongoVersion,
    "uk.gov.hmrc"             %% "domain-play-30"               % "10.0.0",
    "org.apache.pekko"        %% "pekko-connectors-csv"         % pekkoVersion,
    "org.typelevel"           %% "cats-core"                    % "2.13.0",
    "io.github.samueleresca"  %% "pekko-quartz-scheduler"       % "1.2.2-pekko-1.0.x"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-test-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-test-play-30" % mongoVersion,
    "org.scalatest"                %% "scalatest"               % "3.2.19",
    "com.vladsch.flexmark"          % "flexmark-all"            % "0.64.8",
    "org.apache.pekko"             %% "pekko-testkit"           % "1.0.3",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"    % "2.18.0",
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
