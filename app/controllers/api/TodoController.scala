package controllers.api

import controllers.api.TodoController._
import javax.inject.Inject
import models.domain.error.{ApplicationError, ParamError, ServerError, BadRequest => BadRequestError, NotFound => NotFoundError}
import models.domain.todo.model.{CreateRequest, SearchQuery}
import models.infra.json.ReadsWrites._
import models.service.TodoService
import play.api.libs.json._
import play.api.mvc.Results._
import play.api.mvc.{Result, _}

class TodoController @Inject()(
  cc: ControllerComponents,
  todoService: TodoService
) extends AbstractController(cc) {

  def create: Action[JsValue] = Action(parse.json) { request =>

    request.body
      .validate[CreateRequest]
      .performWithJsonPayload { req =>
        this.todoService.registerNewTodo(req)
          .toResult(todo => Created(Json.toJson(todo)))
      }
  }

  def modify(id: Long): Action[JsValue] = Action(parse.json) { request =>

    request.body
      .validate[CreateRequest]
      .performWithJsonPayload { req =>
        this.todoService.modifyTodo(id, req)
          .toResult(todo => Created(Json.toJson(todo)))
      }
  }

  def get(id: Long): Action[AnyContent] = Action {

    this.todoService.getTodoById(id)
      .toResult(todo => Ok(Json.toJson(todo)))
  }

  def search(title: Option[String], content: Option[String], limit: Int) = Action {

    val query = SearchQuery()
    val withTitle = title.fold(query)(query.withTitle)
    val withContent = content.fold(withTitle)(query.withContent)

    this.todoService.searchTodo(withContent, limit)
      .toResult(todos => Ok(Json.obj("todos" -> Json.toJson(todos))))
  }

  def remove(id: Long) = Action {

    this.todoService.removeTodoById(id)
      .toResult(_ => NoContent)
  }
}

object TodoController {

  implicit class ToResult[A](val either: Either[ApplicationError, A]) extends AnyVal {

    def toResult(f: A => Result): Result = {
      either match {
        case Right(v) => f(v)
        case Left(e) => e match {
          case e: BadRequestError => BadRequest(Json.toJson(e))
          case e: NotFoundError   => NotFound(Json.toJson(e))
          case e: ServerError     => InternalServerError(Json.toJson(e))
        }
      }
    }
  }

  implicit class ToParamErrors(val errors: Seq[(JsPath, Seq[JsonValidationError])]) extends AnyVal {

    def toParamErrors: List[ParamError] = {

      val xs = for {
        (path, validationErrors) <- errors
        validationError          <- validationErrors
        message                  <- validationError.messages
      } yield ParamError(path.toJsonString, message)

      xs.toList
    }
  }

  implicit class PerformWithJsonPayload[A](val jsResult: JsResult[A]) extends AnyVal {

    def performWithJsonPayload(f: A => Result): Result = {

      jsResult match {
        case JsSuccess(result, _) =>
          f(result)

        case JsError(errors) =>
          val err = BadRequestError(errors.toParamErrors)
          BadRequest(Json.toJson(err))
      }
    }
  }
}