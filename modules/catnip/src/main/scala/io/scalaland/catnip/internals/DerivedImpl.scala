package io.scalaland.catnip.internals

import com.typesafe.config.{ Config, ConfigFactory }

import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context

private[catnip] class DerivedImpl(config: Config)(val c: Context)(annottees: Seq[Any]) extends Loggers {

  import c.universe._

  private def buildDerivation(classDef: ClassDef, typeClass: TypeName): ValOrDefDef = classDef match {
    case q"""$_ class $name[..${params: Seq[TypeDef] }] $_(...${ctorParams: Seq[Seq[ValDef]] })
                  extends { ..$_ }
                  with ..$_ { $_ => ..$_ }""" =>
      withTraceLog("Derivation expanded") {
        val derivation     = config.getString(typeClass.toString)
        val derivationName = TermName(s"_derived_$typeClass")
        if (params.nonEmpty) {
          val providerArgs = ctorParams.flatten.map(p => q"${p.name}: $typeClass[${p.tpt}]")

          q"""implicit def ${derivationName}[..$params](implicit ..$providerArgs)
                    : $typeClass[$name[..${params.map(_.name)}]] =
                 $derivation""": DefDef
        } else {
          q"""implicit val ${derivationName}
                    : $typeClass[$name] =
                 $derivation""": ValDef
        }
      }
  }

  private def extendCompanion(objectDef: ModuleDef, classDef: ClassDef, typeClasses: Seq[TypeName]): ModuleDef =
    objectDef match {
      case q"$mods object $tname extends { ..$earlydefns } with ..$parents { $self => ..$body }" =>
        q"""$mods object $tname extends { ..$earlydefns } with ..$parents { $self =>
              $body
              ..${typeClasses.map(buildDerivation(classDef, _))}
            }""": ModuleDef
    }

  private def createCompanion(classDef: ClassDef, typeClasses: Seq[TypeName]): ModuleDef =
    q"""object ${TermName(classDef.name.toString)} {
          ..${typeClasses.map(buildDerivation(classDef, _))}
        }""": ModuleDef

  def derive(): c.Expr[Any] = withDebugLog("Type class injection result") {
    val typeClasses = c.prefix.tree match {
      case q"new $_(..$tcs)" => tcs.map(tc => TypeName(tc.toString))
    }

    println(typeClasses)
    println(config)
    Stream
      .iterate(getClass.getClassLoader.getResources("catnip-version.conf"))(identity)
      .map { r =>
        if (r.hasMoreElements) Some(r.nextElement()) else None
      }
      .takeWhile(_.isDefined)
      .collect { case Some(x) => x }
      .foreach(println)
    println(ConfigFactory.parseURL(getClass.getClassLoader.getResources("catnip-version.conf").nextElement))
    println(ConfigFactory.parseResources("catnip-version.conf"))

    annottees.toList match {
      case Expr(classDef: ClassDef) :: Expr(objectDef: ModuleDef) :: Nil =>
        c.Expr(q"""$classDef
                   ${extendCompanion(objectDef, classDef, typeClasses)}""")
      case Expr(objectDef: ModuleDef) :: Expr(classDef: ClassDef) :: Nil =>
        c.Expr(q"""${extendCompanion(objectDef, classDef, typeClasses)}
                   $classDef""")
      case Expr(classDef: ClassDef) :: Nil =>
        c.Expr(q"""$classDef
                   ${createCompanion(classDef, typeClasses)}""")
      case got => c.abort(c.enclosingPosition, s"@Semi or @Cached can only annotate class, got: $got")
    }
  }
}

private[catnip] object DerivedImpl {

  sealed trait Type
  object Type {
    case object Semi extends Type
    case object Cached extends Type
  }

  private def loadConfig(name: String) =
    ConfigFactory.parseURL(getClass.getClassLoader.getResources(name).nextElement)

  private val mappings: Map[Type, Config] = Map(
    Type.Semi -> loadConfig("derive.semi.conf"),
    Type.Cached -> loadConfig("derive.cached.conf")
  )

  def impl(derivedType: DerivedImpl.Type)(c: Context)(annottees: Seq[c.Expr[Any]]): c.Expr[Any] =
    new DerivedImpl(mappings(derivedType))(c)(annottees).derive().asInstanceOf[c.Expr[Any]]
}
