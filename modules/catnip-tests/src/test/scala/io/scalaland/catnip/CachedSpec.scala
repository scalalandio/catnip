package io.scalaland.catnip

import cats.implicits._
import org.specs2.mutable.Specification

class CachedSpec extends Specification {

  "@Cached" should {

    "handle non-parametric classes" in {
      // given
      cats.derived.cached.eq
      @Cached(cats.Eq) final case class Test(a: String, b: String)

      // when
      val result1 = cats.Eq[Test].eqv(Test("a", "b"), Test("a", "b"))
      val result2 = cats.Eq[Test].eqv(Test("a", "b"), Test("c", "d"))

      // then
      result1 must beTrue
      result2 must beFalse
    }

    "handle parametric classes" in {
      // given
      @Cached(cats.Eq) final case class Test[A](a: A)

      // when
      val result1 = cats.Eq[Test[String]].eqv(Test("a"), Test("a"))
      val result2 = cats.Eq[Test[String]].eqv(Test("a"), Test("b"))

      // then
      result1 must beTrue
      result2 must beFalse
    }

    "handle type aliases" in {
      // given
      @Cached(Aliased.X) final case class Test(a: String, b: String)

      // when
      val result1 = cats.Eq[Test].eqv(Test("a", "b"), Test("a", "b"))
      val result2 = cats.Eq[Test].eqv(Test("a", "b"), Test("c", "d"))

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
      @Cached(cats.Eq) final case class Test(a: String)

      // when
      val result1 = cats.Eq[Test].eqv(Test("a"), Test("a"))
      val result2 = cats.Eq[Test].eqv(Test("a"), Test("b"))

      // then
      result1 must beTrue
      result2 must beFalse
    }

    "generate for cats.PartialOrder" in {
      // given
      @Cached(cats.PartialOrder) final case class Test(a: String)

      // when
      val result1 = Test("a") <= Test("a")
      val result2 = Test("a") >= Test("b")

      // then
      result1 must beTrue
      result2 must beFalse
    }

    "generate for cats.Order" in {
      // given
      @Cached(cats.Order) final case class Test(a: String)

      // when
      val result1 = Test("a") min Test("a")
      val result2 = Test("a") max Test("b")

      // then
      result1 must beEqualTo(Test("a"))
      result2 must beEqualTo(Test("b"))
    }

    "generate for cats.Hash" in {
      // given
      @Cached(cats.Hash) final case class Test(a: String)

      // when
      val result1 = cats.Hash.hash(Test("a"))
      val result2 = cats.Hash.hash(Test("b"))

      // then
      result1 must not(beEqualTo(result2))
    }

    "generate for cats.Functor" in {
      // given
      @Cached(cats.Functor) final case class Test[A](a: A)

      // when
      val result1 = Test("1").map(_.toInt)
      val result2 = Test("2").map(_.toInt)

      // then
      result1 must beEqualTo(Test(1))
      result2 must beEqualTo(Test(2))
    }

    "generate for cats.Show" in {
      // given
      @Cached(cats.Show) final case class Test(a: String)

      // when
      val result1 = Test("a").show
      val result2 = Test("b").show

      // then
      result1 must beEqualTo("Test(a = a)")
      result2 must beEqualTo("Test(a = b)")
    }

    "generate for cats.MonoidK" in {
      // given
      @Semi(cats.MonoidK, cats.Eq) final case class Test[A](a: List[A])

      // when
      implicit val a = cats.MonoidK[Test].algebra[String]
      val result1    = Test[String](List()).isEmpty
      val result2    = Test[String](List("")).isEmpty

      // then
      result1 must beTrue
      result2 must beFalse
    }

    "generate for cats.Semigroup" in {
      // given
      @Cached(cats.Semigroup) final case class Test(a: String)

      // when
      val result1 = Test("a") |+| Test("a")
      val result2 = Test("b") |+| Test("a")

      // then
      result1 must beEqualTo(Test("aa"))
      result2 must beEqualTo(Test("ba"))
    }

    "generate for cats.SemigroupK" in {
      // given
      @Cached(cats.SemigroupK, cats.Eq) final case class Test[A](a: List[A])

      // when
      implicit val a = cats.SemigroupK[Test].algebra[String]
      val result1    = Test[String](List("a")) |+| Test[String](List("a"))
      val result2    = Test[String](List("b")) |+| Test[String](List("a"))

      // then
      result1 must beEqualTo(Test(List("a", "a")))
      result2 must beEqualTo(Test(List("b", "a")))
    }
  }
}
