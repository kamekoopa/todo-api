package models.infra.db

import javax.inject.{Inject, Singleton}
import models.domain.error._
import models.domain.todo.lifecycle.TodoRepository
import models.domain.todo.model.{SearchQuery, Todo}
import play.api.Configuration
import scalikejdbc._

import scala.util.control.NonFatal

@Singleton
class TodoRepositoryImpl @Inject()(protected val config: Configuration) extends TodoRepository with Es {

  private lazy val t = Todos.syntax

  def create(title: String, content: String)(implicit s: DBSession): Either[ApplicationError, Todo] = {

    for {

      generatedId <- tryWith {
        applyUpdateAndReturnGeneratedKey(insert.into(Todos).columns().values())
      }

      _ <- this.storeDocument(generatedId, Document(title, content))

      newTodo <- this.findById(generatedId, ServerError(s"new todo insertion failed - ($title, $content)"))

    } yield newTodo
  }

  def findByIdOpt(id: Long)(implicit s: DBSession): Either[ApplicationError, Option[Todo]] = {

    val sql = withSQL {
      select.from(Todos as t).where.eq(t.id, id)
    }

    for {
      todosOpt <- tryWith {
        sql.map(Todos(t.resultName)).single().apply()
      }

      docOpt <- todosOpt match {
        case Some(todos) => this.findDocumentById(todos.id).right.map(Some(_))
        case None        => Right(None)
      }
    } yield todosOpt.flatMap(todos => docOpt.map(doc => Todo(todos.id, doc.title, doc.content)))
  }

  def findById(id: Long, e: => ApplicationError)(implicit s: DBSession): Either[ApplicationError, Todo] = {
    this.findByIdOpt(id).flatMap {
      case Some(todo) => Right(todo)
      case None => Left(e)
    }
  }

  def searchTodos(query: SearchQuery, limit: Int)(implicit s: DBSession): Either[ApplicationError, List[Todo]] = {

    def fetchTodos(docs: List[(Long, Document)]): Either[ApplicationError, List[Todo]] = {

      docs.foldLeft[Either[ApplicationError, List[Todo]]](Right(Nil)) {(acc, doc) =>
        acc match {
          case Right(todos) =>
            this.findById(doc._1, ServerError(s"corresponding todo entity not found(id: ${doc._1}")) match {
              case Right(todo) => Right(todos :+ todo)
              case Left(e) => Left(e)
            }
          case Left(e) => Left(e)
        }
      }
    }

    for {
      docs  <- this.searchEs(query.toMap, limit)
      todos <- fetchTodos(docs)
    } yield todos
  }

  def modify(todo: Todo)(implicit s: DBSession): Either[ApplicationError, Todo] = {

    for {
      _        <- this.findById(todo.id, NotFound(s"todo(id: ${todo.id}) not found"))
      _        <- this.updateDocument(todo.id, Document(todo.title, todo.content))
      modified <- this.findById(todo.id, ServerError(s"modified todo entity not found(id: ${todo.id})"))
    } yield modified
  }

  def remove(id: Long)(implicit s: DBSession): Either[ApplicationError, Unit] = {

    lazy val clm = Todos.column
    for {
      _ <- this.deleteDocument(id)
      _ <- tryWith {
        applyUpdate(delete.from(Todos).where.eq(clm.id, id))
      }
    } yield ()
  }

  private def tryWith[A](block: => A): Either[ApplicationError, A] = {
    try {

      Right(block)

    } catch {
      case NonFatal(e) =>
        Left(ServerError(s"database returns error - ${e.getMessage}"))
    }
  }
}


case class Todos(id: Long)

object Todos extends SQLSyntaxSupport[Todos] {
  def apply(rn: ResultName[Todos])(rs: WrappedResultSet): Todos = {
    Todos(rs.long(rn.id))
  }
}
