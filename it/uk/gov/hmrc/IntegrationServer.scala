package uk.gov.hmrc

import _root_.play.modules.reactivemongo.MongoDbConnection
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.ws.WSRequest
import uk.gov.hmrc.play.it.{ServiceSpec, ExternalService, MicroServiceEmbeddedServer}

class IntegrationServer(override val testName: String, extraConfig: Map[String, String]) extends MicroServiceEmbeddedServer {

  import ExternalService.runFromJar

  override protected val externalServices: Seq[ExternalService] = Seq().map(runFromJar(_))

  override val additionalConfig = extraConfig
}

abstract class ISpec(testName:String, additionalConfig: Seq[(String, String)] = Seq.empty) extends ServiceSpec with WSRequest {

  override val server = new IntegrationServer(testName, additionalConfig.toMap)

  protected lazy val mongoConnection = new MongoDbConnection {}
  protected implicit lazy val db = mongoConnection.db

  def request(url: String) = buildRequest(resource(url))
  implicit val headerCarrier = HeaderCarrier()

}
