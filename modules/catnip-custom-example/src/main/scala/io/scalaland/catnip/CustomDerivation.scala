package io.scalaland.catnip

import shapeless._

// define a type class (or have a library define it for you)
trait CustomDerivation[A] {

  def doSomething(a: A): A
}

object NotACompanion {

  trait Derived[A] extends CustomDerivation[A]

  private def instance[A](f: A => A): Derived[A] = new Derived[A] {
    override def doSomething(a: A) = f(a)
  }

  // let's pretend this is semi-auto macro or sth
  //
  // add full.name.to.derive.method into derive.semi.conf e.g.:
  //   io.scalaland.catnip.CustomDerivation=io.scalaland.catnip.NotACompanion.derive
  def derive[A](implicit tc: Derived[A]): CustomDerivation[A] = tc

  implicit def hlist[A, ARepr <: HList](implicit gen: Generic.Aux[A, ARepr], reprTC: Derived[ARepr]): Derived[A] =
    instance { a =>
      gen.from(reprTC.doSomething(gen.to(a)))
    }

  implicit val hNil: Derived[HNil] = instance { _ =>
    HNil
  }

  implicit def hCons[H, T <: HList](implicit hTC: Derived[H], tTC: Derived[T]): Derived[H :: T] = instance {
    case h :: t =>
      hTC.doSomething(h) :: tTC.doSomething(t)
  }

  implicit val string: Derived[String] = instance(s => s)

  implicit val int: Derived[Int] = instance(s => s)
}

// if a type class is missing a companion which we could pass as @Semi(TypeClass)
// we could create a fake companion which we could use instead and configure it in derive.stub.conf e.g.
//   io.scalaland.catnip.FakeCompanion=io.scalaland.catnip.CustomDerivation
object FakeCompanion
