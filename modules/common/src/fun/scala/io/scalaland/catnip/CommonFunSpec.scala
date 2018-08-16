package io.scalaland.catnip

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

class CommonFunSpec extends Specification with Mockito {

  "Common integration" should {

    "function in Church" in {
      1 mustEqual 1
    }
  }
}
