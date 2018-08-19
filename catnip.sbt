import sbt._
import Settings._
import sbtcrossproject.CrossPlugin.autoImport.{ crossProject, CrossType }

lazy val root = project.root
  .setName("catnip")
  .setDescription("Catnip build")
  .configureRoot
  .aggregate(catnipJVM, catnipJS, catnipTestsJVM, catnipTestsJS)

lazy val catnip = crossProject(JVMPlatform, JSPlatform).crossType(CrossType.Pure).build.from("catnip")
  .setName("catnip")
  .setDescription("Macro annotations for Kittens library")
  .setInitialImport("cats.implicits._")
  .configureModule
  .settings(
//    "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
//    "org.scala-lang" % "scala-compiler" % scalaVersion.value % Provided,
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross sbt.CrossVersion.patch)
  )

lazy val catnipJVM = catnip.jvm
lazy val catnipJS  = catnip.js

lazy val catnipTests = crossProject(JVMPlatform, JSPlatform).crossType(CrossType.Pure).build.from("catnip-tests")
  .setName("catnip-tests")
  .setDescription("Catnip tests")
  .setInitialImport("cats.implicits._")
  .dependsOn(catnip)
  .configureModule
  .configureTests()
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
