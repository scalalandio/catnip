package io.scalaland.catnip

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

class CommonItSpec extends Specification with Mockito {

  "Common integration" should {

    "integrate with Cauchy" in {
      1 mustEqual 1
    }
  }
}
