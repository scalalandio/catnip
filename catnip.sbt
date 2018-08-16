import sbt._
import Settings._

lazy val root = project.root
  .setName("catnip")
  .setDescription("Catnip build")
  .configureRoot
  .aggregate(catnip, catnipTests)

lazy val catnip = project.from("catnip")
  .setName("catnip")
  .setDescription("Macro annotations for Kittens library")
  .setInitialImport("_")
  .configureModule
  .settings(Compile / resourceGenerators += task[Seq[File]] {
    val file = (Compile / resourceManaged).value / "catnip-version.conf"
    IO.write(file, s"version=${version.value}")
    Seq(file)
  })

lazy val catnipTests = project.from("catnip-tests")
  .setName("catnip-tests")
  .setDescription("Catnip tests")
  .setInitialImport("_")
  .configureModule
  .dependsOn(catnip)
  .configureTests()

addCommandAlias("fullTest", ";test;scalastyle")

addCommandAlias("fullCoverageTest", ";coverage;test;fun:test;it:test;coverageReport;coverageAggregate;scalastyle")

addCommandAlias("relock", ";unlock;reload;update;lock")
