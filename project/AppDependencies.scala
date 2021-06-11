import sbt._

object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._

  private val silencerVersion = "1.7.1"
  private val akkaVersion = "2.6.15"
  private val alpakkaVersion = "3.0.1"
  private val mongoLockVersion = "7.0.0-play-27"
  private val nettyTransportVersion = "4.1.65.Final"

  val compile = Seq(
    ws,
    "uk.gov.hmrc"        %% "bootstrap-backend-play-27"           % "5.4.0",
    "uk.gov.hmrc"        %% "domain"                              % "5.11.0-play-27",
    "uk.gov.hmrc"        %% "mongo-lock"                          % mongoLockVersion,
    "uk.gov.hmrc"        %% "simple-reactivemongo"                % "7.31.0-play-27",
    "org.reactivemongo"  %% "reactivemongo-akkastream"            % "1.0.4",
    "com.typesafe.play"  %% "play-json-joda"                      % "2.9.2",
    "io.netty"           %  "netty-transport-native-epoll"        % nettyTransportVersion,
    "com.lightbend.akka" %% "akka-stream-alpakka-csv"             % alpakkaVersion,
    "com.lightbend.akka" %% "akka-stream-alpakka-json-streaming"  % alpakkaVersion,
    "com.typesafe.akka"  %% "akka-stream"                         % akkaVersion,
    "com.typesafe.akka"  %% "akka-slf4j"                          % akkaVersion,
    "com.typesafe.akka"  %% "akka-protobuf"                       % akkaVersion,
    "com.typesafe.akka"  %% "akka-http-spray-json"                % "10.1.12",
    compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = Seq()
  }

  private val reactiveMongoTestVersion = "4.22.0-play-27"
  private val pegdownVersion = "1.6.0"
  private val jsoupVersion = "1.13.1"
  private val scalaTestPlusPlayVersion = "4.0.3"
  private val scalaTestVersion = "3.0.9"
  private val mockitoCoreVersion = "3.11.1"

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc"            %% "reactivemongo-test" % reactiveMongoTestVersion % scope,
        "uk.gov.hmrc"            %% "mongo-lock"         % mongoLockVersion         % scope,
        "org.scalatest"          %% "scalatest"          % scalaTestVersion         % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusPlayVersion % scope,
        "org.mockito"            %  "mockito-core"       % mockitoCoreVersion       % scope,
        "org.pegdown"            %  "pegdown"            % pegdownVersion           % scope,
        "org.jsoup"              %  "jsoup"              % jsoupVersion             % scope,
        "com.typesafe.play"      %% "play-test"          % PlayVersion.current      % scope,
        "com.typesafe.akka"      %% "akka-testkit"       % akkaVersion              % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply(): Seq[ModuleID] = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test = Seq(
        "uk.gov.hmrc"            %% "reactivemongo-test"           % reactiveMongoTestVersion % scope,
        "uk.gov.hmrc"            %% "mongo-lock"                   % mongoLockVersion         % scope,
        "com.typesafe.play"      %% "play-test"                    % PlayVersion.current      % scope,
        "org.scalatest"          %% "scalatest"                    % scalaTestVersion         % scope,
        "org.scalatestplus.play" %% "scalatestplus-play"           % scalaTestPlusPlayVersion % scope,
        "org.mockito"            %  "mockito-core"                 % mockitoCoreVersion       % scope,
        "org.pegdown"            %  "pegdown"                      % pegdownVersion           % scope,
        "org.jsoup"              %  "jsoup"                        % jsoupVersion             % scope,
        "io.netty"               %  "netty-transport-native-epoll" % nettyTransportVersion    % scope,
        "com.github.tomakehurst" %  "wiremock-jre8"                % "2.28.1"                 % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}