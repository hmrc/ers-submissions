package uk.gov.hmrc.erssubmission.it

import org.scalatest.BeforeAndAfterEach
import repositories.PresubmissionMongoRepository
import uk.gov.hmrc.{FakeAuthService, Fixtures, ISpec}

import scala.concurrent.ExecutionContext.Implicits.global
import _root_.play.api.test.Helpers._
import config.ApplicationConfig
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.HeaderCarrier

class ValidatorIntegrationSpec extends ISpec("ReceiverFromValidatorTest", additionalConfig = Seq(
  ("microservice.services.auth.host", "localhost"),
  ("microservice.services.auth.port", "18500")
)) with BeforeAndAfterEach with FakeAuthService with GuiceOneAppPerSuite {

  override def applicableHeaders(url: String)(implicit hc: HeaderCarrier): Seq[(String, String)] = Nil

  val appConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  def wsClient: WSClient = app.injector.instanceOf[WSClient]

  private lazy val presubmissionRepository = app.injector.instanceOf[PresubmissionMongoRepository]

  override protected def afterEach: Unit = {
    super.afterEach
    await(presubmissionRepository.drop)
  }

  // /submit-presubmission
  "Sending data from validator" should {

    "return BAD_REQUEST if invalid object is sent" in {
      val response = await(request("ers/ABC%2F1234/submit-presubmission").post(Fixtures.invalidPayload))
      response.status shouldBe BAD_REQUEST
    }

    "be stored successfully in database" in {
      val response = await(request("ers/ABC%2F1234/submit-presubmission").post(Fixtures.schemeDataPayload))
      response.status shouldBe OK

      val presubmissionData = await(presubmissionRepository.getJson(Fixtures.schemeInfo))
      presubmissionData.length shouldBe 1
      presubmissionData.head.equals(Fixtures.schemeData)
    }

  }

  // /remove-presubmission
  "Removing data" should {

    "successfully remove data by session Id and scheme ref" in {
      val response = await(request("ers/ABC%2F1234/submit-presubmission").post(Fixtures.schemeDataPayload))
      response.status shouldBe OK

      val presubmissionData = await(presubmissionRepository.getJson(Fixtures.schemeInfo))
      presubmissionData.length shouldBe 1
      presubmissionData.head.equals(Fixtures.schemeData)

      val removeResponse = await(request("ers/remove-presubmission")).post(Fixtures.schemeInfoPayload)
      removeResponse.status shouldBe OK

      val presubmissionDataAfterRemove = await(presubmissionRepository.getJson(Fixtures.schemeInfo))
      presubmissionDataAfterRemove.length shouldBe 0
    }

  }

  // /check-for-presubmission/:validatedSheets

  "Checking for received presubmission data" should {

    "return OK if expected records are equal to existing ones" in {
      val response = await(request("ers/ABC%2F1234/submit-presubmission").post(Fixtures.schemeDataPayload))
      response.status shouldBe OK
      val presubmissionData = await(presubmissionRepository.getJson(Fixtures.schemeInfo))
      presubmissionData.length shouldBe 1
      presubmissionData.head.equals(Fixtures.schemeData)

      val checkResponse = await(request("ers/check-for-presubmission/1")).post(Fixtures.schemeInfoPayload)
      checkResponse.status shouldBe OK
    }

    "return InternalServerError if expected records are not equal to existing ones" in {
      val response = await(request("ers/ABC%2F1234/submit-presubmission").post(Fixtures.schemeDataPayload))
      response.status shouldBe OK
      val presubmissionData = await(presubmissionRepository.getJson(Fixtures.schemeInfo))
      presubmissionData.length shouldBe 1
      presubmissionData.head.equals(Fixtures.schemeData)

      val checkResponse = await(request("ers/check-for-presubmission/2")).post(Fixtures.schemeInfoPayload)
      checkResponse.status shouldBe INTERNAL_SERVER_ERROR
    }

  }

}
