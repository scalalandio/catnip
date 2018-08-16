package io.scalaland.catnip

import cats._
import cats.derived._
import cats.implicits._
import org.specs2.mutable.Specification

class SemiSpec extends Specification {

  "@Semi" should {

    "handle non-parametric classes" in {
      // given
      @Semi(cats.Eq, cats.Show) final case class Test(a: String, b: String)

      // when
      val result1 = Eq[Test].eqv(Test("a", "b"), Test("a", "b"))
      val result2 = Eq[Test].eqv(Test("a", "b"), Test("c", "d"))

      // then
      result1 must beTrue
      result2 must beFalse
    }
  }
}
