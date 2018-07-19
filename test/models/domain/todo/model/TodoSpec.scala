package models.domain.todo.model

import org.scalacheck._
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatest.prop.PropertyChecks

class TodoSpec extends WordSpec with MustMatchers with PropertyChecks {

  private val arbString = Arbitrary.arbString.arbitrary

  "todo" should {

    "タイトルを変更できる" in {

      forAll(arbString) { str =>

        val modified = Todo(1, "", "").modifyTitle(str)

        modified.title mustBe str
      }
    }

    "タイトルを変更しても内容は変わらない" in {

      forAll(arbString) { str =>

        val todo = Todo(1, "", "")
        val modified = todo.modifyTitle(str)

        modified.content mustBe todo.content
      }
    }

    "内容を変更できる" in {

      forAll(arbString) { str =>

        val modified = Todo(1, "", "").modifyContent(str)

        modified.content mustBe str
      }
    }

    "内容を変更してもタイトルは変わらない" in {

      forAll(arbString) { str =>

        val todo = Todo(1, "", "")
        val modified = todo.modifyContent(str)

        modified.title mustBe todo.title
      }
    }
  }
}
