import sbt._
import sbt.Keys.libraryDependencies
import Dependencies._
import sbtcrossproject.CrossProject
import sbtcrossproject.CrossPlugin.autoImport._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object Dependencies {

  // scala version
  val scalaOrganization  = "org.scala-lang" // "org.typelevel"
  val scalaVersion       = "2.12.7" // "2.12.4-bin-typelevel-4"
  val crossScalaVersions = Seq("2.11.12", "2.12.7", "2.13.0-M4")

  // build tools version
  val scalaFmtVersion = "1.5.1"

  // libraries versions
  val catsVersion     = "1.3.1"
  val specs2Version   = "4.3.4"

  // resolvers
  val resolvers = Seq(
    Resolver sonatypeRepo "public",
    Resolver typesafeRepo "releases"
  )

  val alleyCats          = libraryDependencies += "org.typelevel"   %%% "alleycats-core"    % catsVersion
  val cats               = libraryDependencies += "org.typelevel"   %%% "cats-core"         % catsVersion
  val kittens            = libraryDependencies += "org.typelevel"   %%% "kittens"           % "1.2.0"
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
