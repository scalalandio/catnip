# catnip

[![Build Status](https://travis-ci.org/scalalandio/catnip.svg?branch=master)](https://travis-ci.org/scalalandio/catnip)

Static annotations for Kittens for people who don't like to write
semiautomatic derivations into companion objects themselves.

## Usage

> Not published anywhere yet.

```scala
import catnip._
import cats.implicits._

@Semi(cats.Eq, cats.Monoid, cats.Show) final case class Test(a: String)

Test("a") === Test("b") // false
Test("a") |+| Test("b") // Test("ab")
Test("a").show          // "Test(a = a)"
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
