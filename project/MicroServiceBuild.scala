import sbt._

object MicroServiceBuild extends Build with MicroService {
  import scala.util.Properties.envOrElse

  val appName = "ers-submissions"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val microserviceBootstrapVersion = "5.8.0"
  private val playAuthVersion = "4.2.0"
  private val playHealthVersion = "2.0.0"
  private val playJsonLoggerVersion = "3.1.0"
  private val playUrlBindersVersion = "2.0.0"
  private val playConfigVersion = "3.0.0"
  private val domainVersion = "4.0.0"
  private val playReactivemongoVersion = "5.1.0"
  private val scalatestPlusPlayVersion = "1.2.0"
  private val hmrcTestVersion = "2.2.0"
  private val reactivemongoTestVersion = "2.0.0"
  private val scalatestVersion = "3.0.1"
  private val mongoLock = "4.0.0"
  private val wiremockVersion = "2.4.1"
  private val reactiveMongoVersion = "5.1.0"

  val compile = Seq(
    "uk.gov.hmrc" %% "play-reactivemongo" % playReactivemongoVersion,
    ws,
    "uk.gov.hmrc" %% "microservice-bootstrap" % microserviceBootstrapVersion,
    "uk.gov.hmrc" %% "play-authorisation" % playAuthVersion,
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,
    "uk.gov.hmrc" %% "play-url-binders" % playUrlBindersVersion,
    "uk.gov.hmrc" %% "play-config" % playConfigVersion,
    "uk.gov.hmrc" %% "logback-json-logger" % playJsonLoggerVersion,
    "uk.gov.hmrc" %% "domain" % domainVersion,
    "uk.gov.hmrc" %% "reactivemongo-test" % reactivemongoTestVersion,
    "uk.gov.hmrc" %% "mongo-lock" % mongoLock,
    "uk.gov.hmrc" %% "play-reactivemongo" % reactiveMongoVersion

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

