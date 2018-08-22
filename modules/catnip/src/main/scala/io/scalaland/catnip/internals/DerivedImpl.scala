package io.scalaland.catnip.internals

import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context

private[catnip] class DerivedImpl(config: Map[String, (String, List[String])])(val c: Context)(annottees: Seq[Any])
    extends Loggers {

  import c.universe._

  private type TypeClass = TypeName

  // TODO: there must be a better way to dealias F[_] type
  private def str2TypeConstructor(typeClassName: String): Type =
    c.typecheck(c.parse(s"null: $typeClassName[Nothing]")).tpe.dealias.typeConstructor

  private def buildDerivation(classDef: ClassDef, typeClassName: TypeClass): ValOrDefDef = classDef match {
    case q"""$_ class $name[..${params: Seq[TypeDef] }] $_(...${ctorParams: Seq[Seq[ValDef]] })
                  extends { ..$_ }
                  with ..$_ { $_ => ..$_ }""" =>
      withTraceLog("Derivation expanded") {
        val fType        = str2TypeConstructor(typeClassName.toString)
        val otherReqTCs  = config(fType.toString)._2.map(str2TypeConstructor)
        val needKind     = scala.util.Try(c.typecheck(c.parse(s"null: $fType[List]"))).isSuccess
        val implName     = TermName(s"_derived_${fType.toString.replace('.', '_')}")
        lazy val aType   = if (params.nonEmpty) tq"$name[..${params.map(_.name)}]" else tq"$name"
        val providerArgs = ctorParams.flatten.flatMap { p =>
          (fType :: otherReqTCs).map(tpe => s"${tpe.toString.replace('.', '_')}_${p.name}: $tpe[${p.tpt}]")
        }.map(c.parse)
        val body         = c.parse(s"${config(fType.toString)._1}[${if (needKind) name else aType}]")
        if (params.isEmpty || needKind) q"""implicit val $implName = $body""":            ValDef
        else q"""implicit def $implName[..$params](implicit ..$providerArgs)  = $body""": DefDef
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
    scala.io.Source
      .fromURL(getClass.getClassLoader.getResources(name).nextElement)
      .getLines
      .map(_.trim)
      .filterNot(_ startsWith """////""")
      .filterNot(_ startsWith """#""")
      .filterNot(_.isEmpty)
      .map { s =>
        val kv                           = s.split('=')
        val typeClass                    = kv(0)
        val generator :: otherRequiredTC = kv(1).split(',').toList
        typeClass.trim -> (generator -> otherRequiredTC)
      }
      .toMap

  private val mappings: Map[Type, Map[String, (String, List[String])]] = Map(
    Type.Semi -> loadConfig("derive.semi.conf"),
    Type.Cached -> loadConfig("derive.cached.conf")
  )

  def impl(derivedType: DerivedImpl.Type)(c: Context)(annottees: Seq[c.Expr[Any]]): c.Expr[Any] =
    new DerivedImpl(mappings(derivedType))(c)(annottees).derive().asInstanceOf[c.Expr[Any]]
}
