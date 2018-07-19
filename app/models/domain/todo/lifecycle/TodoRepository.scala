package models.domain.todo.lifecycle

import models.domain.error.ApplicationError
import models.domain.todo.model.{SearchQuery, Todo}
import scalikejdbc._

trait TodoRepository {

  def create(title: String, content: String)(implicit s: DBSession): Either[ApplicationError, Todo]

  def findByIdOpt(id: Long)(implicit s: DBSession): Either[ApplicationError, Option[Todo]]

  def findById(id: Long, e: => ApplicationError)(implicit s: DBSession): Either[ApplicationError, Todo]

  def searchTodos(query: SearchQuery, limit: Int)(implicit s: DBSession): Either[ApplicationError, List[Todo]]

  def modify(todo: Todo)(implicit s: DBSession): Either[ApplicationError, Todo]

  def remove(id: Long)(implicit s: DBSession): Either[ApplicationError, Unit]
}
