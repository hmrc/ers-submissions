
package uk.gov.hmrc

import org.scalatest.BeforeAndAfterEach
import repositories.{MetadataMongoRepository, PresubmissionMongoRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import _root_.play.api.test.Helpers._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.http.HeaderCarrier
import _root_.play.api.libs.ws.WSClient

class ADRSubmissionIntegration extends ISpec("ADRSubmissionIntegration", additionalConfig = Seq(
  ("microservice.services.ers-stub.host", "localhost"),
  ("microservice.services.ers-stub.port", "19339")
)) with BeforeAndAfterEach with FakeErsStubService with GuiceOneAppPerSuite {

  override def applicableHeaders(url: String)(implicit hc: HeaderCarrier): Seq[(String, String)] = Nil

  def wsClient: WSClient = app.injector.instanceOf[WSClient]

  private lazy val presubmissionRepository = app.injector.instanceOf[PresubmissionMongoRepository]
  private lazy val metadataMongoRepository = app.injector.instanceOf[MetadataMongoRepository]

  override protected def beforeEach: Unit = {
    super.beforeEach()
    await(presubmissionRepository.storeJson(Fixtures.schemeData))
  }

  override protected def afterEach: Unit = {
    super.afterEach
    await(presubmissionRepository.drop)
    await(metadataMongoRepository.drop)
  }

   //submit-metadata
  "Receiving data for submission" should {

    "return OK if valid metadata is received, filedata is extracted from database and it's successfully sent to ADR" in {
      val data = Fixtures.buildErsSummaryPayload(false)

      await(metadataMongoRepository.storeErsSummary(Fixtures.buildErsSummary(false)))
      val test = Fixtures.schemeInfo
      val metadataAfterSave = await(metadataMongoRepository.getJson(test))

      metadataAfterSave.length shouldBe 1
      metadataAfterSave.head.transferStatus.get shouldBe "saved"

      val res = await(request("ers/submit-metadata").post(data))
      res.status shouldBe OK

      val metadata = await(metadataMongoRepository.getJson(Fixtures.schemeInfo))
      metadata.length shouldBe 1
      metadata.head.transferStatus.get shouldBe "sent"
    }

    "return OK if valid metadata is received for nil return and it's successfully sent to ADR" in {
      val data = Fixtures.buildErsSummaryPayload(true)

      await(metadataMongoRepository.storeErsSummary(Fixtures.buildErsSummary(true)))
      val metadataAfterSave = await(metadataMongoRepository.getJson(Fixtures.schemeInfo))
      metadataAfterSave.length shouldBe 1
      metadataAfterSave.head.transferStatus.get shouldBe "saved"

      val res = await(request("ers/submit-metadata").post(Fixtures.buildErsSummaryPayload(true)))
      res.status shouldBe OK

      val metadata = await(metadataMongoRepository.getJson(Fixtures.schemeInfo))
      metadata.length shouldBe 1
      metadata.head.transferStatus.get shouldBe "sent"
    }

    "return BAD_REQUEST if invalid request is made" in {
      val response = await(request("ers/submit-metadata").post(Fixtures.invalidPayload))
      response.status shouldBe BAD_REQUEST
    }
  }

  "Calling /save-metadata" should {

    "return OK and save metadata if valid metadata is sent" in {

      val response = await(request("ers/save-metadata").post(Fixtures.buildErsSummaryPayload( true)))
      response.status shouldBe OK
      val result = await(metadataMongoRepository.getJson(Fixtures.schemeInfo))

      result.length shouldBe 1
      result.head.toString shouldBe Fixtures.buildErsSummary(true).toString
    }
  }

  "return BAD_REQUEST if invalid request is made" in {
    val response = await(request("ers/save-metadata").post(Fixtures.invalidPayload))
    response.status shouldBe BAD_REQUEST
  }

}
