package models.infra.db

import helper.{DataStoreReset, FixtureSpec}
import models.domain.error._
import models.domain.todo.model.{SearchQuery, Todo}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import scalikejdbc._

class TodoRepositoryImplSpec extends FixtureSpec with GuiceOneAppPerSuite with DataStoreReset {

  override def fixture(implicit session: FixtureParam): Unit = {
    es.recreateIndex()
    sql"truncate todos".update().apply()

    es.storeDocument(10, Document("title_10", "content_10"))
    sql"insert todos (id) values ('10')".update().apply()

    sql"insert todos (id) values ('20')".update().apply()
    es.storeDocument(20, Document("タスクをこなす", "割り当てられたタスクをこなす"))

    sql"alter table todos auto_increment = 100".update().apply()
  }

  val repo = new TodoRepositoryImpl(app.configuration)

  "TodoRepository" should {

    "todoを作成できる" in {implicit s=>

      val result = repo.create("タイトル", "内容")

      result.right.value.title mustBe "タイトル"
      result.right.value.content mustBe "内容"
    }

    "idでTodoを見つけられる" in { implicit s =>

      val result = repo.findByIdOpt(10)

      result.right.value.value.id mustBe 10
      result.right.value.value.title mustBe "title_10"
      result.right.value.value.content mustBe "content_10"
    }

    "存在しないidで検索するとNone" in {implicit  s =>

      val result = repo.findByIdOpt(99)

      result.right.value mustBe None
    }

    "idでTodoを見つけられる(must get版)" in { implicit s =>

      val result = repo.findById(10, ServerError(""))

      result.right.value.id mustBe 10
      result.right.value.title mustBe "title_10"
      result.right.value.content mustBe "content_10"
    }

    "存在しないidで検索するとエラー(must get版)" in {implicit  s =>

      val result = repo.findById(99, ServerError(""))

      result.left.value mustBe a[ServerError]
    }

    "todoをタイトルで検索できる" in {implicit s =>

      val results = repo.searchTodos(SearchQuery().withTitle("タスク"), 10)

      results.right.value.head.id mustBe 20
      results.right.value.head.title mustBe "タスクをこなす"
      results.right.value.head.content mustBe "割り当てられたタスクをこなす"
    }

    "todoを内容で検索できる" in {implicit s =>

      val results = repo.searchTodos(SearchQuery().withContent("割り当てる"), 10)

      results.right.value.head.id mustBe 20
      results.right.value.head.title mustBe "タスクをこなす"
      results.right.value.head.content mustBe "割り当てられたタスクをこなす"
    }

    "todoを変更できる" in { implicit  s =>

      val target = repo.findById(10, ServerError("")).right.value

      val result = repo.modify(target.modifyTitle("modified"))

      result.right.value.title mustBe "modified"
    }

    "変更対象のtodoが存在しない場合はNotFound" in { implicit  s =>

      val result = repo.modify(Todo(99, "", ""))

      result.left.value mustBe a[NotFound]
    }

    "todoを削除できる" in {implicit s=>

      val result = repo.remove(10)

      result.isRight mustBe true
    }

    "存在しないtodoの削除はNotFound" in {implicit s=>

      val result = repo.remove(99)

      result.left.value mustBe a[NotFound]
    }
  }
}
