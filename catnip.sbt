import sbt._
import Settings._
import sbtcrossproject.CrossPlugin.autoImport.{ crossProject, CrossType }

lazy val root = project.root
  .setName("catnip")
  .setDescription("Catnip build")
  .configureRoot
  .noPublish
  .aggregate(catnipJVM, catnipJS, catnipCustomTestsJVM, catnipCustomTestsJS, catnipTestsJVM, catnipTestsJS)

lazy val catnip = crossProject(JVMPlatform, JSPlatform).crossType(CrossType.Pure).build.from("catnip")
  .setName("catnip")
  .setDescription("Macro annotations for Kittens library")
  .setInitialImport("cats.implicits._, alleycats.std.all._")
  .configureModule
  .publish

lazy val catnipJVM = catnip.jvm
  .settings(Compile / unmanagedResourceDirectories += baseDirectory.value / "../src/main/resources")
lazy val catnipJS  = catnip.js
  .settings(Compile / unmanagedResourceDirectories += baseDirectory.value / "../src/main/resources")

lazy val catnipCustomTests = crossProject(JVMPlatform, JSPlatform).crossType(CrossType.Pure).build.from("catnip-custom-example")
  .setName("catnip-custom-example")
  .setDescription("Example for custom derivation")
  .setInitialImport("cats.implicits._, alleycats.std.all._")
  .dependsOn(catnip)
  .configureModule
  .noPublish

lazy val catnipCustomTestsJVM = catnipCustomTests.jvm
  .settings(Compile / unmanagedResourceDirectories += baseDirectory.value / "../src/main/resources")
lazy val catnipCustomTestsJS  = catnipCustomTests.js
  .settings(Compile / unmanagedResourceDirectories += baseDirectory.value / "../src/main/resources")

lazy val catnipTests = crossProject(JVMPlatform, JSPlatform).crossType(CrossType.Pure).build.from("catnip-tests")
  .setName("catnip-tests")
  .setDescription("Catnip tests")
  .setInitialImport("cats.implicits._, alleycats.std.all._")
  .dependsOn(catnip, catnipCustomTests)
  .configureModule
  .configureTests()
  .noPublish
  .settings(libraryDependencies ++= Seq(
    "org.specs2" %%% "specs2-core"       % Dependencies.specs2Version.value % "test",
    "org.specs2" %%% "specs2-scalacheck" % Dependencies.specs2Version.value % "test"
  ))

lazy val catnipTestsJVM = catnipTests.jvm
lazy val catnipTestsJS  = catnipTests.js

lazy val readme = scalatex.ScalatexReadme(
    projectId = "readme",
    wd        = file(""),
    url       = "https://github.com/scalalandio/catnip/tree/master",
    source    = "Readme"
  )
  .configureModule
  .noPublish
  .enablePlugins(GhpagesPlugin)
  .settings(
    scalaVersion := "2.12.11", // there is no ScalaTex for 2.13
    siteSourceDirectory := target.value / "scalatex",
    git.remoteRepo := "git@github.com:scalalandio/catnip.git",
    Jekyll / makeSite / includeFilter := new FileFilter { def accept(p: File) = true }
  )

addCommandAlias("fullTest", ";test;scalastyle")
addCommandAlias("fullCoverageTest", ";coverage;test;coverageReport;coverageAggregate;scalastyle")
