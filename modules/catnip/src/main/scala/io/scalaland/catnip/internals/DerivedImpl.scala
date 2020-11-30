package io.scalaland.catnip.internals

import cats.implicits._
import cats.data.{ Validated, ValidatedNel }
import io.scalaland.catnip.internals.DerivedImpl.Config

import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context
import scala.util

private[catnip] class DerivedImpl(mappings: Map[String, Config], stubs: Map[String, Config])(val c: Context)(
  annottees:                                Seq[Any]
) extends Loggers {

  import c.universe._

  private type TypeClass = TypeName

  // TODO: there must be a better way to dealias F[_] type
  private def str2TypeConstructor(typeClassName: String): Type =
    util
      .Try {
        // allows using "fake" companion object if a type class is missing one, and use it to redirect to the right type
        val key  = c.typecheck(c.parse(s"null: ${typeClassName}.type")).tpe.dealias.toString
        val stub = stubs.get(key).orElse(stubs.get(key.substring(0, key.length - ".type".length))).get.target
        c.typecheck(c.parse(s"null: ${stub}[Nothing]")).tpe.dealias.typeConstructor
      }
      .orElse(util.Try {
        c.typecheck(c.parse(s"null: ${typeClassName}[Nothing]")).tpe.dealias.typeConstructor
      })
      .get

  private def isParametrized(name: TypeName): String => Boolean =
    s"""(^|[,\\[])$name([,\\]]|$$)""".r.pattern.asPredicate.test _

  private def generateBody(name:          TypeName,
                           typeClassName: TypeClass,
                           params:        Seq[TypeDef],
                           ctorParamsOpt: Option[Seq[Seq[ValDef]]]): ValOrDefDef = {
    val fType       = str2TypeConstructor(typeClassName.toString)
    val otherReqTCs = mappings(fType.toString).arguments.map(str2TypeConstructor)
    val needKind    = util.Try(c.typecheck(c.parse(s"null: $fType[List]"))).isSuccess
    val implName    = TermName(s"_derived_${fType.toString.replace('.', '_')}")
    lazy val aType  = TypeName(if (params.nonEmpty) tq"$name[..${params.map(_.name)}]".toString else name.toString)
    lazy val usedParams = ctorParamsOpt match {
      case Some(ctorParams) =>
        lazy val used = ctorParams.flatten.groupBy(_.tpt.toString).flatMap(_._2.headOption.toList).map(_.tpt.toString)
        if (needKind) Nil else params.map(_.name).filter(name => used.exists(isParametrized(name)))
      case None =>
        if (needKind) Nil else params.map(_.name)
    }
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
    val body       = c.parse(s"{ $suppressUnused${mappings(fType.toString).target}[$tcForType] }")
    val returnType = tq"$fType[$tcForType]"
    val sanitizedParams = params.map {
      case TypeDef(_: Modifiers, name: TypeName, tparams: List[TypeDef], rhs: Tree) =>
      TypeDef(Modifiers(), name, tparams, rhs)
    }
    // TODO: figure out why c.parse is needed :/
    if (usedParams.isEmpty) c.parse(s"""implicit val $implName: $returnType = $body""").asInstanceOf[ValDef]
    else
      c.parse(q"""implicit def $implName[..$sanitizedParams](implicit ..$providerArgs): $returnType = $body""".toString)
        .asInstanceOf[DefDef]
  }

  private def buildDerivation(classDef: ClassDef, typeClassName: TypeClass): ValOrDefDef = classDef match {
    case q"""$_ trait $name[..${params: Seq[TypeDef] }]
                  extends { ..$_ }
                  with ..$_ { $_ => ..$_ }""" =>
      withTraceLog(s"Derivation expanded for $name trait") {
        generateBody(name, typeClassName, params, None)
      }
    case q"""$_ class $name[..${params: Seq[TypeDef] }] $_(...${ctorParams: Seq[Seq[ValDef]] })
                  extends { ..$_ }
                  with ..$_ { $_ => ..$_ }""" =>
      withTraceLog(s"Derivation expanded for $name class") {
        generateBody(name, typeClassName, params, Some(ctorParams))
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

private[catnip] object DerivedImpl extends Loggers {

  final case class Config(target: String, arguments: List[String])

  private def loadConfig(name: String): ValidatedNel[String, Map[String, Config]] =
    withTraceLog(s"Configs found for $name") {
      val configFiles = getClass.getClassLoader.getResources(name)
      Iterator
        .continually {
          if (configFiles.hasMoreElements) Some(configFiles.nextElement())
          else None
        }
        .takeWhile(_.isDefined)
        .collect { case Some(value) => value }
        .toList
    }.map { url =>
        val source = scala.io.Source.fromURL(url)
        try {
          Validated.valid(
            source
              .getLines()
              .map(_.trim)
              .filterNot(_ startsWith raw"""//""")
              .filterNot(_ startsWith raw"""#""")
              .filterNot(_.isEmpty)
              .map { s =>
                val kv                           = s.split('=')
                val typeClass                    = kv(0)
                val generator :: otherRequiredTC = kv(1).split(',').toList
                typeClass.trim -> (Config(generator, otherRequiredTC))
              }
              .toMap
          )
        } catch {
          case _: java.util.NoSuchElementException =>
            Validated.invalidNel(s"Unable to load $name using ${getClass.getClassLoader.toString} - failed at $url")
          case err: Throwable =>
            Validated.invalidNel(err.getMessage)
        } finally {
          source.close()
        }
      }
      .sequence
      .map(_.fold(Map.empty[String, Config])(_ ++ _))

  private val mappingsE: ValidatedNel[String, Map[String, Config]] = loadConfig("derive.semi.conf").map { map =>
    map.withDefault { key =>
      val msg = s"No semi definition found for a type class $key, available definitions:\n${map.mkString("\n")}"
      throw new NoSuchElementException(msg)
    }
  }
  private val stubsE: ValidatedNel[String, Map[String, Config]] = loadConfig("derive.stub.conf").map { map =>
    map.withDefault { key =>
      val msg = s"No stub definition found for object $key, available definitions:\n${map.mkString("\n")}"
      throw new NoSuchElementException(msg)
    }
  }

  def impl(c: Context)(annottees: Seq[c.Expr[Any]]): c.Expr[Any] =
    (mappingsE, stubsE).tupled match {
      case Validated.Valid((mappings, stubs)) =>
        try {
          new DerivedImpl(mappings, stubs)(c)(annottees).derive().asInstanceOf[c.Expr[Any]]
        } catch {
          case e: Throwable => c.abort(c.enclosingPosition, e.getMessage)
        }
      case Validated.Invalid(errors) => c.abort(c.enclosingPosition, errors.mkString_("\n"))
    }
}
