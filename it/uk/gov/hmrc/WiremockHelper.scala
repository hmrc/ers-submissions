package uk.gov.hmrc

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Suite}

trait FakeAuthService extends BeforeAndAfterAll with ScalaFutures {
  this: Suite =>

  lazy val authServiceHost = "localhost"
  lazy val authServicePort = 18500

  lazy val authServer = new WireMockServer(wireMockConfig().port(authServicePort))

  final lazy val authServiceBaseUrl = s"http://$authServiceHost:$authServicePort"

  override def beforeAll() = {
    super.beforeAll()
    authServer.start()
  }

  override def afterAll() = {
    super.afterAll()
    authServer.stop()
  }

  authServer.stubFor(WireMock.post(urlMatching("/auth/authorise")).willReturn(WireMock.aResponse().withStatus(200).withBody("""{}""")))
}

trait FakeErsStubService extends BeforeAndAfterAll with ScalaFutures {
  this: Suite =>

  lazy val stubServiceHost = "localhost"
  lazy val stubServicePort = 19339

  lazy val stubServer = new WireMockServer(wireMockConfig().port(stubServicePort))

  final lazy val stubServiceBaseUrl = s"http://$stubServiceHost:$stubServicePort"

  override def beforeAll() = {
    super.beforeAll()
    stubServer.start()
  }

  override def afterAll() = {
    super.afterAll()
    stubServer.stop()
  }

  stubServer.stubFor(WireMock.post(urlMatching("/.*")).willReturn(WireMock.aResponse().withStatus(202)))
}