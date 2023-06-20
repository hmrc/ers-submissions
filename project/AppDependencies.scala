import sbt.*

object AppDependencies {
  import play.sbt.PlayImport.ws

  private val akkaVersion = "2.6.20"
  private val bootstrapVersion = "7.15.0"

  private val alpakkaVersion = "3.0.4"
  private val nettyTransportVersion = "4.1.93.Final"
  private val mongoVersion = "0.74.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"        %% "bootstrap-backend-play-28"          % bootstrapVersion,
    "uk.gov.hmrc"        %% "domain"                             % "8.3.0-play-28",
    "uk.gov.hmrc.mongo"  %% "hmrc-mongo-play-28"                 % mongoVersion,
    "com.typesafe.play"  %% "play-json-joda"                     % "2.9.4",
    "io.netty"           %  "netty-transport-native-epoll"       % nettyTransportVersion,
    "com.lightbend.akka" %% "akka-stream-alpakka-csv"            % alpakkaVersion,
    "com.lightbend.akka" %% "akka-stream-alpakka-json-streaming" % alpakkaVersion,
    "com.typesafe.akka"  %% "akka-stream"                        % akkaVersion,
    "com.typesafe.akka"  %% "akka-slf4j"                         % akkaVersion,
    "com.typesafe.akka"  %% "akka-protobuf"                      % akkaVersion,
    "com.typesafe.akka"  %% "akka-actor-typed"                   % akkaVersion,
    "com.typesafe.akka"  %% "akka-serialization-jackson"         % akkaVersion,
    "com.typesafe.akka"  %% "akka-http-spray-json"               % "10.2.10",
    "com.enragedginger"  %% "akka-quartz-scheduler"              % "1.9.3-akka-2.6.x",
    "org.typelevel"      %% "cats-core"                          % "2.9.0"
  )

  private val pegdownVersion = "1.6.0"
  private val jsoupVersion = "1.16.1"
  private val scalaTestVersion = "3.2.16"
  private val mockitoCoreVersion = "4.6.1"
  private val flexmarkAllVersion = "0.64.8"

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % mongoVersion,
    "org.scalatest"          %% "scalatest"               % scalaTestVersion,
    "com.vladsch.flexmark"   %  "flexmark-all"            % flexmarkAllVersion,
    "org.scalatestplus"      %% "mockito-4-11"            % "3.2.16.0",
    "org.pegdown"            %  "pegdown"                 % pegdownVersion,
    "org.jsoup"              %  "jsoup"                   % jsoupVersion,
    "com.typesafe.akka"      %% "akka-testkit"            % akkaVersion
  ).map(_ % Test)

  val integrationTest: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                   %% "bootstrap-test-play-28"       % bootstrapVersion,
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-test-play-28"      % mongoVersion,
    "org.scalatest"                 %% "scalatest"                    % scalaTestVersion,
    "com.vladsch.flexmark"          %  "flexmark-all"                 % flexmarkAllVersion,
    "org.pegdown"                   %  "pegdown"                      % pegdownVersion,
    "org.jsoup"                     %  "jsoup"                        % jsoupVersion,
    "io.netty"                      %  "netty-transport-native-epoll" % nettyTransportVersion,
    "com.github.tomakehurst"        %  "wiremock-jre8"                % "2.35.0",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"         % "2.15.2"
  ).map(_ % "it")

  def all: Seq[ModuleID] = compile ++ test ++ integrationTest
}
