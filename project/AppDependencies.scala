import sbt._

object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._

  private val hmrcTestVersion = "3.8.0-play-26"
  private val domainVersion = "5.9.0-play-26"
  private val reactiveMongoTestVersion = "4.21.0-play-26"
  private val scalatestVersion = "3.0.9"
  private val pegdownVersion = "1.6.0"
  private val jsoupVersion = "1.13.1"
  private val silencerVersion = "1.7.1"

  val compile = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-backend-play-26" % "3.0.0",
    "uk.gov.hmrc"       %% "domain"                    % domainVersion,
    "uk.gov.hmrc"       %% "auth-client"               % "3.2.0-play-26",
    "uk.gov.hmrc"       %% "mongo-lock"                % "6.23.0-play-26",
    "uk.gov.hmrc"       %% "simple-reactivemongo"      % "7.30.0-play-26",
    "org.reactivemongo" %% "reactivemongo-akkastream" % "0.20.13",
    "com.typesafe.play" %% "play-json-joda"            % "2.7.4",
    "xerces"             % "xercesImpl"                % "2.12.0",
    "io.netty"           % "netty-transport-native-epoll"    % "4.0.17.Final",
    "com.lightbend.akka" %% "akka-stream-alpakka-csv" % "2.0.2",
    "com.lightbend.akka" %% "akka-stream-alpakka-json-streaming" % "2.0.2",
    "com.typesafe.akka" %% "akka-stream" % "2.6.12",
    "com.typesafe.akka" %% "akka-slf4j" % "2.6.12",
    "com.typesafe.akka" %% "akka-protobuf" % "2.6.12",
    "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.12",
    compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = Seq()
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc"            %% "reactivemongo-test" % reactiveMongoTestVersion % scope,
        "org.scalatest"          %% "scalatest"          % scalatestVersion         % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3"                  % scope,
        "org.mockito"             % "mockito-core"       % "3.3.3"                  % scope,
        "org.pegdown"             % "pegdown"            % pegdownVersion           % scope,
        "org.jsoup"               % "jsoup"              % jsoupVersion             % scope,
        "com.typesafe.play"      %% "play-test"          % PlayVersion.current      % scope,
        "uk.gov.hmrc"            %% "hmrctest"           % hmrcTestVersion          % scope,
        "uk.gov.hmrc"            %% "mongo-lock"         % "6.23.0-play-26"
      )
    }.test
  }

  object IntegrationTest {
    def apply(): Seq[ModuleID] = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test = Seq(
        "uk.gov.hmrc"            %% "reactivemongo-test" % reactiveMongoTestVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3"                  % scope,
        "org.pegdown"             % "pegdown"            % pegdownVersion           % scope,
        "org.jsoup"               % "jsoup"              % jsoupVersion             % scope,
        "org.mockito"             % "mockito-core"       % "2.6.9"                  % scope,
        "com.typesafe.play"      %% "play-test"          % PlayVersion.current      % scope,
        "io.netty"                % "netty-transport-native-epoll"    % "4.0.17.Final" % scope,
        "uk.gov.hmrc"            %% "hmrctest"           % hmrcTestVersion          % scope,
        "com.github.tomakehurst"  % "wiremock-jre8"      % "2.27.2"                 % scope,
        "uk.gov.hmrc"            %% "mongo-lock"         % "6.23.0-play-26"         % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}