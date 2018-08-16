package io.scalaland.catnip

import cats.implicits._
import org.specs2.mutable.Specification

class SemiSpec extends Specification {

  "@Semi" should {

    "handle non-parametric classes" in {
      // given
      @Semi(cats.Eq) final case class Test(a: String, b: String)

      // when
      val result1 = cats.Eq[Test].eqv(Test("a", "b"), Test("a", "b"))
      val result2 = cats.Eq[Test].eqv(Test("a", "b"), Test("c", "d"))

      // then
      result1 must beTrue
      result2 must beFalse
    }

    "handle parametric classes" in {
      // given
      @Semi(cats.Eq) final case class Test[A](a: A)

      // when
      val result1 = cats.Eq[Test[String]].eqv(Test("a"), Test("a"))
      val result2 = cats.Eq[Test[String]].eqv(Test("a"), Test("b"))

      // then
      result1 must beTrue
      result2 must beFalse
    }

    "handle type aliases" in {
      // given
      @Semi(Aliased.X) final case class Test(a: String, b: String)

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
  }
}
