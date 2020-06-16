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
  .setInitialImport("cats.implicits._, alleycats.std.all._")
  .configureModule
  .publish

lazy val catnipJVM = catnip.jvm
  .settings(Compile / unmanagedResourceDirectories += baseDirectory.value / "../src/main/resources")
lazy val catnipJS  = catnip.js
  .settings(Compile / unmanagedResourceDirectories += baseDirectory.value / "../src/main/resources")

lazy val catnipTests = crossProject(JVMPlatform, JSPlatform).crossType(CrossType.Pure).build.from("catnip-tests")
  .setName("catnip-tests")
  .setDescription("Catnip tests")
  .setInitialImport("cats.implicits._, alleycats.std.all._")
  .dependsOn(catnip)
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
    siteSourceDirectory := target.value / "scalatex",
    git.remoteRepo := "git@github.com:scalalandio/catnip.git",
    Jekyll / makeSite / includeFilter := new FileFilter { def accept(p: File) = true }
  )

addCommandAlias("fullTest", ";test;scalastyle")
addCommandAlias("fullCoverageTest", ";coverage;test;coverageReport;coverageAggregate;scalastyle")
addCommandAlias("relock", ";unlock;reload;update;lock")
