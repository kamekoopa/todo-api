package models.service

import javax.inject.{Inject, Singleton}
import models.domain.error.{ApplicationError, NotFound}
import models.domain.todo.lifecycle.TodoRepository
import models.domain.todo.model.{CreateRequest, SearchQuery, Todo}
import scalikejdbc._

@Singleton
class TodoService @Inject()(private val todoRepository: TodoRepository) {

  def registerNewTodo(request: CreateRequest): Either[ApplicationError, Todo] = {

    DB.localTx { implicit s =>
      this.todoRepository.create(request.title, request.content)
    }
  }

  def modifyTodo(id: Long, request: CreateRequest): Either[ApplicationError, Todo] = {

    DB.localTx { implicit s =>
      for {
        modified <- this.todoRepository.modify(Todo(id, request.title, request.content))
      } yield modified
    }
  }

  def getTodoByIdAsOption(id: Long): Either[ApplicationError, Option[Todo]] = {

    DB.readOnly { implicit s =>
      this.todoRepository.findByIdOpt(id)
    }
  }

  def getTodoById(id: Long): Either[ApplicationError, Todo]= {

    DB.readOnly { implicit s =>
      this.todoRepository.findById(id, NotFound(s"todo of id `$id` not found"))
    }
  }

  def searchTodo(query: SearchQuery, limit: Int): Either[ApplicationError, List[Todo]] = {

    DB.readOnly { implicit s =>
      this.todoRepository.searchTodos(query, limit)
    }
  }

  def removeTodoById(id: Long): Either[ApplicationError, Unit] = {

    DB.localTx { implicit s =>
      this.todoRepository.remove(id)
    }
  }
}
