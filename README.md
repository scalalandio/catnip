# catnip

[![Build Status](https://travis-ci.org/scalalandio/catnip.svg?branch=master)](https://travis-ci.org/scalalandio/catnip)
[![Maven Central](https://img.shields.io/maven-central/v/io.scalaland/catnip_2.13.svg)](http://search.maven.org/#search%7Cga%7C1%7Ccatnip)
[![Scala.js](https://www.scala-js.org/assets/badges/scalajs-1.0.0.svg)](https://www.scala-js.org)
[![License](http://img.shields.io/:license-Apache%202-green.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

Static annotations for Kittens for people who don't like to write
semiautomatic derivations into companion objects themselves.

## Usage

Add to your sbt (2.11, 2.12, 2.13):

```scala
libraryDependencies += "io.scalaland" %% "catnip" % catnipVersion // see Maven badge
// If you use Scala >= 2.13:
scalacOptions += "-Ymacro-annotations"
// If you use Scala 2.12 or 2.11:
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross sbt.CrossVersion.patch)
```

or, if you use Scala.js:

```scala
libraryDependencies += "io.scalaland" %%% "catnip" % catnipVersion // see Maven badge
// If you use Scala >= 2.13:
scalacOptions += "-Ymacro-annotations"
// If you use Scala 2.12 or 2.11:
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross sbt.CrossVersion.patch)
```

From now on you can add implicit Kittens-generated type classes for your case classes
with a simple macro-annotation:

```scala
import io.scalaland.catnip._
import cats._
import cats.implicits._ // don't forget to import the right implicits!
import alleycats.std.all._ // might also come handy

@Semi(Eq, Monoid, Show) final case class Test(a: String)

Test("a") === Test("b") // false
Test("a") |+| Test("b") // Test("ab")
Test("a").show          // "Test(a = a)"
```

You can also test it with ammonite like:

```scala
interp.preConfigureCompiler(_.processArguments(List("-Ymacro-annotations"), true))

@

import $ivy.`io.scalaland::catnip:1.0.0`, io.scalaland.catnip._, cats._, cats.implicits._

@Semi(Eq, Monoid, Functor) final case class Test[A](a: A)

Test("a") === Test("b") // false
Test("a") |+| Test("b") // Test("ab")
Test("1").map(_.toInt)  // Test(1)
```

## Implemented

`cats.Eq`, `cats.PartialOrder`, `cats.Order`,
`cats.Functor`, `cats.Foldable`, `cats.Traverse`, `cats.Show`, `cats.derived.ShowPretty`,
`cats.Monoid`, `cats.MonoidK`, `cats.Semigroup`, `cats.SemigroupK`,
`alleycats.Empty`, `alleycats.Pure`.

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

## Customizations

You should be able to extend the abilities of the macro by expanding
the content of `derive.semi.conf`. You can create this file and add it to your library
if you want Catnip to support it as all files with that name are looked through during
compilation. When it comes to sbt it doesn't export resources to `Compile` scope,
so your configs might not be visible in your modules while they would be available
in created JARs. (Creating somewhat inconsistent experience).
Personally, I fixed this by adding something like

```
val myProject = project.in(file("my-project"))
  // other settings
  .settings(Compile / unmanagedResourceDirectories += baseDirectory.value / "../src/main/resources")
```

to sbt. This will make your customizations immediately available to your modules.

Take a look at an [example](modules/catnip-custom-example) project to see how it works
in practice.

## Debugging

To debug you can use `catnip.debug` flag:

```
> sbt -Dcatnip.debug=debug # show info about derivation
> sbt -Dcatnip.debug=trace # show even more infor about derivation
```

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
