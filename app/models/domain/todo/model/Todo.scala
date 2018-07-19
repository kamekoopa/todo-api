package models.domain.todo.model

case class Todo(id: Long, title: String, content: String) {

  def modifyTitle(title: String): Todo = this.copy(title = title)

  def modifyContent(content: String): Todo = this.copy(content = content)
}
