package io.scalaland.catnip

import cats.implicits._
import org.specs2.mutable.Specification

class CachedSpec extends Specification {

  "@Cached" should {

    "handle non-parametric classes" in {
      // given
      @Cached(cats.Order, cats.Show, cats.Semigroup) final case class NonParam(a: String, b: String)

      // when
      val result1 = cats.Eq[NonParam].eqv(NonParam("a", "b"), NonParam("a", "b"))
      val result2 = cats.Eq[NonParam].eqv(NonParam("a", "b"), NonParam("c", "d"))

      // then
      result1 must beTrue
      result2 must beFalse
    }

    "handle parametric classes" in {
      // given
      // cats.Show is not supported until https://github.com/typelevel/kittens/issues/102 is resolved
      @Cached(cats.Order, cats.Functor, cats.MonoidK) final case class Param[A](a: List[A])

      // when
      val result1 = cats.Eq[Param[String]].eqv(Param(List("a")), Param(List("a")))
      val result2 = cats.Eq[Param[String]].eqv(Param(List("a")), Param(List("b")))

      // then
      result1 must beTrue
      result2 must beFalse
    }

    "handle type aliases" in {
      // given
      @Cached(Aliased.X) final case class Aliases(a: String, b: String)

      // when
      val result1 = cats.Eq[Aliases].eqv(Aliases("a", "b"), Aliases("a", "b"))
      val result2 = cats.Eq[Aliases].eqv(Aliases("a", "b"), Aliases("c", "d"))

      // then
      result1 must beTrue
      result2 must beFalse
    }

    /*
    // TODO: local aliases fail type checker
    "handle type aliases" in {
      // given
      type X[A] = cats.Eq[A]; val X = cats.Eq
      @Cached(X) final case class Test(a: String, b: String)

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
      @Cached(X) final case class Test(a: String, b: String)

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
      @Cached(cats.Eq) final case class TestEq(a: String)

      // when
      val result1 = cats.Eq[TestEq].eqv(TestEq("a"), TestEq("a"))
      val result2 = cats.Eq[TestEq].eqv(TestEq("a"), TestEq("b"))

      // then
      result1 must beTrue
      result2 must beFalse
    }

    "generate for cats.PartialOrder" in {
      // given
      @Cached(cats.PartialOrder) final case class TestPO(a: String)

      // when
      val result1 = TestPO("a") <= TestPO("a")
      val result2 = TestPO("a") >= TestPO("b")

      // then
      result1 must beTrue
      result2 must beFalse
    }

    "generate for cats.Order" in {
      // given
      @Cached(cats.Order) final case class TestO(a: String)

      // when
      val result1 = TestO("a") min TestO("a")
      val result2 = TestO("a") max TestO("b")

      // then
      result1 must beEqualTo(TestO("a"))
      result2 must beEqualTo(TestO("b"))
    }

    "generate for cats.Hash" in {
      // given
      @Cached(cats.Hash) final case class TestHash(a: String)

      // when
      val result1 = cats.Hash.hash(TestHash("a"))
      val result2 = cats.Hash.hash(TestHash("b"))

      // then
      result1 must not(beEqualTo(result2))
    }

    "generate for cats.Functor" in {
      // given
      @Cached(cats.Functor) final case class TestFunctor[A](a: A)

      // when
      val result1 = TestFunctor("1").map(_.toInt)
      val result2 = TestFunctor("2").map(_.toInt)

      // then
      result1 must beEqualTo(TestFunctor(1))
      result2 must beEqualTo(TestFunctor(2))
    }

    "generate for cats.Show" in {
      // given
      @Cached(cats.Show) final case class TestShow(a: String)

      // when
      val result1 = TestShow("a").show
      val result2 = TestShow("b").show

      // then
      result1 must beEqualTo("TestShow(a = a)")
      result2 must beEqualTo("TestShow(a = b)")
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
      @Cached(cats.Semigroup) final case class TestSemi(a: String)

      // when
      val result1 = TestSemi("a") |+| TestSemi("a")
      val result2 = TestSemi("b") |+| TestSemi("a")

      // then
      result1 must beEqualTo(TestSemi("aa"))
      result2 must beEqualTo(TestSemi("ba"))
    }

    "generate for cats.SemigroupK" in {
      // given
      @Cached(cats.SemigroupK, cats.Eq) final case class TestSemiK[A](a: List[A])

      // when
      implicit val a = cats.SemigroupK[TestSemiK].algebra[String]
      val result1    = TestSemiK[String](List("a")) |+| TestSemiK[String](List("a"))
      val result2    = TestSemiK[String](List("b")) |+| TestSemiK[String](List("a"))

      // then
      result1 must beEqualTo(TestSemiK(List("a", "a")))
      result2 must beEqualTo(TestSemiK(List("b", "a")))
    }
  }
}
