package models.domain

package object error {

  sealed trait ApplicationError

  case class ParamError(paramName: String, message: String)
  case class BadRequest(errors: List[ParamError]) extends ApplicationError

  case class NotFound(message: String) extends ApplicationError

  case class ServerError(message: String) extends ApplicationError
}
