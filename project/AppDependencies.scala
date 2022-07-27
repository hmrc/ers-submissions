import sbt._

object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._

  private val silencerVersion = "1.7.1"
  private val akkaVersion = "2.6.19"
  private val alpakkaVersion = "3.0.3"
  private val nettyTransportVersion = "4.1.69.Final"
  private val mongoVersion = "0.68.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc"        %% "bootstrap-backend-play-28"           % "5.24.0",
    "uk.gov.hmrc"        %% "domain"                              % "8.1.0-play-28",
    "uk.gov.hmrc.mongo"  %% "hmrc-mongo-play-28"                  % mongoVersion,
    "com.typesafe.play"  %% "play-json-joda"                      % "2.9.2",
    "io.netty"           %  "netty-transport-native-epoll"        % nettyTransportVersion,
    "com.lightbend.akka" %% "akka-stream-alpakka-csv"             % alpakkaVersion,
    "com.lightbend.akka" %% "akka-stream-alpakka-json-streaming"  % alpakkaVersion,
    "com.typesafe.akka"  %% "akka-stream"                         % akkaVersion,
    "com.typesafe.akka"  %% "akka-slf4j"                          % akkaVersion,
    "com.typesafe.akka"  %% "akka-protobuf"                       % akkaVersion,
    "com.typesafe.akka"  %% "akka-actor-typed"                    % akkaVersion,
    "com.typesafe.akka"  %% "akka-serialization-jackson"          % akkaVersion,
    "com.typesafe.akka"  %% "akka-http-spray-json"                % "10.2.6",
    compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = Seq()
  }

  private val pegdownVersion = "1.6.0"
  private val jsoupVersion = "1.14.3"
  private val scalaTestPlusPlayVersion = "5.1.0"
  private val scalaTestVersion = "3.2.10"
  private val mockitoCoreVersion = "4.0.0"
  private val flexmarkAllVersion = "0.62.2"

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % mongoVersion         % scope,
        "org.scalatest"          %% "scalatest"               % scalaTestVersion         % scope,
        "org.scalatestplus.play" %% "scalatestplus-play"      % scalaTestPlusPlayVersion % scope,
        "com.vladsch.flexmark"   %  "flexmark-all"            % flexmarkAllVersion       % scope,
        "org.scalatestplus"      %% "mockito-3-4"             % "3.2.10.0"               % scope,
        "org.pegdown"            %  "pegdown"                 % pegdownVersion           % scope,
        "org.jsoup"              %  "jsoup"                   % jsoupVersion             % scope,
        "com.typesafe.play"      %% "play-test"               % PlayVersion.current      % scope,
        "com.typesafe.akka"      %% "akka-testkit"            % akkaVersion              % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply(): Seq[ModuleID] = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test = Seq(
        "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28"      % mongoVersion         % scope,
        "com.typesafe.play"      %% "play-test"                    % PlayVersion.current      % scope,
        "org.scalatest"          %% "scalatest"                    % scalaTestVersion         % scope,
        "org.scalatestplus.play" %% "scalatestplus-play"           % scalaTestPlusPlayVersion % scope,
        "com.vladsch.flexmark"   %  "flexmark-all"                 % flexmarkAllVersion       % scope,
        "org.mockito"            %  "mockito-core"                 % mockitoCoreVersion       % scope,
        "org.pegdown"            %  "pegdown"                      % pegdownVersion           % scope,
        "org.jsoup"              %  "jsoup"                        % jsoupVersion             % scope,
        "io.netty"               %  "netty-transport-native-epoll" % nettyTransportVersion    % scope,
        "com.github.tomakehurst" %  "wiremock-jre8"                % "2.31.0"                 % scope,
        "com.fasterxml.jackson.module" %% "jackson-module-scala"   % "2.13.0"                 % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}