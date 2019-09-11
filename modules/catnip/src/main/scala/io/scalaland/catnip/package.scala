package io.scalaland

import cats.derived.MkHash
import cats.Hash
import shapeless.OrElse

package object catnip {

  // workaround for missing implicits in kittens derivation
  type PotentiallyDerivedHash[A]=Hash[A] OrElse MkHash[A]
}
