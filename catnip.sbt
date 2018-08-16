import sbt._
import Settings._

def resourcesOnCompilerCp(config: Configuration): Setting[_] =
  managedClasspath in config := {
    val res = (resourceDirectory in config).value
    val old = (managedClasspath in config).value
    Attributed.blank(res) +: old
  }

lazy val root = project.root
  .setName("catnip")
  .setDescription("Catnip build")
  .configureRoot
  .aggregate(catnip)

lazy val catnip = project.from("catnip")
  .setName("catnip")
  .setDescription("Macro annotations for Kittens library")
  .setInitialImport("_")
  .configureModule
  .configureTests()
  .settings(resourcesOnCompilerCp(Compile))
  .settings(Compile / resourceGenerators += task[Seq[File]] {
    val file = (Compile / resourceManaged).value / "catnip-version.conf"
    IO.write(file, s"version=${version.value}")
    Seq(file)
  })

addCommandAlias("fullTest", ";test;scalastyle")

addCommandAlias("fullCoverageTest", ";coverage;test;fun:test;it:test;coverageReport;coverageAggregate;scalastyle")

addCommandAlias("relock", ";unlock;reload;update;lock")
