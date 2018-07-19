package models.infra.framework

import models.domain.todo.lifecycle.TodoRepository
import models.infra.db.TodoRepositoryImpl
import models.service.TodoService
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}

class AppModule extends Module {

  def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[TodoService].toSelf,
      bind[TodoRepository].to[TodoRepositoryImpl]
    )
  }
}
