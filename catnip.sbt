import sbt._
import Settings._
import sbtcrossproject.CrossPlugin.autoImport.{ crossProject, CrossType }

lazy val root = project.root
  .setName("catnip")
  .setDescription("Catnip build")
  .configureRoot
  .noPublish
  .aggregate(catnipJVM, catnipJS, catnipTestsJVM, catnipTestsJS)

lazy val catnip = crossProject(JVMPlatform, JSPlatform).crossType(CrossType.Pure).build.from("catnip")
  .setName("catnip")
  .setDescription("Macro annotations for Kittens library")
  .setInitialImport("cats.implicits._")
  .configureModule
  .publish
  .settings(addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross sbt.CrossVersion.patch))

lazy val catnipJVM = catnip.jvm
  .settings(Compile / unmanagedResourceDirectories += baseDirectory.value / "../src/main/resources")
lazy val catnipJS  = catnip.js
  .settings(Compile / unmanagedResourceDirectories += baseDirectory.value / "../src/main/resources")

lazy val catnipTests = crossProject(JVMPlatform, JSPlatform).crossType(CrossType.Pure).build.from("catnip-tests")
  .setName("catnip-tests")
  .setDescription("Catnip tests")
  .setInitialImport("cats.implicits._")
  .dependsOn(catnip)
  .configureModule
  .configureTests()
  .noPublish
  .settings(libraryDependencies ++= Seq(
    "org.specs2" %%% "specs2-core"       % Dependencies.specs2Version % "test",
    "org.specs2" %%% "specs2-scalacheck" % Dependencies.specs2Version % "test",
    compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross sbt.CrossVersion.patch)
  ))

lazy val catnipTestsJVM = catnipTests.jvm
lazy val catnipTestsJS  = catnipTests.js

addCommandAlias("fullTest", ";test;scalastyle")
addCommandAlias("fullCoverageTest", ";coverage;test;coverageReport;coverageAggregate;scalastyle")
addCommandAlias("relock", ";unlock;reload;update;lock")
