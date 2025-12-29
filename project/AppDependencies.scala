import sbt.*

object AppDependencies {

  private val bootstrapVersion = "10.5.0"
  private val mongoVersion = "2.11.0"

  private val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"    % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"           % mongoVersion,
    "uk.gov.hmrc"             %% "domain-play-30"               % "11.0.0",
    "org.apache.pekko"        %% "pekko-connectors-csv"         % "1.0.2",
    "org.typelevel"           %% "cats-core"                    % "2.13.0",
    "io.github.samueleresca"  %% "pekko-quartz-scheduler"       % "1.2.2-pekko-1.0.x"
  )

  private val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-test-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-test-play-30" % mongoVersion,
    "org.apache.pekko"             %% "pekko-testkit"           % "1.0.3",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"    % "2.20.1",
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test

}
