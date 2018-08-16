import sbt._

import Dependencies._

object Dependencies {

  // scala version
  val scalaOrganization = "org.scala-lang" // "org.typelevel"
  val scalaVersion      = "2.12.6" // "2.12.4-bin-typelevel-4"

  // build tools version
  val scalaFmtVersion = "1.5.1"

  // aspectj version
  val aspectjVersion = "1.9.1"

  // libraries versions
  val catsVersion     = "1.1.0"
  val monixVersion    = "3.0.0-RC1"
  val specs2Version   = "4.2.0"

  // resolvers
  val resolvers = Seq(
    Resolver sonatypeRepo "public",
    Resolver typesafeRepo "releases"
  )

  // functional libraries
  val cats               = "org.typelevel"                %% "cats-core"                 % catsVersion
  val catsLaws           = "org.typelevel"                %% "cats-laws"                 % catsVersion
  val shapeless          = "com.chuusai"                  %% "shapeless"                 % "2.3.3"
  // async
  val monixExecution     = "io.monix"                     %% "monix-execution"           % monixVersion
  val monixEval          = "io.monix"                     %% "monix-eval"                % monixVersion
  // config
  val scopt              = "com.github.scopt"             %% "scopt"                     % "3.7.0"
  val scalaConfig        = "com.typesafe"                 %  "config"                    % "1.3.3"
  val pureConfig         = "com.github.pureconfig"        %% "pureconfig"                % "0.9.1"  excludeAll (
          ExclusionRule(   "org.scala-lang")
  )
  // logging
  val scalaLogging       = "com.typesafe.scala-logging"   %% "scala-logging"             % "3.9.0"
  val logback            = "ch.qos.logback"               %  "logback-classic"           % "1.2.3"
  // testing
  val spec2Core          = "org.specs2"                   %% "specs2-core"               % specs2Version
  val spec2Mock          = "org.specs2"                   %% "specs2-mock"               % specs2Version
  val spec2Scalacheck    = "org.specs2"                   %% "specs2-scalacheck"         % specs2Version
}

trait Dependencies {

  val scalaOrganizationUsed = scalaOrganization
  val scalaVersionUsed = scalaVersion

  val scalaFmtVersionUsed = scalaFmtVersion

  val aspectjVersionUsed = aspectjVersion

  // resolvers
  val commonResolvers = resolvers

  val mainDeps = Seq(cats, shapeless, scopt, scalaConfig, pureConfig, monixExecution, monixEval, scalaLogging, logback)

  val testDeps = Seq(catsLaws, spec2Core, spec2Mock, spec2Scalacheck)

  implicit class ProjectRoot(project: Project) {

    def root: Project = project in file(".")
  }

  implicit class ProjectFrom(project: Project) {

    private val commonDir = "modules"

    def from(dir: String): Project = project in file(s"$commonDir/$dir")
  }

  implicit class DependsOnProject(project: Project) {

    private val testConfigurations = Set("test", "fun", "it")
    private def findCompileAndTestConfigs(p: Project) =
      (p.configurations.map(_.name).toSet intersect testConfigurations) + "compile"

    private val thisProjectsConfigs = findCompileAndTestConfigs(project)
    private def generateDepsForProject(p: Project) =
      p % (thisProjectsConfigs intersect findCompileAndTestConfigs(p) map (c => s"$c->$c") mkString ";")

    def compileAndTestDependsOn(projects: Project*): Project =
      project dependsOn (projects.map(generateDepsForProject): _*)
  }
}
