package models.infra.db

import java.net.InetSocketAddress

import models.domain.error.{ApplicationError, NotFound, ServerError}
import org.elasticsearch.action.DocWriteResponse
import org.elasticsearch.action.support.WriteRequest
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.TransportAddress
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient
import play.api.Configuration

import scala.util.control.NonFatal

trait Es {

  protected val config: Configuration

  private val uri = config.get[String]("elasticsearch.uri")
  private val user = config.get[String]("elasticsearch.user")
  private val pass = config.get[String]("elasticsearch.password")
  private val clusterName = config.get[String]("elasticsearch.cluster-name")

  require(uri != null)
  require(user != null)
  require(pass != null)
  require(clusterName != null)

  private val settings = Settings.builder
    .put("cluster.name", clusterName)
    .put("xpack.security.user", s"$user:$pass")
    .build()

  protected val client: TransportClient = new PreBuiltXPackTransportClient(settings)
    .addTransportAddress(
      new TransportAddress(new InetSocketAddress(uri, 9300))
    )

  protected val index = "todo-api"
  protected val `type` = "todos"

  private[db] def searchEs(query: Map[String, String], limit: Int): Either[ApplicationError, List[(Long, Document)]] = {

    tryWith {

      val matchQueries = query.foldLeft(QueryBuilders.boolQuery()) { (acc, x) =>
        x match {
          case (key, value) => acc.must(QueryBuilders.matchQuery(key, value))
        }
        acc
      }

      val q = QueryBuilders.boolQuery().should(matchQueries)
      val response = client
        .prepareSearch(index)
        .setTypes(`type`)
        .setQuery(q)
        .setFrom(0)
        .setSize(limit)
        .get()

      response.getHits.getHits.foldLeft[Either[ApplicationError, List[(Long, Document)]]](Right(Nil)) {(acc, hit) =>
        acc match {
          case Right(docs) => Document.fromMap(hit.getSourceAsMap) match {
            case Right(doc) => Right(docs :+ (hit.getId.toLong -> doc))
            case Left(e)    => Left(e)
          }
          case Left(e)     => Left(e)
        }
      }
    }
  }

  private[db] def storeDocument(id: Long, doc: Document): Either[ApplicationError, Unit] = {

    tryWith {

      val response = client
        .prepareUpdate(index, `type`, id.toString)
        .setDoc(doc.toMap)
        .setDocAsUpsert(true)
        .setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL)
        .get()

      val result = response.getResult
      if (result == DocWriteResponse.Result.CREATED || result == DocWriteResponse.Result.UPDATED) {
        Right(Unit)
      } else {
        Left(ServerError(s"elastic search returns $result"))
      }
    }
  }

  private[db] def findDocumentByIdOpt(id: Long): Either[ApplicationError, Option[Document]] = {

    tryWith {
      val response = client
        .prepareGet(index, `type`, id.toString)
        .get()

      if (response.isExists) {

        for {
          doc <- Document.fromMap(response.getSourceAsMap)
        } yield Some(doc)

      } else {
        Right(None)
      }
    }
  }

  private[db] def findDocumentById(id: Long): Either[ApplicationError, Document] = {
    this.findDocumentByIdOpt(id) match {
      case Right(None) =>
        Left(NotFound(s"todo content of $id not found"))
      case Right(Some(v)) =>
        Right(v)
      case Left(e) =>
        Left(e)
    }
  }

  private[db] def updateDocument(id: Long, doc: Document): Either[ApplicationError, Document] = {

    for {
      _       <- this.findDocumentById(id)
      _       <- this.storeDocument(id, doc)
      updated <- this.findDocumentById(id)
    } yield updated
  }

  private[db] def deleteDocument(id: Long): Either[ApplicationError, Unit] = {

    tryWith {

      val response = client.prepareDelete(index, `type`, id.toString).get()

      if (response.getResult == DocWriteResponse.Result.DELETED) {
        Right(())
      } else if (response.getResult == DocWriteResponse.Result.NOT_FOUND) {
        Left(NotFound(s"todo content of $id not found"))
      } else {
        Left(ServerError(s"elastic search returns unexpected result - ${response.getResult.toString}"))
      }
    }
  }

  private def tryWith[A](block: => Either[ApplicationError, A]): Either[ApplicationError, A] = {
    try {

      block

    } catch {
      case NonFatal(e) =>
        Left(ServerError(s"elasticsearch returns error - ${e.getMessage}"))
    }
  }
}


case class Document(title: String, content: String) {

  def toMap: java.util.Map[String, String] = {

    val source = new java.util.HashMap[String, String]()
    source.put("title", this.title)
    source.put("content", this.content)

    source
  }
}

object Document {

  def fromMap(map: java.util.Map[String, AnyRef]): Either[ApplicationError, Document] = {

    val nullableTitle = map.get("title")
    val nullableContent = map.get("content")

    if (nullableTitle != null && nullableTitle.isInstanceOf[String]){
      if(nullableContent != null && nullableContent.isInstanceOf[String]){
        Right(Document(nullableTitle.asInstanceOf[String], nullableContent.asInstanceOf[String]))
      } else {
        Left(ServerError("`content` field can not get from document source from es"))
      }
    } else {
      Left(ServerError("`title` field can not get from document source from es"))
    }
  }
}
