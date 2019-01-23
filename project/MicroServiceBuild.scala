import sbt._

object MicroServiceBuild extends Build with MicroService {

  val appName = "ers-submissions"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._

  private val microserviceBootstrapVersion = "10.0.0"
  private val domainVersion = "5.3.0"
  private val playReactivemongoVersion = "6.2.0"
  private val scalatestPlusPlayVersion = "3.0.1"
  private val hmrcTestVersion = "3.3.0"
  private val reactivemongoTestVersion = "3.1.0"
  private val scalatestVersion = "3.0.1"
  private val mongoLock = "5.1.0"
  private val wiremockVersion = "2.4.1"
  private val reactiveMongoVersion = "6.2.0"
  private val mockitoVersion = "2.6.1"


  val compile = Seq(
    "uk.gov.hmrc" %% "play-reactivemongo" % playReactivemongoVersion,
    ws,
    "uk.gov.hmrc" %% "microservice-bootstrap" % microserviceBootstrapVersion,
    "uk.gov.hmrc" %% "domain" % domainVersion,
    "uk.gov.hmrc" %% "reactivemongo-test" % reactivemongoTestVersion,
    "uk.gov.hmrc" %% "mongo-lock" % mongoLock,
    "uk.gov.hmrc" %% "play-reactivemongo" % reactiveMongoVersion,
    "xerces" % "xercesImpl" % "2.12.0"

  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  private val pegdownVersion = "1.6.0"
  private val jsoupVersion = "1.8.3"

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "reactivemongo-test" % reactivemongoTestVersion % scope,
        "org.scalatest" %% "scalatest" % scalatestVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % scope,
        "org.mockito" % "mockito-core" % mockitoVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "org.jsoup" % "jsoup" % jsoupVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "uk.gov.hmrc" %% "mongo-lock" % mongoLock
      )
    }.test
  }

  object IntegrationTest {
    def apply() = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test = Seq(
        "uk.gov.hmrc" %% "reactivemongo-test" % reactivemongoTestVersion % scope,
        "org.scalatest" %% "scalatest" % scalatestVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "org.jsoup" % "jsoup" % jsoupVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "uk.gov.hmrc" %% "mongo-lock" % mongoLock,
        "com.github.tomakehurst" % "wiremock" % wiremockVersion % "it"
      )
    }.test
  }

  def apply() = compile ++ Test() ++ IntegrationTest()
}

