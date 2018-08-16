package io.scalaland.catnip

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

class CommonSpec extends Specification with Mockito {

  "Common" should {

    "measure units" in {
      1 mustEqual 1
    }
  }
}
