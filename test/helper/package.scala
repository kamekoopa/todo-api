import models.infra.db.{Document, Es}
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.xcontent.XContentType
import org.scalatest._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import scalikejdbc.config.DBs
import scalikejdbc.scalatest.AutoRollback

package object helper {

  trait Matchers extends MustMatchers with EitherValues with OptionValues

  trait GeneralSpec extends WordSpec with Matchers

  trait FixtureSpec extends fixture.WordSpec with Matchers

  trait EsAutoIndexReCreator { self: fixture.TestSuite with GuiceOneAppPerSuite =>

    type FixtureParam = TransportClient

    protected val es: TestEs = new TestEs(app.configuration)
  }

  trait DataStoreReset extends AutoRollback { self: fixture.TestSuite with GuiceOneAppPerSuite =>

    DBs.setupAll()

    protected val es: TestEs = new TestEs(app.configuration)

    def storeDocument(id: Long, title: String, content: String): Unit = {
      es.getClient
        .prepareUpdate("todo-api", "todos", id.toString)
        .setRefreshPolicy(RefreshPolicy.WAIT_UNTIL)
        .setDocAsUpsert(true)
        .setDoc(Document(title, content).toMap)
        .get()
    }
  }

  class TestEs(protected val config: Configuration) extends Es {

    def getClient: TransportClient = this.client

    def recreateIndex(): Unit = {

      val adminClient = this.client.admin().indices()

      try {
        adminClient.prepareDelete("todo-api").get()
        adminClient.prepareCreate("todo-api").get()

        val mapping =
          """
            |{
            |  "properties": {
            |    "title": {
            |      "type": "text",
            |      "analyzer": "kuromoji",
            |      "store": true
            |    },
            |    "content": {
            |      "type": "text",
            |      "analyzer": "kuromoji",
            |      "store": true
            |    }
            |  }
            |}
          """.stripMargin
        adminClient
          .preparePutMapping("todo-api")
          .setType("todos")
          .setSource(mapping, XContentType.JSON)
          .get()
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }
  }
}
