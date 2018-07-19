package models.domain.todo.model

case class SearchQuery(title: Option[String] = None, content: Option[String] = None) {

  def withTitle(title: String): SearchQuery = this.copy(title = Some(title))

  def withContent(content: String): SearchQuery = this.copy(content = Some(content))

  def toMap: Map[String, String] = {

    val withTitle = this.title.fold(Map[String, String]())(t => Map("title" -> t))
    val withContent = this.content.fold(withTitle)(c => withTitle + ("content" -> c))

    withContent
  }
}
