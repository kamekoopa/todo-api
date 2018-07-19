import helper.{DataStoreReset, FixtureSpec}
import models.infra.json.ReadsWrites._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Writes._
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Results
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scalikejdbc._

class TodoControllerSpec extends FixtureSpec with GuiceOneAppPerSuite with DataStoreReset with Results {

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

  "TodoController" should {

    "Todoの作成に成功すると201が返る" in { _ =>

      val payload = Json.obj("title" -> "タイトル", "content" -> "内容")
      val ret = route(app, FakeRequest(POST, "/api/todos").withBody(payload)).get

      status(ret) mustBe CREATED
      (contentAsJson(ret) \ "title").as[String] mustBe "タイトル"
      (contentAsJson(ret) \ "content").as[String] mustBe "内容"
    }

    "Todoの作成に必要なパラメータが足りないと400が返る" in { _ =>

      val payload = Json.obj("title" -> "タイトル")
      val ret = route(app, FakeRequest(POST, "/api/todos").withBody(payload)).get

      status(ret) mustBe BAD_REQUEST
    }

    "Todoの変更に成功すると201が返る" in { _ =>

      val payload = Json.obj("title" -> "バグを直せ", "content" -> "割り当てられたバグを直す")
      val ret = route(app, FakeRequest(PUT, "/api/todos/10").withBody(payload)).get

      status(ret) mustBe CREATED
      (contentAsJson(ret) \ "title").as[String] mustBe "バグを直せ"
      (contentAsJson(ret) \ "content").as[String] mustBe "割り当てられたバグを直す"
    }

    "存在しないTodoを変更しようとすると404が返る" in { _ =>

      val payload = Json.obj("title" -> "バグを直せ", "content" -> "割り当てられたバグを直す")
      val ret = route(app, FakeRequest(PUT, "/api/todos/99").withBody(payload)).get

      status(ret) mustBe NOT_FOUND
    }

    "変更に必要なパラメータが足りないと400が返る" in { _ =>

      val payload = Json.obj("title" -> "バグを直せ")
      val ret = route(app, FakeRequest(PUT, "/api/todos/99").withBody(payload)).get

      status(ret) mustBe BAD_REQUEST
    }

    "idでTodoが200で取得できる" in { _ =>

      val ret = route(app, FakeRequest(GET, "/api/todos/10")).get

      status(ret) mustBe OK
      (contentAsJson(ret) \ "title").as[String] mustBe "バグを直す"
      (contentAsJson(ret) \ "content").as[String] mustBe "割り当てられたバグを直す"
    }

    "存在しないidでTodoを取得すると404が返る" in { _ =>

      val ret = route(app, FakeRequest(GET, "/api/todos/99")).get

      status(ret) mustBe NOT_FOUND
    }

    "タイトルのみでTodoが検索できる" in { _ =>

      val ret = route(app, FakeRequest(GET, "/api/todos?title=バグ")).get

      status(ret) mustBe OK
      (contentAsJson(ret) \ "todos" \ 0 \ "title").as[String] mustBe "バグを直す"
    }

    "内容のみでTodoが検索できる" in { _ =>

      val ret = route(app, FakeRequest(GET, "/api/todos?content=バグ")).get

      status(ret) mustBe OK
      (contentAsJson(ret) \ "todos" \ 0 \ "content").as[String] mustBe "割り当てられたバグを直す"
    }

    "タイトルと内容でTodoが検索できる" in { _ =>

      val ret = route(app, FakeRequest(GET, "/api/todos?title=バグ&content=割り当てる")).get

      status(ret) mustBe OK
      (contentAsJson(ret) \ "todos" \ 0 \ "title").as[String] mustBe "バグを直す"
    }

    "limitで検索結果数を制限できる" in { _ =>

      val ret = route(app, FakeRequest(GET, "/api/todos?limit=1")).get

      status(ret) mustBe OK
      (contentAsJson(ret) \ "todos").as[List[JsObject]] must have length 1
    }

    "todoが削除できる" in { _ =>

      val ret = route(app, FakeRequest(DELETE, "/api/todos/10")).get

      status(ret) mustBe NO_CONTENT
    }

    "存在しないidでtodoを削除しようとすると404が返る" in { _ =>

      val ret = route(app, FakeRequest(DELETE, "/api/todos/99")).get

      status(ret) mustBe NOT_FOUND
    }
  }
}
