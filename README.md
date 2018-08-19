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

From now on you can add implicit kitten-generated type classes to you case classes
with a simple macro-annotation:

```scala
import io.scalaland.catnip._
import cats.implicits._

@Semi(cats.Eq, cats.Monoid, cats.Show) final case class Test(a: String)

Test("a") === Test("b") // false
Test("a") |+| Test("b") // Test("ab")
Test("a").show          // "Test(a = a)"
```

You can also test it with ammonite like:

```scala
import $ivy.`io.scalaland::catnip:0.1`, io.scalaland.catnip._, cats.implicits._
interp.load.plugin.ivy("org.scalamacros" % "paradise_2.12.4" % "2.1.1")

@Semi(cats.Eq, cats.Monoid, cats.Functor) final case class Test[A](a: A)
```

## Implemented

 * `@Semi`: `cats.Eq`, `cats.PartialOrder`, `cats.Order`, `cats.Hash`,
   `cats.Functor`, `cats.Foldable`, `cats.Show`,  `cats.Monoid`, `cats.MonoidK`,
   `cats.Semigroup`, `cats.SemigroupK`, `alleycats.Empty`,
 * `@Cached`: `cats.Eq`, `cats.PartialOrder`, `cats.Order`, `cats.Hash`,
   `cats.Functor`, `cats.Show`, `cats.MonoidK`, `cats.Semigroup`,
   `cats.SemigroupK`.

## Limitations

 * type checker complains if you use type aliases from the same compilation unit
   ```scala
   type X = cats.Eq; val X = cats.Eq
   @Semi(X) final case class Test(a: String)
   // scala.reflect.macros.TypecheckException: not found: type X
   ```
 * same if you rename type during import
   ```scala
   import cats.{ Eq => X }
   @Semi(X) final case class Test(a: String)
   // scala.reflect.macros.TypecheckException: not found: type X
   ```
