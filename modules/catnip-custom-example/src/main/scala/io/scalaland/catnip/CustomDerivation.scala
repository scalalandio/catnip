package io.scalaland.catnip

import shapeless._

// 1. Let's define a type class (or have a library define it for you)
trait CustomDerivation[A] {

  def doSomething(a: A): A
}

object NotACompanion {

  trait Derived[A] extends CustomDerivation[A]

  private def instance[A](f: A => A): Derived[A] = new Derived[A] {
    override def doSomething(a: A) = f(a)
  }

  // 2. Let's pretend this is semi-auto macro or sth.
  //
  // To configure a custom derivation we have to add full.name.to.derive.method=full.path.to.method into
  // `derive.semi.conf` e.g.:
  //   io.scalaland.catnip.CustomDerivation=io.scalaland.catnip.NotACompanion.derive
  // Then when (if CustomDerivation had a companion) @Semi(CustomDerivation) would see that it has to derive
  // CustomDerivation and it will use implementation from NotACompanion.derive.
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

// 3. Normally @Semi(TypeClass) takes a companion object as a parameter and from it obtains name of the type class.
//
// If a type class is missing a companion which we could pass as @Semi(TypeClass) we could create a "fake" companion
// which we could use instead and configure it in derive.stub.conf e.g.
//   io.scalaland.catnip.FakeCompanion=io.scalaland.catnip.CustomDerivation
// Then we could pass that fake companion into Semi e.g. @Semi(FakeCompanion), and Catnip would:
// * figure that FakeCompanion refers to CustomDerivation type class
// * use method NotACompanion.derive to instantiante it
//
// To make things less confusing for users in such cases, we can create a type alias for a type class with the same name
// as our fake companion.
object FakeCompanion
