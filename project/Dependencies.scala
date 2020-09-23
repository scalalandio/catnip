import sbt._
import sbt.Keys.{
  scalaBinaryVersion,
  libraryDependencies
}
import Dependencies._
import sbtcrossproject.CrossProject
import sbtcrossproject.CrossPlugin.autoImport._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.scalaJSVersion

object Dependencies {

  // scala version
  val scalaOrganization  = "org.scala-lang" // "org.typelevel"
  val scalaVersion       = "2.13.3" // "2.12.4-bin-typelevel-4"
  val crossScalaVersions = Seq("2.13.3 ", "2.12.11", "2.11.12")

  // build tools version
  val scalaFmtVersion = "1.5.1"

  // libraries versions
  val catsVersion     = Def.setting(if (scalaBinaryVersion.value == "2.11") "2.0.0" else "2.1.1")
  val kittensVersion  = Def.setting(if (scalaJSVersion.startsWith("0.6.")) "2.0.0" else "2.1.0")
  val specs2Version   = Def.setting(if (scalaJSVersion.startsWith("0.6.")) "4.8.3" else "4.10.3")

  // resolvers
  val resolvers = Seq(
    Resolver sonatypeRepo "public",
    Resolver typesafeRepo "releases"
  )

  val alleyCats          = libraryDependencies += "org.typelevel"   %%% "alleycats-core"    % catsVersion.value
  val cats               = libraryDependencies += "org.typelevel"   %%% "cats-core"         % catsVersion.value
  val kittens            = libraryDependencies += "org.typelevel"   %%% "kittens"           % kittensVersion.value
  val shapeless          = libraryDependencies += "com.chuusai"     %%% "shapeless"         % "2.3.3"
}

trait Dependencies {

  val scalaOrganizationUsed = scalaOrganization
  val scalaVersionUsed = scalaVersion
  val crossScalaVersionsUsed = crossScalaVersions

  val scalaFmtVersionUsed = scalaFmtVersion

  // resolvers
  val commonResolvers = resolvers

  val mainDeps = Seq(alleyCats, cats, kittens, shapeless)

  implicit class ProjectRoot(project: Project) {

    def root: Project = project in file(".")
  }

  implicit class ProjectFrom(project: CrossProject) {

    private val commonDir = "modules"

    def from(dir: String): CrossProject = project in file(s"$commonDir/$dir")
  }

  implicit class DependsOnProject(project: CrossProject) {

    private val testConfigurations = Set("test")
    private def findCompileAndTestConfigs(p: CrossProject) =
      (p.projects(JVMPlatform).configurations.map(_.name).toSet intersect testConfigurations) + "compile"

    private val thisProjectsConfigs = findCompileAndTestConfigs(project)
    private def generateDepsForProject(p: CrossProject) =
      p % (thisProjectsConfigs intersect findCompileAndTestConfigs(p) map (c => s"$c->$c") mkString ";")

    def compileAndTestDependsOn(projects: CrossProject*): CrossProject =
      project dependsOn (projects.map(generateDepsForProject): _*)
  }
}
