package models.service

import helper.{DataStoreReset, FixtureSpec}
import models.domain.error.NotFound
import models.domain.todo.model.{CreateRequest, SearchQuery, Todo}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import scalikejdbc._

class TodoServiceSpec extends FixtureSpec with GuiceOneAppPerSuite with DataStoreReset {

  override def fixture(implicit session: FixtureParam): Unit = {

    es.recreateIndex()

    using(db()) { db =>
      db.localTx { s =>
        sql"truncate todos".update().apply()(s)

        storeDocument(10, "バグを直す", "割り当てられたバグを直す")
        sql"insert todos (id) values ('10')".update().apply()(s)

        storeDocument(20, "タスクをこなす", "割り当てられたタスクをこなす")
        sql"insert todos (id) values ('20')".update().apply()(s)
      }
    }
  }

  private val service = app.injector.instanceOf(classOf[TodoService])

  "TodoService" should {

    "Todoを登録できる" in { _ =>

      val result = service.registerNewTodo(CreateRequest("タイトル", "内容"))

      result.right.value.title mustBe "タイトル"
      result.right.value.content mustBe "内容"
    }

    "Todoを更新できる" in { _ =>

      val result = service.modifyTodo(10, CreateRequest("タイトル", "内容"))

      result.right.value.title mustBe "タイトル"
      result.right.value.content mustBe "内容"
    }

    "存在しないTodoを更新しようとするとNotFound" in { _ =>

      val result = service.modifyTodo(99, CreateRequest("タイトル", "内容"))

      result.left.value mustBe a[NotFound]
    }

    "Todoをidで取得できる" in { _ =>

      val result = service.getTodoByIdAsOption(10)

      result.right.value.value.id mustBe 10
      result.right.value.value.title mustBe "バグを直す"
      result.right.value.value.content mustBe "割り当てられたバグを直す"
    }

    "存在しないidのTodoを取得するとNone" in { _ =>

      val result = service.getTodoByIdAsOption(99)

      result.right.value mustBe None
    }

    "タイトルでTodoを検索できる" in { _ =>

      val result = service.searchTodo(SearchQuery().withTitle("バグ"), 10)

      result.right.value.head.title mustBe "バグを直す"
      result.right.value.head.content mustBe "割り当てられたバグを直す"
    }

    "内容でTodoを検索できる" in { _ =>

      val result = service.searchTodo(SearchQuery().withContent("割り当てる"), 10)

      result.right.value must contain theSameElementsAs List(
        Todo(10, "バグを直す", "割り当てられたバグを直す"),
        Todo(20, "タスクをこなす", "割り当てられたタスクをこなす")
      )
    }

    "タイトルと内容でTodoを検索できる" in { _ =>

      val result = service.searchTodo(SearchQuery().withTitle("タスク").withContent("割り当てる"), 10)

      result.right.value must contain theSameElementsAs List(
        Todo(20, "タスクをこなす", "割り当てられたタスクをこなす")
      )
    }

    "Todoを削除できる" in { _ =>

      val removeResult = service.removeTodoById(10)
      val getResult = service.getTodoByIdAsOption(10)

      removeResult.isRight mustBe true
      getResult.right.value mustBe None
    }

    "存在しないidのTodoの削除はNotFoundになる" in { _ =>

      val result = service.removeTodoById(99)

      result.left.value mustBe a[NotFound]
    }
  }
}
