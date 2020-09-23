package io.scalaland.catnip

import org.specs2.mutable.Specification

@Semi(FakeCompanion) final case class CustomType(a: String, b: Int)

class CustomSpec extends Specification {

  "customized @Semi" should {

    "handle custom configs" in {
      // given
      val value = CustomType("test", 5354)

      // when
      val result = implicitly[CustomDerivation[CustomType]].doSomething(value)

      // then
      result must_=== value
    }
  }
}
