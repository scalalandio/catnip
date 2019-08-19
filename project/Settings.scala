import sbt._
import sbt.Keys._
import sbt.TestFrameworks.Specs2
import sbt.Tests.Argument
import com.typesafe.sbt._
import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._
import org.scalastyle.sbt.ScalastylePlugin.autoImport._
import sbtcrossproject.CrossProject
import scoverage._
import wartremover._

object Settings extends Dependencies {

  private val commonSettings = Seq(
    organization := "io.scalaland",

    scalaOrganization  := scalaOrganizationUsed,
    scalaVersion       := scalaVersionUsed,
    crossScalaVersions := crossScalaVersionsUsed,

    scalafmtVersion := scalaFmtVersionUsed
  )

  private val rootSettings = commonSettings

  private val modulesSettings = commonSettings ++ Seq(
    scalacOptions ++= Seq(
      // standard settings
      "-target:jvm-1.8",
      "-encoding", "UTF-8",
      "-unchecked",
      "-deprecation",
      "-explaintypes",
      "-feature",
      // language features
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:postfixOps",
      // private options
      "-Xexperimental",
      "-Ybackend-parallelism", "8",
      "-Ymacro-annotations",
      "-Yno-adapted-args",
      "-Ypartial-unification",
      // warnings
      "-Ywarn-dead-code",
      "-Ywarn-extra-implicit",
      "-Ywarn-inaccessible",
      "-Ywarn-infer-any",
      "-Ywarn-macros:after",
      "-Ywarn-nullary-override",
      "-Ywarn-nullary-unit",
      "-Ywarn-numeric-widen",
      "-Ywarn-unused:implicits",
      "-Ywarn-unused:patvars",
      "-Ywarn-unused:privates",
      "-Ywarn-value-discard",
      // advanced options
      "-Xcheckinit",
      "-Xfatal-warnings",
      "-Xfuture",
      // linting
      "-Xlint:adapted-args",
      "-Xlint:by-name-right-associative",
      "-Xlint:constant",
      "-Xlint:delayedinit-select",
      "-Xlint:doc-detached",
      "-Xlint:inaccessible",
      "-Xlint:infer-any",
      "-Xlint:missing-interpolator",
      "-Xlint:nullary-override",
      "-Xlint:nullary-unit",
      "-Xlint:option-implicit",
      "-Xlint:package-object-classes",
      "-Xlint:poly-implicit-overload",
      "-Xlint:private-shadow",
      "-Xlint:stars-align",
      "-Xlint:type-parameter-shadow",
      "-Xlint:unsound-match"
    ).filterNot(
      (if (scalaVersion.value.startsWith("2.13")) Set(
        // removed in 2.13.x
        "-Yno-adapted-args",
        "-Ypartial-unification",
        // only for 2.11.x
        "-Xexperimental"
      ) else if (scalaVersion.value.startsWith("2.12")) Set(
        // added in 2.13.x
        "-Ymacro-annotations",
        // only for 2.11.x
        "-Xexperimental"
      ) else if (scalaVersion.value.startsWith("2.11")) Set(
        // added in 2.13.x
        "-Ymacro-annotations",
        // added in 2.12.x
        "-Ybackend-parallelism", "8",
        "-Ywarn-extra-implicit",
        "-Ywarn-macros:after",
        "-Ywarn-unused:implicits",
        "-Ywarn-unused:patvars",
        "-Ywarn-unused:privates",
        "-Xlint:constant"
      ) else Set.empty[String]).contains _
    ),
    console / scalacOptions --= Seq(
      // warnings
      "-Ywarn-unused:implicits",
      "-Ywarn-unused:imports",
      "-Ywarn-unused:locals",
      "-Ywarn-unused:params",
      "-Ywarn-unused:patvars",
      "-Ywarn-unused:privates",
      // advanced options
      "-Xfatal-warnings",
      // linting
      "-Xlint"
    ),

    Global / cancelable := true,

    resolvers ++= commonResolvers,

    Compile / scalafmtOnCompile := true,

    scalastyleFailOnError := true,

    Compile / compile / wartremoverWarnings ++= Warts.allBut(
      Wart.Any,
      Wart.AsInstanceOf,
      Wart.DefaultArguments,
      Wart.ExplicitImplicitTypes,
      Wart.ImplicitConversion,
      Wart.ImplicitParameter,
      Wart.Overloading,
      Wart.PublicInference,
      Wart.NonUnitStatements,
      Wart.Nothing,
      Wart.ToString
    )
  ) ++ mainDeps ++ Seq(libraryDependencies ++= (if (!scalaVersion.value.startsWith("2.13")) {
    Seq(compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross sbt.CrossVersion.patch))
  } else {
    Nil
  }))

  private val publishSettings = Seq(
    homepage := Some(url("https://scalaland.io")),
    licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    scmInfo := Some(
      ScmInfo(url("https://github.com/scalalandio/catnip"), "scm:git:git@github.com:scalalandio/catnip.git")
    ),
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ =>
      false
    },
    pomExtra :=
      <developers>
        <developer>
          <id>krzemin</id>
          <name>Piotr Krzemiński</name>
          <url>https://github.com/krzemin</url>
        </developer>
        <developer>
          <id>MateuszKubuszok</id>
          <name>Mateusz Kubuszok</name>
          <url>https://github.com/MateuszKubuszok</url>
        </developer>
      </developers>
  )

  private val noPublishSettings =
    Seq(skip in publish := true, publishArtifact := false)

  implicit class PublishRootConfigurator(project: Project) {

    def publish: Project = project
      .settings(publishSettings)

    def noPublish: Project = project
      .settings(noPublishSettings)
  }

  implicit class PublishConfigurator(project: CrossProject) {

    def publish: CrossProject = project
      .settings(publishSettings)

    def noPublish: CrossProject = project
      .settings(noPublishSettings)
  }

  abstract class TestConfigurator(project: CrossProject, config: Configuration) {

    protected def configure(requiresFork: Boolean): CrossProject = project
      .settings(inConfig(config)(scalafmtSettings))
      .settings(inConfig(config)(Seq(
        scalafmtOnCompile := true,
        scalastyleConfig := baseDirectory.value / "scalastyle-test-config.xml",
        scalastyleFailOnError := false,
        fork := requiresFork,
        testFrameworks := Seq(Specs2)
      )))
      .enablePlugins(ScoverageSbtPlugin)

    protected def configureSequential(requiresFork: Boolean): CrossProject = configure(requiresFork)
      .settings(inConfig(config)(Seq(
        testOptions += Argument(Specs2, "sequential"),
        parallelExecution  := false
      )))
  }

  implicit class RootDataConfigurator(project: Project) {

    def setName(newName: String): Project = project.settings(name := newName)

    def setDescription(newDescription: String): Project = project.settings(description := newDescription)

    def setInitialImport(newInitialCommand: String): Project =
      project.settings(initialCommands := s"import io.scalaland.catnip._, $newInitialCommand")
  }

  implicit class DataConfigurator(project: CrossProject) {

    def setName(newName: String): CrossProject = project.settings(name := newName)

    def setDescription(newDescription: String): CrossProject = project.settings(description := newDescription)

    def setInitialImport(newInitialCommand: String): CrossProject =
      project.settings(initialCommands := s"import io.scalaland.catnip._, $newInitialCommand")
  }

  implicit class RootConfigurator(project: Project) {

    def configureRoot: Project = project.settings(rootSettings: _*)
  }

  implicit class ModuleConfiguratorP(project: Project) {

    def configureModule: Project = project.settings(modulesSettings: _*).enablePlugins(GitVersioning)
  }

  implicit class ModuleConfiguratorCP(project: CrossProject) {

    def configureModule: CrossProject = project.settings(modulesSettings: _*).enablePlugins(GitVersioning)
  }

  implicit class UnitTestConfigurator(project: CrossProject) extends TestConfigurator(project, Test) {

    def configureTests(requiresFork: Boolean = false): CrossProject = configure(requiresFork)

    def configureTestsSequential(requiresFork: Boolean = false): CrossProject = configureSequential(requiresFork)
  }
}
