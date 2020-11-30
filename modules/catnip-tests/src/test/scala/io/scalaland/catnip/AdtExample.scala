package io.scalaland.catnip

import cats.{Eq, Functor, Show}

@Semi(Show, Functor, Eq) sealed trait AdtExample[T] extends Product with Serializable
object AdtExample {
  case object ObjectCase extends AdtExample[Nothing]
  final case class Mono(str: String) extends AdtExample[Nothing]
  final case class Poly[T](v: T) extends AdtExample[T]
}
