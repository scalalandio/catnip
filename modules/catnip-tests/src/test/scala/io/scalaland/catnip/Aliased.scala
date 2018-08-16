package io.scalaland.catnip

object Aliased {

  type X[A] = cats.Eq[A]
  val X = cats.Eq
}
