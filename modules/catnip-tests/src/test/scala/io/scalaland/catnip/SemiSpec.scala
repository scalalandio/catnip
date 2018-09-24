package io.scalaland.catnip

import cats.implicits._
import org.specs2.mutable.Specification

class SemiSpec extends Specification {

  "@Semi" should {

    "handle non-parametric classes" in {
      // given
      @Semi(cats.Order, cats.Show, cats.Monoid) final case class NonParam(a: String, b: String)

      // when
      val result1 = cats.Eq[NonParam].eqv(NonParam("a", "b"), NonParam("a", "b"))
      val result2 = cats.Eq[NonParam].eqv(NonParam("a", "b"), NonParam("c", "d"))

      // then
      result1 must beTrue
      result2 must beFalse
    }

    "handle parametric classes" in {
      // given
      @Semi(
        cats.Order, cats.Show, cats.Functor, cats.Foldable, cats.MonoidK, alleycats.Empty
      ) final case class Param[A](a: List[A])

      // when
      val result1 = cats.Eq[Param[String]].eqv(Param(List("a")), Param(List("a")))
      val result2 = cats.Eq[Param[String]].eqv(Param(List("a")), Param(List("b")))

      // then
      result1 must beTrue
      result2 must beFalse
    }

    "handle type aliases" in {
      // given
      @Semi(Aliased.X) final case class Aliases(a: String, b: String)

      // when
      val result1 = cats.Eq[Aliases].eqv(Aliases("a", "b"), Aliases("a", "b"))
      val result2 = cats.Eq[Aliases].eqv(Aliases("a", "b"), Aliases("c", "d"))

      // then
      result1 must beTrue
      result2 must beFalse
    }

    "handle sealed hierarchies" in {
      // given
      @Semi(cats.Eq) sealed trait Test extends Product with Serializable
      object Test {
        // @Semi(cats.Eq) // TODO: handle this without knownDirectSubclass issue
        final case class CaseClass(a: String) extends Test
        case object CaseObject extends Test
      }

      // when
      val result1 = cats.Eq[Test].eqv(Test.CaseClass("a"), Test.CaseClass("a"))
      val result2 = cats.Eq[Test].eqv(Test.CaseClass("a"), Test.CaseObject)

      // then
      result1 must beTrue
      result2 must beFalse
    }

    /*
    // TODO: local aliases fail type checker
    "handle type aliases" in {
      // given
      type X[A] = cats.Eq[A]; val X = cats.Eq
      @Semi(X) final case class Test(a: String, b: String)

      // when
      val result1 = cats.Eq[Test].eqv(Test("a", "b"), Test("a", "b"))
      val result2 = cats.Eq[Test].eqv(Test("a", "b"), Test("c", "d"))

      // then
      result1 must beTrue
      result2 must beFalse
    }
     */

    /*
     // TODO: renamed imports fail type checker
    "handle imports" in {
      // given
      import cats.{ Eq => X }
      @Semi(X) final case class Test(a: String, b: String)

      // when
      val result1 = X[Test].eqv(Test("a", "b"), Test("a", "b"))
      val result2 = X[Test].eqv(Test("a", "b"), Test("c", "d"))

      // then
      result1 must beTrue
      result2 must beFalse
    }
     */

    "generate for cats.Eq" in {
      // given
      @Semi(cats.Eq) final case class TestEq(a: String)

      // when
      val result1 = cats.Eq[TestEq].eqv(TestEq("a"), TestEq("a"))
      val result2 = cats.Eq[TestEq].eqv(TestEq("a"), TestEq("b"))

      // then
      result1 must beTrue
      result2 must beFalse
    }

    "generate for cats.PartialOrder" in {
      // given
      @Semi(cats.PartialOrder) final case class TestPO(a: String)

      // when
      val result1 = TestPO("a") <= TestPO("a")
      val result2 = TestPO("a") >= TestPO("b")

      // then
      result1 must beTrue
      result2 must beFalse
    }

    "generate for cats.Order" in {
      // given
      @Semi(cats.Order) final case class TestO(a: String)

      // when
      val result1 = TestO("a") min TestO("a")
      val result2 = TestO("a") max TestO("b")

      // then
      result1 must beEqualTo(TestO("a"))
      result2 must beEqualTo(TestO("b"))
    }

    "generate for cats.Hash" in {
      // given
      @Semi(cats.Hash) final case class TestHash(a: String)

      // when
      val result1 = cats.Hash.hash(TestHash("a"))
      val result2 = cats.Hash.hash(TestHash("b"))

      // then
      result1 must not(beEqualTo(result2))
    }

    "generate for cats.Functor" in {
      // given
      @Semi(cats.Functor) final case class TestFunctor[A](a: A, b: A)

      // when
      val result1 = TestFunctor("1", "3").map(_.toInt)
      val result2 = TestFunctor("2", "4").map(_.toInt)

      // then
      result1 must beEqualTo(TestFunctor(1, 3))
      result2 must beEqualTo(TestFunctor(2, 4))
    }

    "generate for cats.Foldable" in {
      // given
      @Semi(cats.Foldable) final case class TestFoldable[A](a: A)

      // when
      val result1 = TestFoldable("1").foldLeft(0) { (a, i) =>
        a + i.toInt
      }
      val result2 = TestFoldable("2").foldLeft(0) { (a, i) =>
        a + i.toInt
      }

      // then
      result1 must beEqualTo(1)
      result2 must beEqualTo(2)
    }

    "generate for cats.Traverse" in {
      // given
      @Semi(cats.Traverse) final case class TestTraverse[A](a: A)

      // when
      val result1 = TestTraverse(Option("1")).sequence
      val result2 = TestTraverse(List("1", "2")).traverse(_.map(_.toInt))

      // then
      result1 must beEqualTo(Some(TestTraverse("1")))
      result2 must beEqualTo(List(TestTraverse(1), TestTraverse(2)))
    }

    "generate for cats.Show" in {
      // given
      @Semi(cats.Show) final case class TestShow(a: String)

      // when
      val result1 = TestShow("a").show
      val result2 = TestShow("b").show

      // then
      result1 must beEqualTo("TestShow(a = a)")
      result2 must beEqualTo("TestShow(a = b)")
    }

    "generate for cats.Monoid" in {
      // given
      @Semi(cats.Monoid, cats.Eq) final case class TestMonoid(a: String)

      // when
      val result1 = TestMonoid("").isEmpty
      val result2 = TestMonoid("b").isEmpty

      // then
      result1 must beTrue
      result2 must beFalse
    }

    "generate for cats.MonoidK" in {
      // given
      @Semi(cats.MonoidK, cats.Eq) final case class TestMonoidK[A](a: List[A])

      // when
      implicit val a = cats.MonoidK[TestMonoidK].algebra[String]
      val result1    = TestMonoidK[String](List()).isEmpty
      val result2    = TestMonoidK[String](List("")).isEmpty

      // then
      result1 must beTrue
      result2 must beFalse
    }

    "generate for cats.Semigroup" in {
      // given
      @Semi(cats.Semigroup) final case class TestSemi(a: String)

      // when
      val result1 = TestSemi("a") |+| TestSemi("a")
      val result2 = TestSemi("b") |+| TestSemi("a")

      // then
      result1 must beEqualTo(TestSemi("aa"))
      result2 must beEqualTo(TestSemi("ba"))
    }

    "generate for cats.SemigroupK" in {
      // given
      @Semi(cats.SemigroupK, cats.Eq) final case class TestSemiK[A](a: List[A])

      // when
      implicit val a = cats.SemigroupK[TestSemiK].algebra[String]
      val result1    = TestSemiK[String](List("a")) |+| TestSemiK[String](List("a"))
      val result2    = TestSemiK[String](List("b")) |+| TestSemiK[String](List("a"))

      // then
      result1 must beEqualTo(TestSemiK(List("a", "a")))
      result2 must beEqualTo(TestSemiK(List("b", "a")))
    }

    "generate for alleycats.Empty" in {
      // given
      @Semi(alleycats.Empty) final case class TestEmpty[A](a: A)

      // when
      val result = alleycats.Empty[TestEmpty[String]].empty

      // then
      result must beEqualTo(TestEmpty(""))
    }
  }
}
