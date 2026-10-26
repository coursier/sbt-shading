// projectMatrix rather than crossProject: on sbt 2 the platform is not part of the CrossVersion,
// which is what made shading bundle the whole classpath on a Scala.js row.

// Artifacts are named in full rather than with %%, which carries the platform on sbt 2 but not on
// sbt 1, where %%% does instead.
def rowSettings(suffix: String) = Seq(
  libraryDependencies += "io.argonaut" % ("argonaut" + suffix) % "6.2.5",
  shadedDependencies += "io.argonaut" % ("argonaut" + suffix) % "foo",
  // shading rebuilds this row's artifact name to tell its own module from its dependencies; without
  // the platform that name matches nothing, every module counts as shaded, and packaging asserts
  TaskKey[Unit]("checkRow") := Def.taskDyn {
    // before sbt 2.0.7, the platform is not part of the CrossVersion (sbt/sbt#9621), and this fails
    import sbt.librarymanagement._
    val shouldFail = !suffix.startsWith("_2") &&
      VersionNumber(sbtVersion.value).matchesSemVer(SemanticSelector(">=2.0.0 <2.0.7"))
    if (shouldFail) shadedPackageBin.failure.map(_ => ()) else shadedPackageBin.map(_ => ())
  }.value
)

lazy val root = projectMatrix
  .in(file("."))
  // so the rows are named root and rootJS
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion("2.12.21"))
  .enablePlugins(ShadingPlugin)
  .settings(
    shadingRules += ShadingRule.moveUnder("argonaut", "foo.shaded"),
    validNamespaces += "foo",
    // declared so it counts as a direct dependency and is not bundled with the shaded ones
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    organization := "io.get-coursier.test",
    name := "shading-matrix-test",
    version := "0.1.0-SNAPSHOT"
  )
  .jvmPlatform(Seq("2.12.21"), rowSettings("_2.12"))
  .jsPlatform(Seq("2.12.21"), rowSettings("_sjs1_2.12"))
