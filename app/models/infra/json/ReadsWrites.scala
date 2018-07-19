package models.infra.json

import models.domain.error.{BadRequest, NotFound, ParamError, ServerError}
import models.domain.todo.model.{CreateRequest, Todo}
import play.api.libs.json.{Json, Reads, Writes}
import play.api.libs.functional.syntax._

object ReadsWrites {

  implicit lazy val createRequestReads: Reads[CreateRequest] = Reads { json =>
    (
      (json \ "title").validate[String] and
      (json \ "content").validate[String]
    )(CreateRequest)
  }

  implicit lazy val paramErrorWrites: Writes[ParamError] = Writes { value =>
    Json.obj(
      "param" -> value.paramName,
      "message" -> value.message
    )
  }

  implicit lazy val badRequestWrites: Writes[BadRequest] = Writes { value =>
    Json.obj(
      "erros" -> Json.toJson(value.errors)
    )
  }

  implicit lazy val notFoundWrites: Writes[NotFound] = Writes { value =>
    Json.obj(
      "message" -> value.message
    )
  }

  implicit lazy val serverErrorWrites: Writes[ServerError] = Writes { value =>
    Json.obj(
      "message" -> value.message
    )
  }

  implicit lazy val todoWrites: Writes[Todo] = Writes { value =>
    Json.obj(
      "id" -> value.id,
      "title" -> value.title,
      "content" -> value.content
    )
  }
}
