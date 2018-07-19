package models.infra.db

import helper.{DataStoreReset, FixtureSpec}
import models.domain.error.NotFound
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class EsSpec extends FixtureSpec with GuiceOneAppPerSuite with DataStoreReset {

  override def fixture(implicit session: FixtureParam): Unit = {
    es.recreateIndex()
    es.storeDocument(10, Document("title_10", "content_10"))
    es.storeDocument(20, Document("タスクをこなす", "割り当てられたタスクをこなす"))
  }

  "Es" should {

    "ドキュメントが保存できる" in { _ =>

      val test = Document("title", "content")

      val result = for {
        _   <- es.storeDocument(1, test)
        ret <- es.findDocumentById(1)
      } yield ret

      result.right.value.title mustBe "title"
      result.right.value.content mustBe "content"
    }

    "既存のドキュメントを更新できる" in { _ =>

      val result = es.updateDocument(10, Document("title_modified", "content_modified"))

      result.right.value.title mustBe "title_modified"
      result.right.value.content mustBe "content_modified"
    }

    "存在しないドキュメントに対する更新はNotFound" in { _ =>

      val result = es.updateDocument(99, Document("title_modified", "content_modified"))

      result.left.value mustBe a[NotFound]
    }

    "findOptでidで存在するドキュメントを取得できる" in { _ =>

      val result = es.findDocumentByIdOpt(10)

      result.right.value.value.title mustBe "title_10"
      result.right.value.value.content mustBe "content_10"
    }

    "findOptでidで存在しないドキュメントを取得するとNoneになる" in { _ =>

      val result = es.findDocumentByIdOpt(99)

      result.right.value mustBe None
    }

    "findでidで存在するドキュメントを取得できる" in { _ =>

      val result = es.findDocumentById(10)

      result.right.value.title mustBe "title_10"
      result.right.value.content mustBe "content_10"
    }

    "findでidで存在しないドキュメントを取得するとNotFoundになる" in { _ =>

      val result = es.findDocumentById(99)

      result.left.value mustBe a[NotFound]
    }

    "idで既存のドキュメントを削除できる" in { _ =>

      val result = es.deleteDocument(10)

      val u: Unit = ()
      result.right.value mustBe u
    }

    "idで存在しないドキュメントを削除するとNotFound" in { _ =>

      val result = es.deleteDocument(99)

      result.left.value mustBe a[NotFound]
    }

    "ドキュメントをタイトルで検索できる" in {_ =>

      val result = es.searchEs(Map("title" -> "タスク"), 10)

      result.right.value.head mustBe (20, Document("タスクをこなす", "割り当てられたタスクをこなす"))
    }

    "ドキュメントを内容で検索できる" in {_ =>

      val result = es.searchEs(Map("content" -> "タスク"), 10)

      result.right.value.head mustBe (20, Document("タスクをこなす", "割り当てられたタスクをこなす"))
    }

    "ドキュメントをタイトルと内容で検索できる" in {_ =>

      val result = es.searchEs(Map("title" -> "タスク", "content" -> "割り当てる"), 10)

      result.right.value.head mustBe (20, Document("タスクをこなす", "割り当てられたタスクをこなす"))
    }
  }

}
