# catnip

[![Build Status](https://travis-ci.org/scalalandio/catnip.svg?branch=master)](https://travis-ci.org/scalalandio/catnip)
[![Maven Central](https://img.shields.io/maven-central/v/io.scalaland/catnip_2.12.svg)](http://search.maven.org/#search%7Cga%7C1%7Ccatnip)
[![License](http://img.shields.io/:license-Apache%202-green.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

Static annotations for Kittens for people who don't like to write
semiautomatic derivations into companion objects themselves.

## Usage

Add to your sbt

```scala
libraryDependencies += "io.scalaland" %% "catnip" % catnipVersion // see Maven badge
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross sbt.CrossVersion.patch)
```

or, if you use Scala.js

```scala
libraryDependencies += "io.scalaland" %%% "catnip" % catnipVersion // see Maven badge
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross sbt.CrossVersion.patch)
```

From now on you can add implicit Kittens-generated type classes for your case classes
with a simple macro-annotation:

```scala
import io.scalaland.catnip._
import cats._
import cats.implicits._ // don't forget to import the right implicits!

@Semi(Eq, Monoid, Show) final case class Test(a: String)

Test("a") === Test("b") // false
Test("a") |+| Test("b") // Test("ab")
Test("a").show          // "Test(a = a)"
```

You can also test it with ammonite like:

```scala
import $ivy.`io.scalaland::catnip:0.4.0`, io.scalaland.catnip._, cats._, cats.implicits._
interp.load.plugin.ivy("org.scalamacros" % "paradise_2.12.4" % "2.1.1")

@Semi(Eq, Monoid, Functor) final case class Test[A](a: A)

Test("a") === Test("b") // false
Test("a") |+| Test("b") // Test("ab")
Test("1").map(_.toInt)  // Test(1)
```

## Implemented

 * `@Semi`: `cats.Eq`, `cats.PartialOrder`, `cats.Order`, `cats.Hash`,
   `cats.Functor`, `cats.Foldable`, `cats.Show`,  `cats.Monoid`, `cats.MonoidK`,
   `cats.Semigroup`, `cats.SemigroupK`, `alleycats.Empty`.

## Internals

Macro turns

```scala
@Semi(cats.Semigroup) final case class TestSemi(a: String)

@Semi(cats.SemigroupK, cats.Eq) final case class TestSemiK[A](a: List[A])
```
into
```scala
final case class TestSemi(a: String)
object TestSemi {
  implicit val _derived_cats_kernel_Semigroup = cats.derived.semi.semigroup[TestSemi]
}

final case class TestSemiK[A](a: List[A])
object TestSemiK {
  implicit val _derived_cats_SemigroupK = cats.derived.semi.semigroupK[TestSemiK];
  implicit def _derived_cats_kernel_Eq[A](implicit cats_kernel_Eq_a: cats.kernel.Eq[List[A]]) = cats.derived.semi.eq[TestSemiK[A]]
}
```

In order to do so it:

 * takes the companion object from the argument
 * turns it into a class name an dealias it (so CO should match the class!)
 * then reads [`derive.semi.conf`](modules/catnip/src/main/resources/derive.semi.conf)
   - this class contains type class to kittens generator mappings
 * for plain types is just paste the body
 * for parametric types `[A]` is reuses `TypeClass` to create an implicit
   `TypeClass[A]` argument
 * in special cases like `Show` which would require additional type class
   (`shapeless.Typeable[A]`), they are defined in config after the generator
   function and separated by commas

Therefore, you should be able to extend the abilities of the macro by expanding
the content of `derive.semi.conf`. (Some merge strategy for resources I guess?
That and making sure that compiler _sees_ the resources, since if you define them
in the same project you want compiler to use them it is not the case).

## Limitations

Type checker complains if you use type aliases from the same compilation unit

```scala
type X = cats.Eq; val X = cats.Eq
@Semi(X) final case class Test(a: String)
// scala.reflect.macros.TypecheckException: not found: type X
```
same if you rename type during import
```scala
import cats.{ Eq => X }
@Semi(X) final case class Test(a: String)
// scala.reflect.macros.TypecheckException: not found: type X
```
   
However, if you simply import definitions or aliases already defined somewhere else,
you should have no issues.
