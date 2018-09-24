package io.scalaland.catnip.internals

import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context
import scala.util.Try

private[catnip] class DerivedImpl(config: Map[String, (String, List[String])])(val c: Context)(annottees: Seq[Any])
    extends Loggers {

  import c.universe._

  private type TypeClass = TypeName

  // TODO: there must be a better way to dealias F[_] type
  private def str2TypeConstructor(typeClassName: String): Type =
    c.typecheck(c.parse(s"null: $typeClassName[Nothing]")).tpe.dealias.typeConstructor

  private def isParametrized(name: TypeName): String => Boolean =
    s"""(^|[,\\[])$name([,\\]]|$$)""".r.pattern.asPredicate.test _

  private def buildDerivation(classDef: ClassDef, typeClassName: TypeClass): ValOrDefDef = classDef match {
    case q"""$_ trait $name[..${params: Seq[TypeDef] }]
                  extends { ..$_ }
                  with ..$_ { $_ => ..$_ }""" =>
      withTraceLog(s"Derivation expanded for $name trait") {
        val fType      = str2TypeConstructor(typeClassName.toString)
        val implName   = TermName(s"_derived_${fType.toString.replace('.', '_')}")
        lazy val aType = if (params.nonEmpty) TypeName(tq"$name[..${params.map(_.name)}]".toString) else name
        val body       = c.parse(s"{ ${config(fType.toString)._1}[$aType] }")
        val returnType = tq"$fType[$aType]"
        // TODO: figure out, why this doesn't work
        // q"""implicit val $implName: $returnType = $body""": ValDef
        c.parse(s"""implicit val $implName: $returnType = $body""").asInstanceOf[ValDef]
      }
    case q"""$_ class $name[..${params: Seq[TypeDef] }] $_(...${ctorParams: Seq[Seq[ValDef]] })
                  extends { ..$_ }
                  with ..$_ { $_ => ..$_ }""" =>
      withTraceLog(s"Derivation expanded for $name class") {
        val fType       = str2TypeConstructor(typeClassName.toString)
        val otherReqTCs = config(fType.toString)._2.map(str2TypeConstructor)
        val needKind    = scala.util.Try(c.typecheck(c.parse(s"null: $fType[List]"))).isSuccess
        val implName    = TermName(s"_derived_${fType.toString.replace('.', '_')}")
        lazy val aType  = TypeName(if (params.nonEmpty) tq"$name[..${params.map(_.name)}]".toString else name.toString)
        lazy val argTypes =
          ctorParams.flatten.groupBy(_.tpt.toString).flatMap(_._2.headOption.toList).map(_.tpt.toString)
        lazy val usedParams =
          if (needKind) Nil
          else params.map(_.name).filter(name => argTypes.exists(isParametrized(name)))
        val providerArgs = usedParams
          .flatMap { p =>
            (fType :: otherReqTCs).map(tpe => s"${tpe.toString.replace('.', '_')}_$p: $tpe[$p]")
          }
          .map(c.parse)
        lazy val suppressUnused = usedParams
          .flatMap { p =>
            (fType :: otherReqTCs).map(tpe => s"${tpe.toString.replace('.', '_')}_$p.hashCode;")
          }
          .mkString("")
        val tcForType  = if (needKind) name else aType
        val body       = c.parse(s"{ $suppressUnused${config(fType.toString)._1}[$tcForType] }")
        val returnType = tq"$fType[$tcForType]"
        // TODO: figure out, why this doesn't work
        // if (usedParams.isEmpty) q"""implicit val $implName: $returnType = $body""":                   ValDef
        // else q"""implicit def $implName[..$params](implicit ..$providerArgs): $returnType = $body""": DefDef
        if (usedParams.isEmpty) c.parse(s"""implicit val $implName: $returnType = $body""").asInstanceOf[ValDef]
        else
          c.parse(q"""implicit def $implName[..$params](implicit ..$providerArgs): $returnType = $body""".toString)
            .asInstanceOf[DefDef]
      }
  }

  private def extendCompanion(objectDef: ModuleDef, classDef: ClassDef, typeClasses: Seq[TypeClass]): ModuleDef =
    objectDef match {
      case q"$mods object $tname extends { ..$earlydefns } with ..$parents { $self => ..$body }" =>
        q"""$mods object $tname extends { ..$earlydefns } with ..$parents { $self =>
              ..$body
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
      case got => c.abort(c.enclosingPosition, s"@Semi can only annotate class, got: $got")
    }
  }
}

private[catnip] object DerivedImpl {

  private def loadConfig(name: String) =
    Try {

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
    }.toEither.left.map {
      case _: java.util.NoSuchElementException =>
        s"Unable to load $name using ${getClass.getClassLoader}"
      case err: Throwable =>
        err.getMessage
    }

  private val mappingsE: Either[String, Map[String, (String, List[String])]] = loadConfig("derive.semi.conf")

  def impl(c: Context)(annottees: Seq[c.Expr[Any]]): c.Expr[Any] =
    mappingsE match {
      case Right(mappings) => new DerivedImpl(mappings)(c)(annottees).derive().asInstanceOf[c.Expr[Any]]
      case Left(error) => c.abort(c.enclosingPosition, error)
    }
}
