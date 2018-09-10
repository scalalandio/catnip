package io.scalaland.catnip

import io.scalaland.catnip.internals._

import scala.reflect.macros.whitebox.Context
import scala.language.experimental.macros
import scala.annotation.{ compileTimeOnly, StaticAnnotation }

@compileTimeOnly("enable macro paradise to expand macro annotations")
class Semi(typeClasses: Any*) extends StaticAnnotation {

  def macroTransform(annottees: Any*): Any = macro Semi.impl
}

private object Semi {

  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = DerivedImpl.impl(c)(annottees)
}
