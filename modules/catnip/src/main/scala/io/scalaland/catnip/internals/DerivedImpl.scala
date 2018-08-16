package io.scalaland.catnip.internals

import com.typesafe.config.{ Config, ConfigFactory }

import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context

private[catnip] class DerivedImpl(config: Config)(val c: Context)(annottees: Seq[Any]) extends Loggers {

  import c.universe._

  private type TypeClass = TypeName

  private def buildDerivation(classDef: ClassDef, typeClassName: TypeClass): ValOrDefDef = classDef match {
    case q"""$_ class $name[..${params: Seq[TypeDef] }] $_(...${ctorParams: Seq[Seq[ValDef]] })
                  extends { ..$_ }
                  with ..$_ { $_ => ..$_ }""" =>
      withTraceLog("Derivation expanded") {
        // TODO: there must be a better way to dealias F[_] type
        val fType = TypeName(
          c.typecheck(c.parse(s"null: $typeClassName[Nothing]"))
            .tpe
            .dealias
            .toString
            .replaceFirst("""\[Nothing\]$""", "")
        )
        val implName     = TermName(s"_derived_${fType.toString.replace('.', '_')}")
        val aType        = if (params.nonEmpty) tq"$name[..${params.map(_.name)}]" else tq"$name"
        val faType       = c.parse(s"$fType[$aType]").tpe
        val providerArgs = ctorParams.flatten.map(p => s"${p.name}: $fType[${p.tpt}]").map(c.parse)
        val body         = c.parse(s"${config.getString(fType.toString)}[$aType]")
        if (params.nonEmpty) q"""implicit def $implName[..$params](implicit ..$providerArgs): $faType = $body""": DefDef
        else q"""implicit val $implName: $faType = $body""":                                                      ValDef
      }
  }

  private def extendCompanion(objectDef: ModuleDef, classDef: ClassDef, typeClasses: Seq[TypeClass]): ModuleDef =
    objectDef match {
      case q"$mods object $tname extends { ..$earlydefns } with ..$parents { $self => ..$body }" =>
        q"""$mods object $tname extends { ..$earlydefns } with ..$parents { $self =>
              $body
              ..${typeClasses.map(buildDerivation(classDef, _))}
            }""": ModuleDef
    }

  private def createCompanion(classDef: ClassDef, typeClasses: Seq[TypeClass]): ModuleDef =
    q"""object ${TermName(classDef.name.toString)} {
          ..${typeClasses.map(buildDerivation(classDef, _))}
        }""": ModuleDef

  def derive(): c.Expr[Any] = withDebugLog("Type class injection result") {
    val typeClasses: Seq[TypeClass] = c.prefix.tree match {
      case q"new $_(..$tcs)" => tcs.map(tc => TypeName(tc.toString))
    }

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
