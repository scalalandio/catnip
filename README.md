# catnip

Static annotations for Kittens for people who don't like to semiautomatic
derivations into companion objects themselves.

## Usage

> Not published anywhere yet.

```scala
import catnip._
import cats.implicits._

@Semi(cats.Eq, cats.Monoid, cats.Show) final case class Test(a: String)

Test("a") === Test("b)  // false
Test("a") |+| Test("b") // Test("ab")
Test("a").show          // "Test(a = a)"
```

## Implemented

 * `@Semi`: `cats.Eq`, `cats.PartialOrder`, `cats.Order`, `cats.Hash`,
   `cats.Show`,  `cats.Monoid`, `cats.Semigroup`, `alleycats.Empty`,
 * `@Cached`: `cats.Eq`, `cats.PartialOrder`, `cats.Order`, `cats.Hash`,
   `cats.Show`, `cats.Semigroup`.

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
 * type classes taking type constructor as a parameter are not supported
   (`cats.Functor`, `cats.Foldable`, `cats.MonoidK`, `cats.SemigroupK`).
