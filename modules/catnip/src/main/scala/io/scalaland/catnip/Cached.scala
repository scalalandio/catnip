package io.scalaland.catnip

import io.scalaland.catnip.internals._

import scala.reflect.macros.whitebox.Context
import scala.language.experimental.macros
import scala.annotation.{ compileTimeOnly, StaticAnnotation }

@compileTimeOnly("enable macro paradise to expand macro annotations")
class Cached(typeClasses: Any*) extends StaticAnnotation {

  def macroTransform(annottees: Any*): Any = macro Cached.impl
}

private object Cached {

  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = DerivedImpl.impl(DerivedImpl.Type.Cached)(c)(annottees)
}
